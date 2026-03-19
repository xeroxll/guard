package com.guardian.app.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.guardian.app.GuardianApp
import com.guardian.app.R
import com.guardian.app.data.model.EventType
import com.guardian.app.data.repository.GuardianRepository
import com.guardian.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.regex.Pattern

// ==================== USB DEBUGGING ====================

// Track USB debugging state
private var lastUsbDebuggingState = false
private var usbDebuggingCheckInterval: java.util.Timer? = null

private fun isUsbDebuggingEnabled(context: Context): Boolean {
    return try {
        val secure = android.provider.Settings.Secure::class.java
        val method = secure.getMethod("getInt", android.content.ContentResolver::class.java, String::class.java, Int::class.javaPrimitiveType)
        val result = method.invoke(null, context.contentResolver, "adb_enabled", 0) as Int
        result == 1
    } catch (e: Exception) {
        false
    }
}

private fun isUsbConnected(context: Context): Boolean {
    return try {
        val intent = context.registerReceiver(null, android.content.IntentFilter("android.hardware.usb.action.USB_STATE"))
        intent?.getBooleanExtra("connected", false) ?: false
    } catch (e: Exception) {
        false
    }
}

private fun startUsbDebuggingMonitor(context: Context) {
    // Cancel any existing timer
    usbDebuggingCheckInterval?.cancel()
    
    // Check immediately
    val currentState = isUsbDebuggingEnabled(context)
    lastUsbDebuggingState = currentState
    
    // Start periodic checking
    usbDebuggingCheckInterval = java.util.Timer().apply {
        scheduleAtFixedRate(object : java.util.TimerTask() {
            override fun run() {
                CoroutineScope(Dispatchers.IO).launch {
                    checkUsbDebuggingStateChange(context)
                }
            }
        }, 5000, 5000) // Check every 5 seconds
    }
}

private suspend fun checkUsbDebuggingStateChange(context: Context) {
    try {
        if (!isProtectionAndModuleEnabled(context, "usb")) return
        
        val currentState = isUsbDebuggingEnabled(context)
        val isConnected = isUsbConnected(context)
        
        // State changed from disabled to enabled
        if (currentState && !lastUsbDebuggingState) {
            lastUsbDebuggingState = true
            
            val title = if (isConnected) {
                "🚨 КРИТИЧЕСКАЯ УГРОЗА! USB отладка + подключение"
            } else {
                "⚠️ USB отладка включена"
            }
            
            val message = if (isConnected) {
                "Устройство подключено по USB с включенной отладкой! Немедленно отключите!"
            } else {
                "USB отладка включена. Это позволяет получить полный доступ к устройству."
            }
            
            // Log event
            logEvent(
                context,
                EventType.USB_ENABLED,
                title,
                message
            )
            
            // Show notification
            showNotification(
                context,
                title,
                message,
                "usb_debug_enabled"
            )
        }
        // USB cable connected while debugging is enabled
        else if (currentState && isConnected && lastUsbNotificationTime > 0) {
            val timeSinceLastNotification = System.currentTimeMillis() - lastUsbNotificationTime
            if (timeSinceLastNotification > 300000) { // 5 minutes
                lastUsbNotificationTime = System.currentTimeMillis()
                
                showNotification(
                    context,
                    "⚠️ Устройство подключено по USB",
                    "USB отладка активна. Отключите отладку в настройках разработчика.",
                    "usb_connected_debug"
                )
            }
        }
        // State changed from enabled to disabled
        else if (!currentState && lastUsbDebuggingState) {
            lastUsbDebuggingState = false
            
            logEvent(
                context,
                EventType.USB_DISABLED,
                "✅ USB отладка отключена",
                "Устройство в безопасности"
            )
            
            showNotification(
                context,
                "✅ USB отладка отключена",
                "Устройство защищено",
                "usb_debug_disabled"
            )
        }
    } catch (e: Exception) {
        // Ignore errors
    }
}

// Global flag to prevent repeated notifications
private var lastUsbNotificationTime = 0L
private val recentNotifications = mutableSetOf<String>()
private const val NOTIFICATION_COOLDOWN = 60000L // 1 minute

private fun showNotification(context: Context, title: String, message: String, id: String = "") {
    try {
        // Prevent duplicate notifications
        val notificationKey = "$title:$message"
        if (recentNotifications.contains(notificationKey)) return
        
        recentNotifications.add(notificationKey)
        CoroutineScope(Dispatchers.IO).launch {
            kotlinx.coroutines.delay(NOTIFICATION_COOLDOWN)
            recentNotifications.remove(notificationKey)
        }
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, GuardianApp.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    } catch (e: Exception) {
        // Silently fail
    }
}

