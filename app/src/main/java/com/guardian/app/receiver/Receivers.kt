package com.guardian.app.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsMessage
import android.telephony.TelephonyManager
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

// ==================== SMS FILTER ====================

// Enhanced anti-fraud keywords with categories
private val SCAM_KEYWORDS = mapOf(
    // Banking fraud
    "банк" to 3, "карта" to 3, "карту" to 3, "счет" to 2, "счёт" to 2,
    "блокировка" to 3, "заблокирован" to 3, "suspended" to 3, "account" to 2,
    "пароль" to 3, "password" to 3, "код" to 2, "code" to 2, "подтверждение" to 2,
    "cvv" to 5, "cvc" to 5, "pin" to 5, "пин" to 5,
    
    // Prize/lottery scams
    "вы выиграли" to 4, "winner" to 4, "prize" to 4, "lottery" to 4, 
    "миллион" to 3, "million" to 3, "выигрыш" to 4, "jackpot" to 4,
    
    // Urgency/Social engineering
    "срочно" to 2, "urgent" to 2, "немедленно" to 3, "срочное" to 2,
    "требуется" to 1, "требуется действие" to 3, "action required" to 3,
    "время истекает" to 3, "time running out" to 3, "последний шанс" to 3,
    
    // Money transfer
    "перевод" to 2, "перевести" to 3, "перечисление" to 3, "зачисление" to 2,
    "получить деньги" to 3, "send money" to 3, "transfer" to 2,
    
    // Government impersonation
    "налоговая" to 4, "судебный" to 4, "пристав" to 4, "долг" to 2,
    "задолженность" to 3, "штраф" to 3, "fsb" to 5, "фсб" to 5, "милиция" to 4,
    "полиция" to 4, "мигрант" to 3, "миграционная" to 3, "паспорт" to 2,
    
    // Investment/crypto scams
    "крипто" to 3, "bitcoin" to 3, "биткоин" to 3, "инвестиция" to 3,
    "удвоитель" to 5, "100% прибыль" to 5, "быстрые деньги" to 4,
    "get rich" to 4, "earn money fast" to 4, "forex" to 3, "бинарные опционы" to 5,
    
    // Fake services
    "смс" to 1, "sms" to 1, "короткий номер" to 2, "подписка" to 2,
    "бесплатно" to 1, "free" to 1, "акция" to 1, "скидка" to 1,
    
    // Account security
    "взлом" to 4, "хакер" to 4, "восстановить" to 2, "подозрительный вход" to 4,
    "new login" to 3, "access" to 2, "авторизация" to 2,
    
    // Delivery scams
    "посылка" to 2, "доставка" to 1, "ozon" to 2, "wildberries" to 2,
    "почта россии" to 2, "дпд" to 2, "cdek" to 2, "сдэк" to 2
)

// Known spam sender patterns
private val SPAM_SENDER_PATTERNS = listOf(
    "[0-9]{5,}", // Short numbers
    "[A-Za-z0-9]{10,}", // Alphanumeric spammers
)

// Suspicious URLs in SMS with severity
private val SUSPICIOUS_URLS = mapOf(
    // URL shorteners
    "bit.ly" to 2, "goo.gl" to 2, "tinyurl" to 2, "t.co" to 2, "is.gd" to 2,
    "vk.cc" to 2, "tiny.cc" to 2, "cutt.ly" to 2, "shorturl" to 2,
    "rb.gy" to 2, "short.link" to 2, "clck.ru" to 2,
    // Suspicious TLDs
    ".xyz" to 3, ".top" to 3, ".click" to 3, ".link" to 2, ".info" to 2,
    ".tk" to 3, ".ml" to 3, ".cf" to 3, ".ga" to 3
)

// Phishing patterns
private val PHISHING_PATTERNS = listOf(
    Pattern.compile("https?://[^\\s]*(?:sber|vtb|tinkoff|alfa|raiffeisen)[^\\s]*\\.(?:xyz|top|click|tk|ml)", Pattern.CASE_INSENSITIVE),
    Pattern.compile("https?://[^\\s]*(?:login|signin|auth|verify)[^\\s]*\\.(?:com|ru|net)", Pattern.CASE_INSENSITIVE),
    Pattern.compile("ваша?\\s*(?:карта|карточка|счёт|счет).*?(?:заблокирован|приостановлен|истекает)", Pattern.CASE_INSENSITIVE),
    Pattern.compile("подтвердите?\\s*(?:операцию|платёж|перевод)", Pattern.CASE_INSENSITIVE),
    Pattern.compile("войдите?\\s*(?:в|через)\\s*(?:приложение|личный\\s*кабинет)", Pattern.CASE_INSENSITIVE),
    Pattern.compile("(?:ограничены|ограничение|приостановлен).*?(?:действие|операции)", Pattern.CASE_INSENSITIVE),
)

// ==================== CALL FILTER ====================

