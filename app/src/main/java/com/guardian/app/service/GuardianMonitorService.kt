package com.guardian.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.guardian.app.GuardianApp
import com.guardian.app.R
import com.guardian.app.data.model.EventType
import com.guardian.app.data.repository.GuardianRepository
import com.guardian.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Foreground service that monitors app installations and provides real-time security notifications.
 * This service ensures that app monitoring works even when the app is in the background.
 */
class GuardianMonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var appInstallReceiver: BroadcastReceiver? = null

    companion object {
        const val ACTION_START = "com.guardian.app.action.START_MONITORING"
        const val ACTION_STOP = "com.guardian.app.action.STOP_MONITORING"
        const val NOTIFICATION_ID = 1000
        const val CHANNEL_MONITORING = "monitoring"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerAppInstallReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, createForegroundNotification())
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        unregisterAppInstallReceiver()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_MONITORING,
                "Мониторинг безопасности",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Фоновый мониторинг установки приложений"
                setShowBadge(false)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_MONITORING)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("🛡️ Guardian активен")
            .setContentText("Мониторинг безопасности работает")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun registerAppInstallReceiver() {
        appInstallReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                handleAppInstallEvent(context, intent)
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(appInstallReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(appInstallReceiver, filter)
        }
    }

    private fun unregisterAppInstallReceiver() {
        appInstallReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                // Already unregistered
            }
        }
    }

    private fun handleAppInstallEvent(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return
        val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)

        serviceScope.launch {
            try {
                val repository = GuardianRepository(context)
                
                // Check if protection and app monitoring is enabled
                val protectionEnabled = repository.isProtectionEnabled.first()
                val appMonitorEnabled = repository.isAppMonitorEnabled.first()
                
                if (!protectionEnabled || !appMonitorEnabled) return@launch

                val appName = getAppName(context, packageName)

                when (intent.action) {
                    Intent.ACTION_PACKAGE_ADDED -> {
                        if (!isReplacing) {
                            performSecurityScan(context, packageName, appName, repository)
                        } else {
                            handleAppUpdate(context, packageName, appName, repository)
                        }
                    }
                    Intent.ACTION_PACKAGE_REMOVED -> {
                        if (!isReplacing) {
                            showSecurityNotification(
                                context,
                                "🗑️ Приложение удалено",
                                "$appName было удалено с устройства",
                                NotificationCompat.PRIORITY_DEFAULT
                            )
                            repository.addEvent(
                                EventType.APP_REMOVED,
                                "🗑️ Приложение удалено",
                                appName,
                                packageName
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun performSecurityScan(
        context: Context,
        packageName: String,
        appName: String,
        repository: GuardianRepository
    ) {
        val scanResult = SecurityScanner.scan(context, packageName, appName)

        when (scanResult.threatLevel) {
            SecurityScanner.ThreatLevel.CRITICAL -> {
                showSecurityNotification(
                    context,
                    "🚨 ВРЕДОНОСНОЕ ПРИЛОЖЕНИЕ!",
                    "$appName - ${scanResult.reason}. Немедленно удалите!",
                    NotificationCompat.PRIORITY_MAX
                )
                repository.addEvent(
                    EventType.APP_BLOCKED,
                    "🚨 ВРЕДОНОСНОЕ ПРИЛОЖЕНИЕ!",
                    "$appName - ${scanResult.reason} (риск: ${scanResult.riskScore}/10)",
                    packageName
                )
            }
            SecurityScanner.ThreatLevel.HIGH -> {
                showSecurityNotification(
                    context,
                    "⚠️ Опасное приложение",
                    "$appName - ${scanResult.reason}",
                    NotificationCompat.PRIORITY_HIGH
                )
                repository.addEvent(
                    EventType.APP_BLOCKED,
                    "⚠️ Опасное приложение установлено",
                    "$appName - ${scanResult.reason} (риск: ${scanResult.riskScore}/10)",
                    packageName
                )
            }
            SecurityScanner.ThreatLevel.MEDIUM -> {
                showSecurityNotification(
                    context,
                    "📱 Приложение установлено",
                    "$appName - требует внимания: ${scanResult.reason}",
                    NotificationCompat.PRIORITY_DEFAULT
                )
                repository.addEvent(
                    EventType.APP_INSTALLED,
                    "📱 Приложение установлено (проверено)",
                    "$appName - ${scanResult.reason}",
                    packageName
                )
            }
            SecurityScanner.ThreatLevel.LOW -> {
                showSecurityNotification(
                    context,
                    "✅ Приложение установлено",
                    "$appName - безопасно",
                    NotificationCompat.PRIORITY_LOW
                )
                repository.addEvent(
                    EventType.APP_INSTALLED,
                    "📱 Приложение установлено",
                    "$appName - проверка завершена, угроз не обнаружено",
                    packageName
                )
            }
        }
    }

    private suspend fun handleAppUpdate(
        context: Context,
        packageName: String,
        appName: String,
        repository: GuardianRepository
    ) {
        val scanResult = SecurityScanner.scan(context, packageName, appName)

        if (scanResult.threatLevel == SecurityScanner.ThreatLevel.CRITICAL || 
            scanResult.threatLevel == SecurityScanner.ThreatLevel.HIGH) {
            showSecurityNotification(
                context,
                "⚠️ Опасное обновление",
                "$appName - новая версия содержит угрозы: ${scanResult.reason}",
                NotificationCompat.PRIORITY_HIGH
            )
            repository.addEvent(
                EventType.APP_BLOCKED,
                "🔄 Обновление с угрозой",
                "$appName - после обновления обнаружены опасные разрешения (риск: ${scanResult.riskScore})",
                packageName
            )
        } else {
            repository.addEvent(
                EventType.APP_INSTALLED,
                "🔄 Приложение обновлено",
                appName,
                packageName
            )
        }
    }

    private fun showSecurityNotification(
        context: Context,
        title: String,
        message: String,
        priority: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channel = when (priority) {
            NotificationCompat.PRIORITY_MAX -> GuardianApp.CHANNEL_BLOCKED
            NotificationCompat.PRIORITY_HIGH -> GuardianApp.CHANNEL_ALERTS
            else -> GuardianApp.CHANNEL_ALERTS
        }

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun getAppName(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }
}

/**
 * Security scanner that analyzes apps for potential threats
 */
object SecurityScanner {

    enum class ThreatLevel {
        CRITICAL, HIGH, MEDIUM, LOW
    }

    data class ScanResult(
        val threatLevel: ThreatLevel,
        val riskScore: Int,
        val reason: String,
        val details: List<String> = emptyList()
    )

    // Known trusted apps (whitelisted)
    private val TRUSTED_APPS = setOf(
        "com.guardian.app",
        "com.google.android.youtube", "com.google.android.gm",
        "com.whatsapp", "org.telegram.messenger", "com.vkontakte.android",
        "com.instagram.android", "com.facebook.katana",
        "ru.sberbankmobile", "com.idamob.tinkoff.android",
        "ru.yandex.searchplugin", "ru.yandex.taxi",
        "ru.megafon.mlk", "ru.mts.mymts", "ru.beeline.services", "ru.tele2.mytele2",
        "com.kinopoisk", "com.max.app", "com.wb.max", "com.max.messenger",
        "ru.avito", "com.ubercab", "ru.taxsee.taxsee",
        "com.relens.android", "ru.sushisea", "com.deliveryclub"
    )

    // Malware signatures with risk scores
    private val MALWARE_SIGNATURES = mapOf(
        "trojan" to 10, "backdoor" to 10, "rat" to 9, "spyware" to 9,
        "spy" to 7, "keylog" to 10, "stealer" to 9, "ransomware" to 10,
        "banker" to 10, "bankbot" to 10, "smsfraud" to 9, "miner" to 8,
        "adware" to 6, "riskware" to 7, "hack" to 7, "cracker" to 7,
        "dropper" to 9, "rootkit" to 10, "botnet" to 10, "worm" to 9,
        "fake" to 7, "fraud" to 8, "phishing" to 9, "pegasus" to 10,
        "anubis" to 10, "cerberus" to 10, "eventbot" to 10, "sharkbot" to 10
    )

    // Dangerous permissions with risk scores
    private val DANGEROUS_PERMISSIONS = mapOf(
        "android.permission.SEND_SMS" to 4,
        "android.permission.READ_SMS" to 3,
        "android.permission.RECEIVE_SMS" to 3,
        "android.permission.CALL_PHONE" to 3,
        "android.permission.READ_CALL_LOG" to 3,
        "android.permission.PROCESS_OUTGOING_CALLS" to 4,
        "android.permission.READ_CONTACTS" to 2,
        "android.permission.RECORD_AUDIO" to 3,
        "android.permission.CAMERA" to 2,
        "android.permission.ACCESS_FINE_LOCATION" to 2,
        "android.permission.SYSTEM_ALERT_WINDOW" to 3,
        "android.permission.BIND_ACCESSIBILITY_SERVICE" to 5
    )

    fun scan(context: Context, packageName: String, appName: String): ScanResult {
        val details = mutableListOf<String>()
        var riskScore = 0

        // Check 1: Trusted app whitelist
        if (TRUSTED_APPS.contains(packageName) ||
            packageName.startsWith("com.google.android") ||
            packageName.startsWith("com.android") ||
            packageName.startsWith("ru.yandex") ||
            packageName.startsWith("com.samsung") ||
            packageName.startsWith("com.miui")
        ) {
            return ScanResult(ThreatLevel.LOW, 0, "Доверенное приложение")
        }

        // Check 2: Malware signatures
        val lowerPackage = packageName.lowercase()
        for ((signature, score) in MALWARE_SIGNATURES) {
            if (lowerPackage.contains(signature)) {
                riskScore += score
                details.add("Сигнатура: $signature")
            }
        }

        // Check 3: Analyze permissions
        try {
            val pm = context.packageManager
            val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            val requestedPermissions = packageInfo.requestedPermissions?.toList() ?: emptyList()

            for (perm in requestedPermissions) {
                val weight = DANGEROUS_PERMISSIONS[perm] ?: 0
                if (weight > 0) {
                    riskScore += weight
                    details.add(perm.substringAfterLast("."))
                }
            }

            // Check for suspicious app name patterns
            val suspiciousPatterns = listOf("hack", "crack", "spy", "steal", "trojan", "virus", "miner")
            for (pattern in suspiciousPatterns) {
                if (appName.lowercase().contains(pattern)) {
                    riskScore += 5
                    details.add("Подозрительное имя: $pattern")
                    break
                }
            }

            // Check if system app
            val appInfo = pm.getApplicationInfo(packageName, 0)
            if ((appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 ||
                (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                riskScore = (riskScore * 0.3).toInt()
            }
        } catch (e: Exception) {
            // Ignore
        }

        // Determine threat level
        val threatLevel = when {
            riskScore >= 10 -> ThreatLevel.CRITICAL
            riskScore >= 7 -> ThreatLevel.HIGH
            riskScore >= 4 -> ThreatLevel.MEDIUM
            else -> ThreatLevel.LOW
        }

        val reason = when {
            details.any { it.contains("Сигнатура") } -> "Обнаружены вредоносные сигнатуры"
            details.contains("BIND_ACCESSIBILITY_SERVICE") -> "Требует доступ к спец. возможностям"
            details.contains("SEND_SMS") || details.contains("PROCESS_OUTGOING_CALLS") -> 
                "Может отправлять SMS/звонить"
            details.isNotEmpty() -> "Опасные разрешения: ${details.take(3).joinToString(", ")}"
            else -> "Приложение безопасно"
        }

        return ScanResult(threatLevel, riskScore.coerceAtMost(10), reason, details)
    }
}