private fun logEvent(context: Context, type: EventType, title: String, message: String, packageName: String? = null) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val repository = GuardianRepository(context)
            repository.addEvent(type, title, message, packageName)
        } catch (e: Exception) {
            // Ignore
        }
    }
}

// ==================== PERMISSION CHECK ====================

private suspend fun isProtectionAndModuleEnabled(context: Context, module: String): Boolean {
    return try {
        val repository = GuardianRepository(context)
        val protectionEnabled = repository.isProtectionEnabled.first()
        if (!protectionEnabled) return false
        
        when (module) {
            "usb" -> repository.isUsbMonitorEnabled.first()
            "app" -> repository.isAppMonitorEnabled.first()
            else -> false
        }
    } catch (e: Exception) {
        false
    }
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

private fun analyzeAppPermissions(context: Context, packageName: String): Pair<Int, List<String>> {
    var riskScore = 0
    val dangerousPerms = mutableListOf<String>()
    
    try {
        val pm = context.packageManager
        val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        val requestedPermissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
        
        val DANGEROUS_PERMISSIONS = mapOf(
            "android.permission.SEND_SMS" to 4,
            "android.permission.READ_SMS" to 3,
            "android.permission.RECEIVE_SMS" to 3,
            "android.permission.CALL_PHONE" to 3,
            "android.permission.READ_CALL_LOG" to 3,
            "android.permission.WRITE_CALL_LOG" to 3,
            "android.permission.PROCESS_OUTGOING_CALLS" to 4,
            "android.permission.READ_CONTACTS" to 2,
            "android.permission.WRITE_CONTACTS" to 2,
            "android.permission.RECORD_AUDIO" to 3,
            "android.permission.CAMERA" to 2,
            "android.permission.ACCESS_FINE_LOCATION" to 2,
            "android.permission.ACCESS_COARSE_LOCATION" to 1,
            "android.permission.READ_PHONE_STATE" to 2,
            "android.permission.READ_EXTERNAL_STORAGE" to 1,
            "android.permission.WRITE_EXTERNAL_STORAGE" to 1,
            "android.permission.SYSTEM_ALERT_WINDOW" to 3,
            "android.permission.BIND_ACCESSIBILITY_SERVICE" to 5,
            "android.permission.DEVICE_POWER" to 3,
            "android.permission.KILL_BACKGROUND_PROCESSES" to 2,
            "android.permission.RECEIVE_BOOT_COMPLETED" to 1,
        )
        
        for (perm in requestedPermissions) {
            val weight = DANGEROUS_PERMISSIONS[perm] ?: 0
            if (weight > 0) {
                riskScore += weight
                dangerousPerms.add(perm.substringAfterLast("."))
            }
        }
        
        // Check for suspicious app name
        val SUSPICIOUS_APP_PATTERNS = listOf(
            Pattern.compile(".*hack.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*crack.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*keylog.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*spy.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*steal.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*trojan.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*virus.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*miner.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*free.*premium.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*free.*vip.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*bypass.*", Pattern.CASE_INSENSITIVE),
        )
        
        val appName = getAppName(context, packageName)
        for (pattern in SUSPICIOUS_APP_PATTERNS) {
            if (pattern.matcher(appName).matches()) {
                riskScore += 5
                dangerousPerms.add("suspicious name")
                break
            }
        }
        
        // Check for suspicious package name
        val MALWARE_PACKAGE_PATTERNS = listOf(
            Pattern.compile(".*\\.hack.*"),
            Pattern.compile(".*\\.crack.*"),
            Pattern.compile(".*\\.spy.*"),
            Pattern.compile(".*\\.trojan.*"),
            Pattern.compile(".*\\.malware.*"),
            Pattern.compile("com\\.android\\.vending\\.billing.*"), // Fake billing
        )
        
        for (pattern in MALWARE_PACKAGE_PATTERNS) {
            if (pattern.matcher(packageName).matches()) {
                riskScore += 5
                dangerousPerms.add("suspicious package")
                break
            }
        }
        
    } catch (e: Exception) {
        // Ignore
    }
    
    return Pair(riskScore, dangerousPerms)
}

// ==================== USB MONITOR ====================

// Enhanced USB Debugging Monitor with comprehensive security checks
class UsbMonitorReceiver : BroadcastReceiver() {
    companion object {
        private var lastAdbState: Boolean? = null
        private var lastNotificationTime = 0L
        private const val NOTIFICATION_COOLDOWN = 300000L // 5 minutes
        
        // Known forensic/crime lab tools that use ADB
        private val FORENSIC_TOOLS = listOf(
            "com.cellebrite",
            "com.msab.xry",
            "com.oxygen",
            "com.elcomsoft",
            "com.susteen",
            "com.paraben",
            "com.blackbag",
            "com.magnetforensics",
            "com.solutionary"
        )
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                CoroutineScope(Dispatchers.IO).launch {
                    performUsbSecurityCheck(context)
                }
            }
            "android.hardware.usb.action.USB_STATE" -> {
                val connected = intent.getBooleanExtra("connected", false)
                val configured = intent.getBooleanExtra("configured", false)
                
                CoroutineScope(Dispatchers.IO).launch {
                    if (connected) {
                        performUsbSecurityCheck(context, configured)
                    }
                }
            }
            Intent.ACTION_POWER_CONNECTED -> {
                CoroutineScope(Dispatchers.IO).launch {
                    // Could be USB connection
                    kotlinx.coroutines.delay(1000) // Wait for USB state to stabilize
                    performUsbSecurityCheck(context)
                }
            }
        }
    }
    
    private suspend fun performUsbSecurityCheck(context: Context, usbConfigured: Boolean = false) {
        try {
            if (!isProtectionAndModuleEnabled(context, "usb")) return
            
            // Check ADB state
            val adbEnabled = isUsbDebuggingEnabled(context)
            
            // Check if state changed from last check
            if (lastAdbState == null) {
                lastAdbState = adbEnabled
                if (adbEnabled) {
                    notifyAdbEnabled(context)
                }
            } else if (lastAdbState != adbEnabled) {
                lastAdbState = adbEnabled
                if (adbEnabled) {
                    notifyAdbEnabled(context)
                } else {
                    notifyAdbDisabled(context)
                }
            } else if (adbEnabled) {
                // ADB still enabled, check cooldown and notify
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastNotificationTime >= NOTIFICATION_COOLDOWN) {
                    notifyAdbEnabled(context, isReminder = true)
                }
            }
            
            // Check for forensic tools regardless of ADB state
            checkForForensicTools(context)
            
        } catch (e: Exception) {
            // Ignore errors
        }
    }
    
    private suspend fun notifyAdbEnabled(context: Context, isReminder: Boolean = false) {
        lastNotificationTime = System.currentTimeMillis()
        
        val title = if (isReminder) "🔔 Напоминание: Отладка USB включена" else "🚨 ВНИМАНИЕ! Отладка USB АКТИВНА"
        val message = if (isReminder) 
            "Отладка USB все еще включена. Ваше устройство уязвимо для атак через компьютер."
        else 
            "Ваше устройство доступно для подключения к компьютеру! Злоумышленники могут извлечь данные без разблокировки."
        
        showNotification(
            context,
            title,
            message,
            "usb_debug_${System.currentTimeMillis()}"
        )
        
        logEvent(
            context,
            EventType.USB_ENABLED,
            title,
            "Критическая угроза безопасности: устройство доступно через ADB. Любой компьютер может получить доступ к данным без PIN/пароля."
        )
    }
    
    private suspend fun notifyAdbDisabled(context: Context) {
        logEvent(
            context,
            EventType.USB_DISABLED,
            "✅ Отладка USB отключена",
            "Устройство защищено от ADB-атак"
        )
    }
    
    private suspend fun checkForForensicTools(context: Context) {
        try {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            
            for (app in packages) {
                val packageName = app.packageName.lowercase()
                for (tool in FORENSIC_TOOLS) {
                    if (packageName.contains(tool)) {
                        val appName = pm.getApplicationLabel(app).toString()
                        
                        showNotification(
                            context,
                            "🚨 ОБНАРУЖЕНА КРИМИНАЛИСТИЧЕСКАЯ УТИЛИТА",
                            "Приложение $appName ($packageName) может использоваться для извлечения данных с вашего устройства.",
                            "forensic_tool"
                        )
                        
                        logEvent(
                            context,
                            EventType.APP_BLOCKED,
                            "🚨 Обнаружена криминалистическая утилита",
                            "$appName ($packageName) - инструмент цифровой криминалистики, используемый полицией и спецслужбами для извлечения данных с телефонов. Может обходить шифрование при включенной отладке USB.",
                            packageName
                        )
                        
                        return
                    }
                }
            }
        } catch (e: Exception) { }
    }
}