// Known spam/scam number prefixes by country
private val SPAM_PREFIXES = mapOf(
    // Russia
    "+44" to 3,          // UK (often spoofed)
    "+1-900" to 5,       // US premium
    "+1-800" to 2,       // US toll-free (some legitimate)
    "+7-900" to 2,       // Russia mobile
    "+7-901" to 2,
    "+7-902" to 2,
    "+7-904" to 2,
    "+7-908" to 2,
    "+7-950" to 2,
    "+7-951" to 2,
    "+7-952" to 2,
    "+7-953" to 2,
    "+7-960" to 2,
    "+7-961" to 2,
    "+7-962" to 2,
    "+7-963" to 2,
    "+7-964" to 2,
    "+7-965" to 2,
    "+7-966" to 2,
    "+7-967" to 2,
    "+7-968" to 2,
    "+7-969" to 2,
    "+7-980" to 2,
    "+7-981" to 2,
    "+7-982" to 2,
    "+7-983" to 2,
    "+7-984" to 2,
    "+7-985" to 2,
    "+7-987" to 2,
    "+7-988" to 2,
    "+7-989" to 2,
    "+7-991" to 2,
    "+7-992" to 2,
    "+7-993" to 2,
    "+7-994" to 2,
    "+7-995" to 2,
    "+7-996" to 2,
    "+7-997" to 2,
    "+7-998" to 2,
    "+7-999" to 2,
)

// Premium rate numbers
private val PREMIUM_NUMBERS = listOf(
    "809", "8095", "8096", "8097", "8098", // Russia premium
    "0700", "0808", "0900", // UK premium
    "1900", // US premium
)

// Known scam patterns in caller ID
private val SCAM_CALLER_PATTERNS = listOf(
    Pattern.compile("^\\+?7[0-9]{9}$"), // Russian mobile without country code
    Pattern.compile("^8[0-9]{10}$"),    // Russian 8-prefix
)

// ==================== APP MONITORING ====================

// Dangerous permissions that indicate potential malware
private val DANGEROUS_PERMISSIONS = mapOf(
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

// Suspicious app name patterns
private val SUSPICIOUS_APP_PATTERNS = listOf(
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

// Known malware package patterns
private val MALWARE_PACKAGE_PATTERNS = listOf(
    Pattern.compile(".*\\.hack.*"),
    Pattern.compile(".*\\.crack.*"),
    Pattern.compile(".*\\.spy.*"),
    Pattern.compile(".*\\.trojan.*"),
    Pattern.compile(".*\\.malware.*"),
    Pattern.compile("com\\.android\\.vending\\.billing.*"), // Fake billing
)

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

// ==================== SMS ANALYSIS ====================

private fun analyzeSms(text: String, sender: String): Pair<Boolean, Int> {
    val lowerText = text.lowercase()
    var riskScore = 0
    val reasons = mutableListOf<String>()
    
    // Check keywords with weights
    for ((keyword, weight) in SCAM_KEYWORDS) {
        if (lowerText.contains(keyword.lowercase())) {
            riskScore += weight
            reasons.add(keyword)
        }
    }
    
    // Check suspicious URLs
    for ((url, weight) in SUSPICIOUS_URLS) {
        if (lowerText.contains(url)) {
            riskScore += weight
            reasons.add("suspicious URL: $url")
        }
    }
    
    // Check phishing patterns
    for (pattern in PHISHING_PATTERNS) {
        if (pattern.matcher(lowerText).find()) {
            riskScore += 4
            reasons.add("phishing pattern detected")
            break
        }
    }
    
    // Check sender
    if (sender.matches(Regex("[0-9]{4,5}"))) {
        riskScore += 1 // Short number
    }
    
    // Check for urgency indicators
    val urgencyCount = listOf("!", "срочно", "немедленно", "сейчас", "сегодня", "immediately", "now", "urgent", "asap")
        .count { lowerText.contains(it) }
    riskScore += urgencyCount
    
    // Check for mixed scripts (confusing letters)
    if (text.contains(Regex("[а-я]")) && text.contains(Regex("[a-z]"))) {
        if (text.contains(Regex("[оo]|['иi]|[аa]|[рp]|[еe]|[сc]"))) {
            riskScore += 3 // Possible spoofing with similar letters
        }
    }
    
    return Pair(riskScore >= 4, riskScore)
}

private fun checkForScam(text: String): Boolean {
    return analyzeSms(text, "").first
}

private fun getScamReason(text: String): String {
    val lowerText = text.lowercase()
    val reasons = mutableListOf<String>()
    
    when {
        lowerText.contains(Regex("(банк|карта|cvv|pin|пароль)")) -> reasons.add("попытка кражи банковских данных")
        lowerText.contains(Regex("(выигрыш|prize|winner|million|миллион)")) -> reasons.add("мошенничество с призами")
        lowerText.contains(Regex("(инвестиция|bitcoin|крипто|удвоитель)")) -> reasons.add("финансовое мошенничество")
        lowerText.contains(Regex("(взлом|хакер|подозрительный вход)")) -> reasons.add("попытка кражи аккаунта")
        lowerText.contains(Regex("(налоговая|пристав|судебный|штраф)")) -> reasons.add("маскировка под государственные органы")
        SUSPICIOUS_URLS.keys.any { lowerText.contains(it) } -> reasons.add("подозрительная ссылка")
        else -> reasons.add("подозрительное содержимое")
    }
    
    return reasons.joinToString(", ")
}

// ==================== CALL ANALYSIS ====================

private fun analyzeCall(number: String): Pair<Boolean, String> {
    val cleanNumber = number.replace(Regex("[^0-9+]"), "")
    
    // Check premium rate numbers
    for (prefix in PREMIUM_NUMBERS) {
        if (cleanNumber.contains(prefix)) {
            return Pair(true, "премиум-номер (дорогой звонок)")
        }
    }
    
    // Check spam prefixes
    for ((prefix, severity) in SPAM_PREFIXES) {
        if (cleanNumber.startsWith(prefix)) {
            return Pair(severity >= 3, "подозрительный префикс $prefix")
        }
    }
    
    // Check for hidden/restricted numbers
    if (number == "Restricted" || number == "Unknown" || number == "Private") {
        return Pair(false, "скрытый номер")
    }
    
    // Check international spam patterns
    if (cleanNumber.startsWith("+")) {
        val countryCode = cleanNumber.substring(1, minOf(4, cleanNumber.length))
        when (countryCode) {
            "44", "1", "92", "94", "95", "98" -> return Pair(true, "международный спам")
        }
    }
    
    return Pair(false, "")
}

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

// ==================== PERMISSION CHECK ====================

private suspend fun isProtectionAndModuleEnabled(context: Context, module: String): Boolean {
    return try {
        val repository = GuardianRepository(context)
        val protectionEnabled = repository.isProtectionEnabled.first()
        if (!protectionEnabled) return false
        
        when (module) {
            "usb" -> repository.isUsbMonitorEnabled.first()
            "sms" -> repository.isSmsFilterEnabled.first()
            "call" -> repository.isCallFilterEnabled.first()
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
        
        for (perm in requestedPermissions) {
            val weight = DANGEROUS_PERMISSIONS[perm] ?: 0
            if (weight > 0) {
                riskScore += weight
                dangerousPerms.add(perm.substringAfterLast("."))
            }
        }
        
        // Check for suspicious app name
        val appName = getAppName(context, packageName)
        for (pattern in SUSPICIOUS_APP_PATTERNS) {
            if (pattern.matcher(appName).matches()) {
                riskScore += 5
                dangerousPerms.add("suspicious name")
                break
            }
        }
        
        // Check for suspicious package name
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

// ==================== RECEIVERS ====================

// Enhanced SMS Filter
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!isProtectionAndModuleEnabled(context, "sms")) return@launch
                
                val bundle = intent.extras ?: return@launch
                val pdus = bundle.get("pdus") as? Array<*> ?: return@launch
                
                for (pdu in pdus) {
                    @Suppress("DEPRECATION")
                    val sms = SmsMessage.createFromPdu(pdu as ByteArray)
                    val message = sms.messageBody ?: continue
                    val sender = sms.originatingAddress ?: "Unknown"
                    
                    val (isScam, riskScore) = analyzeSms(message, sender)
                    
                    if (isScam) {
                        val reason = getScamReason(message)
                        
                        logEvent(
                            context, 
                            EventType.SMS_BLOCKED,
                            "⚠️ Мошенническое SMS",
                            "От: $sender - $reason (риск: $riskScore)",
                            sender
                        )
                        
                        showNotification(
                            context,
                            "⚠️ Подозрительное SMS",
                            "От $sender: $reason",
                            "sms:$sender"
                        )
                        
                        // Optionally abort broadcast for high-risk SMS
                        if (riskScore >= 8 && isOrderedBroadcast) {
                            abortBroadcast()
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore errors
            }
        }
    }
}

// Enhanced Call Filter
class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.intent.action.PHONE_STATE") return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!isProtectionAndModuleEnabled(context, "call")) return@launch
                
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                if (state != TelephonyManager.EXTRA_STATE_RINGING) return@launch
                
                @Suppress("DEPRECATION")
                val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: return@launch
                
                val (isSpam, reason) = analyzeCall(number)
                
                if (isSpam) {
                    logEvent(
                        context,
                        EventType.CALL_BLOCKED,
                        "⚠️ Подозрительный звонок",
                        "С номера $number - $reason",
                        number
                    )
                    
                    showNotification(
                        context,
                        "⚠️ Подозрительный звонок",
                        "Номер $number - $reason",
                        "call:$number"
                    )
                }
            } catch (e: Exception) {
                // Ignore errors
            }
        }
    }
}

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
            "ru.megafon.mlk", "ru.mts.mymts", "ru.beeline.services", "ru.tele2.mytele2"
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