// ==================== APP MONITORING ====================

// Enhanced App installation tracker with virus scanning
class AppMonitorReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return
        val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!isProtectionAndModuleEnabled(context, "app")) return@launch
                
                val repository = GuardianRepository(context)
                val appName = getAppName(context, packageName)
                
                when (intent.action) {
                    Intent.ACTION_PACKAGE_ADDED -> {
                        if (!isReplacing) {
                            // Perform full security scan on newly installed app
                            val scanResult = performSecurityScan(context, packageName, appName)
                            
                            when (scanResult.threatLevel) {
                                ThreatLevel.CRITICAL -> {
                                    logEvent(
                                        context, 
                                        EventType.APP_BLOCKED,
                                        "🚨 ВРЕДОНОСНОЕ ПРИЛОЖЕНИЕ!",
                                        "$appName - ${scanResult.reason} (риск: ${scanResult.riskScore}/10)",
                                        packageName
                                    )
                                    showNotification(
                                        context,
                                        "🚨 ВРЕДОНОСНОЕ ПРИЛОЖЕНИЕ УСТАНОВЛЕНО!",
                                        "$appName - ${scanResult.reason}. Рекомендуется немедленное удаление!",
                                        "app_threat:$packageName"
                                    )
                                }
                                ThreatLevel.HIGH -> {
                                    logEvent(
                                        context,
                                        EventType.APP_BLOCKED,
                                        "⚠️ Подозрительное приложение",
                                        "$appName - ${scanResult.reason} (риск: ${scanResult.riskScore}/10)",
                                        packageName
                                    )
                                    showNotification(
                                        context,
                                        "⚠️ Опасное приложение установлено",
                                        "$appName - ${scanResult.reason}",
                                        "app_suspicious:$packageName"
                                    )
                                }
                                ThreatLevel.MEDIUM -> {
                                    logEvent(
                                        context,
                                        EventType.APP_INSTALLED,
                                        "📱 Приложение установлено (проверено)",
                                        "$appName - обнаружены потенциально опасные разрешения",
                                        packageName
                                    )
                                    showNotification(
                                        context,
                                        "📱 Приложение установлено",
                                        "$appName - требует внимания (риск: ${scanResult.riskScore})",
                                        "app_installed:$packageName"
                                    )
                                }
                                ThreatLevel.LOW -> {
                                    logEvent(
                                        context,
                                        EventType.APP_INSTALLED,
                                        "📱 Приложение установлено",
                                        "$appName - безопасно",
                                        packageName
                                    )
                                    showNotification(
                                        context,
                                        "✅ Приложение установлено",
                                        "$appName - проверка завершена, угроз не обнаружено",
                                        "app_safe:$packageName"
                                    )
                                }
                            }
                        } else {
                            // App updated - quick check
                            val (riskScore, dangerousPerms) = analyzeAppPermissions(context, packageName)
                            
                            if (riskScore >= 10) {
                                logEvent(
                                    context,
                                    EventType.APP_BLOCKED,
                                    "🔄 Обновление с угрозой",
                                    "$appName - после обновления обнаружены опасные разрешения (риск: $riskScore)",
                                    packageName
                                )
                                showNotification(
                                    context,
                                    "⚠️ Опасное обновление",
                                    "$appName - новая версия содержит подозрительные разрешения",
                                    "app_update:$packageName"
                                )
                            } else {
                                logEvent(
                                    context,
                                    EventType.APP_INSTALLED,
                                    "🔄 Приложение обновлено",
                                    "$appName",
                                    packageName
                                )
                            }
                        }
                    }
                    Intent.ACTION_PACKAGE_REMOVED -> {
                        if (!isReplacing) {
                            logEvent(
                                context,
                                EventType.APP_REMOVED,
                                "🗑️ Приложение удалено",
                                appName,
                                packageName
                            )
                            showNotification(
                                context,
                                "🗑️ Приложение удалено",
                                "$appName было удалено с устройства",
                                "app_removed:$packageName"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore errors
            }
        }
    }
    
    // Comprehensive security scan result
    data class ScanResult(
        val threatLevel: ThreatLevel,
        val riskScore: Int,
        val reason: String,
        val details: List<String>
    )
    
    enum class ThreatLevel {
        CRITICAL, HIGH, MEDIUM, LOW
    }
    
    // Perform comprehensive security scan on app
    private fun performSecurityScan(context: Context, packageName: String, appName: String): ScanResult {
        val details = mutableListOf<String>()
        var riskScore = 0
        
        // Check 1: Is it a known trusted app?
        val trustedApps = setOf(
            "com.guardian.app", "com.google.android.youtube", "com.whatsapp", "org.telegram.messenger",
            "com.vkontakte.android", "com.instagram.android", "com.facebook.katana",
            "ru.sberbankmobile", "com.idamob.tinkoff.android", "ru.yandex.searchplugin",
            "ru.megafon.mlk", "ru.mts.mymts", "ru.beeline.services", "ru.tele2.mytele2",
            // Movies/Streaming
            "com.kinopoisk", "ru.kinopoisk", "com.max.app", "com.wb.max",
            // Classifieds
            "ru.avito", "ru.avito.debug", "com.avito",
            // Taxi
            "ru.yandex.taxi", "com.yandex.taxi", "com.ubertaxi", "ru.taxsee.taxsee",
            // Camera/Photo
            "com.relens.android", "com.relens", "com.adobe.lightroom", "com.vsco.cam",
            // Food delivery
            "ru.sushisea", "com.sushisea", "ru.chibbis", "com.chibbis", "com.deliveryclub"
        )
        
        if (trustedApps.contains(packageName) || 
            packageName.startsWith("com.google.android") ||
            packageName.startsWith("com.android") ||
            packageName.startsWith("ru.yandex")) {
            return ScanResult(ThreatLevel.LOW, 0, "Доверенное приложение", emptyList())
        }
        
        // Check 2: Malware signatures in package name
        val malwareSignatures = mapOf(
            "trojan" to 10, "backdoor" to 10, "rat" to 9, "spyware" to 9,
            "spy" to 7, "keylog" to 10, "stealer" to 9, "ransomware" to 10,
            "banker" to 10, "bankbot" to 10, "smsfraud" to 9, "miner" to 8,
            "adware" to 6, "riskware" to 7, "hack" to 7, "cracker" to 7,
            "dropper" to 9, "rootkit" to 10, "botnet" to 10, "worm" to 9,
            "fake" to 7, "fraud" to 8, "phishing" to 9, "pegasus" to 10,
            "anubis" to 10, "cerberus" to 10, "eventbot" to 10, "sharkbot" to 10
        )
        
        val lowerPackage = packageName.lowercase()
        for ((signature, score) in malwareSignatures) {
            if (lowerPackage.contains(signature)) {
                riskScore += score
                details.add("Сигнатура malware: $signature")
            }
        }
        
        // Check 3: Analyze permissions
        val (permRisk, dangerousPerms) = analyzeAppPermissions(context, packageName)
        riskScore += permRisk
        if (dangerousPerms.isNotEmpty()) {
            details.addAll(dangerousPerms.take(5))
        }
        
        // Check 4: Is it a system app?
        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            if ((appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 ||
                (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                // System apps are generally trusted
                riskScore = (riskScore * 0.3).toInt() // Reduce risk for system apps
                details.add("Системное приложение")
            }
        } catch (e: Exception) { }
        
        // Determine threat level
        val threatLevel = when {
            riskScore >= 10 -> ThreatLevel.CRITICAL
            riskScore >= 7 -> ThreatLevel.HIGH
            riskScore >= 4 -> ThreatLevel.MEDIUM
            else -> ThreatLevel.LOW
        }
        
        val reason = when {
            details.any { it.contains("malware") || it.contains("Сигнатура") } -> 
                "Обнаружены вредоносные сигнатуры"
            details.any { it.contains("BIND_ACCESSIBILITY") } -> 
                "Требует доступ к специальным возможностям (может перехватывать ввод)"
            details.any { it.contains("SEND_SMS") || it.contains("PROCESS_OUTGOING_CALLS") } -> 
                "Может отправлять SMS/звонить без вашего ведома"
            details.any { it.contains("READ_SMS") || it.contains("READ_CONTACTS") } -> 
                "Доступ к личным данным (SMS, контакты)"
            dangerousPerms.isNotEmpty() -> 
                "Опасные разрешения: ${dangerousPerms.take(3).joinToString(", ")}"
            else -> "Приложение безопасно"
        }
        
        return ScanResult(threatLevel, riskScore.coerceAtMost(10), reason, details)
    }
}
