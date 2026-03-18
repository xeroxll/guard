package com.guardian.app.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guardian.app.data.api.ScanResult
import com.guardian.app.data.api.VirusTotalResult
import com.guardian.app.data.api.VirusTotalService
import com.guardian.app.data.model.AppStats
import com.guardian.app.data.model.BlacklistedApp
import com.guardian.app.data.model.EventType
import com.guardian.app.data.model.ScanHistory
import com.guardian.app.data.model.ScanType
import com.guardian.app.data.model.SecurityEvent
import com.guardian.app.data.model.TrustedApp
import com.guardian.app.data.notification.NotificationHelper
import com.guardian.app.data.repository.GuardianRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GuardianViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = GuardianRepository(application)
    private val virusTotalService = VirusTotalService(application)
    
    // Main protection
    val isProtectionEnabled = repository.isProtectionEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    
    // Theme
    val isDarkTheme = repository.isDarkTheme.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    
    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDarkTheme(enabled)
        }
    }
    
    // Individual module toggles
    val isUsbMonitorEnabled = repository.isUsbMonitorEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val isSmsFilterEnabled = repository.isSmsFilterEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val isCallFilterEnabled = repository.isCallFilterEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val isAppMonitorEnabled = repository.isAppMonitorEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    
    // USB Debug state - check actual system setting
    val isUsbDebuggingEnabled: Boolean
        get() = android.provider.Settings.Global.getInt(
            getApplication<Application>().contentResolver,
            android.provider.Settings.Global.ADB_ENABLED, 0
        ) == 1
    
    // Data
    val blacklist = repository.blacklist.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val trustedApps = repository.trustedApps.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val events = repository.events.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val stats = repository.stats.stateIn(viewModelScope, SharingStarted.Eagerly, AppStats())
    val scanHistory = repository.scanHistory.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    
    // VirusTotal scan state
    private val _virusTotalResults = MutableStateFlow<Map<String, VirusTotalResult>>(emptyMap())
    val virusTotalResults: StateFlow<Map<String, VirusTotalResult>> = _virusTotalResults.asStateFlow()
    
    private val _isVirusTotalScanning = MutableStateFlow(false)
    val isVirusTotalScanning: StateFlow<Boolean> = _isVirusTotalScanning.asStateFlow()
    
    private val _virusTotalProgress = MutableStateFlow(Pair(0, 0))
    val virusTotalProgress: StateFlow<Pair<Int, Int>> = _virusTotalProgress.asStateFlow()
    
    // Computed
    val threats get() = events.value.count { it.type == EventType.APP_BLOCKED || it.type == EventType.USB_ENABLED }
    val blocks get() = stats.value.threatsBlocked
    val checks get() = stats.value.appsScanned
    val lastScanTime get() = stats.value.lastScanTime
    
    // Setters
    fun setProtectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setProtectionEnabled(enabled)
            repository.addEvent(
                if (enabled) EventType.PROTECTION_ENABLED else EventType.PROTECTION_DISABLED,
                if (enabled) "🛡️ Защита включена" else "⏸️ Защита выключена",
                "Security protection has been ${if (enabled) "enabled" else "disabled"}"
            )
        }
    }
    
    fun setUsbMonitorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setUsbMonitorEnabled(enabled)
            repository.addEvent(
                EventType.SCAN_COMPLETED,
                "🔌 USB Monitor",
                "USB monitoring ${if (enabled) "enabled" else "disabled"}"
            )
        }
    }
    
    // Open developer settings to disable USB debugging
    fun openDeveloperSettings(): android.content.Intent {
        return android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
    }
    
    fun setSmsFilterEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSmsFilterEnabled(enabled)
            repository.addEvent(
                EventType.SCAN_COMPLETED,
                "📱 SMS Filter",
                "SMS filtering ${if (enabled) "enabled" else "disabled"}"
            )
        }
    }
    
    fun setCallFilterEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setCallFilterEnabled(enabled)
            repository.addEvent(
                EventType.SCAN_COMPLETED,
                "📞 Call Filter",
                "Call filtering ${if (enabled) "enabled" else "disabled"}"
            )
        }
    }
    
    fun setAppMonitorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAppMonitorEnabled(enabled)
            repository.addEvent(
                EventType.SCAN_COMPLETED,
                "📦 App Monitor",
                "App monitoring ${if (enabled) "enabled" else "disabled"}"
            )
        }
    }
    
    // Blacklist operations
    fun addToBlacklist(name: String, packageName: String) {
        viewModelScope.launch {
            repository.addToBlacklist(BlacklistedApp(name = name, packageName = packageName))
            // Remove from trusted if it was there
            val trusted = trustedApps.value.find { it.packageName == packageName }
            trusted?.let { repository.removeFromTrustedApps(it.id) }
        }
    }
    
    fun removeFromBlacklist(id: String) {
        viewModelScope.launch {
            repository.removeFromBlacklist(id)
        }
    }
    
    // Trusted apps (whitelist) operations
    fun addToTrustedApps(name: String, packageName: String) {
        viewModelScope.launch {
            repository.addToTrustedApps(TrustedApp(name = name, packageName = packageName))
            repository.addEvent(
                EventType.APP_BLOCKED,
                "✅ Добавлен в доверенные",
                "$packageName добавлен в список доверенных приложений",
                packageName
            )
        }
    }
    
    fun removeFromTrustedApps(id: String) {
        viewModelScope.launch {
            repository.removeFromTrustedApps(id)
        }
    }
    
    // Uninstall app - returns intent to start uninstall dialog
    fun getUninstallIntent(packageName: String): android.content.Intent {
        return android.content.Intent(android.content.Intent.ACTION_DELETE).apply {
            data = android.net.Uri.parse("package:$packageName")
        }
    }
    
    // Scan with advanced real threat detection
    fun startScan() {
        viewModelScope.launch {
            try {
                val pm = getApplication<Application>().packageManager
                val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                val blacklisted = blacklist.value.map { it.packageName }.toSet()
                
                var threatsFound = 0
                val detectedThreats = mutableListOf<Triple<String, String, Int>>() // name, reason, riskScore
                val scanResultsData = mutableListOf<com.guardian.app.ui.screens.AppScanResult>()
                
                // Known malware signatures and patterns
                val malwareSignatures = mapOf(
                    // Trojans
                    "trojan" to 10, "backdoor" to 10, "rat" to 9,
                    // Spyware
                    "spyware" to 9, "spy" to 7, "keylog" to 10, "keylogger" to 10,
                    "stealer" to 9, "password" to 5,
                    // Ransomware
                    "ransomware" to 10, "crypt" to 6, "encrypt" to 5,
                    // Banking trojans
                    "banker" to 10, "bankbot" to 10, "ankabot" to 10,
                    // SMS fraud
                    "smsfraud" to 9, "sms Trojan" to 10, "smspay" to 9,
                    "premium" to 5, "subscription" to 4,
                    // Miners
                    "miner" to 8, "mining" to 7, "bitcoin" to 5, "crypto" to 4,
                    // Adware
                    "adware" to 6, "ads" to 3,
                    // Riskware
                    "riskware" to 7, "hack" to 7, "cracker" to 7, "bypass" to 6,
                    "cheat" to 5, "mod" to 4, "patch" to 4,
                    // Droppers
                    "dropper" to 9, "loader" to 6, "injector" to 8,
                    // Rootkits
                    "rootkit" to 10, "root" to 4, "su" to 4,
                    // Botnet
                    "botnet" to 10, "bot" to 3, "zombie" to 6,
                    // Worms
                    "worm" to 9, "spread" to 4,
                    // Fake apps
                    "fake" to 7, "fraud" to 8, "phishing" to 9,
                    // Specific known malware families
                    "pegasus" to 10, "pegasusspy" to 10,
                    "nso" to 10, "candiru" to 10,
                    "anubis" to 10, "anubisthrat" to 10,
                    "cerberus" to 10, "cerberusthrat" to 10,
                    "eventbot" to 10, "gustuff" to 10,
                    "teabot" to 10, "toddler" to 10,
                    "sharkbot" to 10, "fluhorse" to 10,
                    // Obfuscation
                    "obfuscated" to 6, "packed" to 5
                )
                
                // Suspicious package patterns with risk scores
                val suspiciousPatterns = mapOf(
                    "com.android.vending.billing" to 8, // Fake billing
                    "com.svidersk" to 9,
                    "com.zmzpass" to 9,
                    "com.hack" to 8,
                    "com.cheat" to 7,
                    "com.crack" to 8,
                    "com.mod" to 5,
                    "free.vip" to 7,
                    "free.premium" to 7,
                    "free.pro" to 6,
                    "hack.tool" to 9,
                    "crack.tool" to 9,
                    "bypass.premium" to 8,
                    ".hacker" to 8,
                    ".cracker" to 8,
                    ".spy" to 8,
                    ".steal" to 9
                )
                
                // Known trusted system packages to exclude from threat detection
                val trustedPackages = setOf(
                    "com.android", "com.google.android", "android",
                    "com.samsung", "com.miui", "com.huawei", "com.xiaomi",
                    "com.oppo", "com.vivo", "com.oneplus", "com.sony",
                    "com.lge", "com.motorola", "com.asus",
                    "ru.yandex", "com.yandex"
                )
                
                // Known trusted apps (popular legitimate apps that should never be flagged)
                val trustedApps = setOf(
                    // Own app
                    "com.guardian.app",
                    
                    // Google apps
                    "com.google.android.youtube",
                    "com.google.android.apps.youtube.music",
                    "com.google.android.apps.youtube.kids",
                    "com.google.android.gm",  // Gmail
                    "com.google.android.apps.maps",
                    "com.google.android.apps.photos",
                    "com.google.android.apps.docs",
                    "com.google.android.apps.drive",
                    "com.google.android.apps.translate",
                    "com.google.android.apps.chrome",
                    "com.google.android.apps.wallet",
                    "com.google.android.apps.messaging",
                    "com.google.android.dialer",
                    "com.google.android.contacts",
                    "com.google.android.calendar",
                    "com.google.android.keep",
                    "com.google.android.apps.tasks",
                    "com.google.android.play.games",
                    
                    // Social Media
                    "com.facebook.katana",
                    "com.facebook.orca",  // Messenger
                    "com.instagram.android",
                    "com.twitter.android",
                    "com.snapchat.android",
                    "com.whatsapp",
                    "com.whatsapp.w4b",  // Business
                    "org.telegram.messenger",
                    "org.telegram.messenger.web",
                    "org.telegram.plus",  // Telegram Plus
                    "com.viber.voip",
                    "com.skype.raider",
                    "com.linkedin.android",
                    "com.discord",
                    "com.zhiliaoapp.musically",  // TikTok
                    "com.ss.android.ugc.trill",
                    
                    // Chinese Social Media
                    "com.tencent.mm",  // WeChat
                    "com.tencent.mobileqq",  // QQ
                    "com.sina.weibo",  // Weibo
                    "com.ss.android.article.news",  // Toutiao
                    "com.xingin.xhs",  // Xiaohongshu
                    "tv.danmaku.bili",  // Bilibili
                    "com.Qunar",  // Qunar
                    "com.autonavi.minimap",  // Amap/Gaode
                    
                    // Russian Social/Games
                    "ru.mail.mailapp",  // Mail.ru
                    "ru.mail.android.my",  // My World
                    "com.mgerrit",  
                    "com.playrix.fishdom",
                    "com.playrix.township",
                    "com.playrix.homescapes",
                    "com.playrix.gardenscapes",
                    "com.wargaming.wot.blitz",  // World of Tanks
                    "com.lab666",
                    "ru.vk.store",
                    "ru.dublgis.dgismobile",  // 2GIS
                    
                    // Carriers/Operator apps
                    "ru.megafon.mlk",  // MegaFon
                    "ru.megafon.mlk.enterprise",
                    "com.megafon.digital",
                    "ru.mts.mymts",  // MTS
                    "ru.beeline.services",  // Beeline
                    "ru.beeline.b2c",
                    "ru.tele2.mytele2",  // Tele2
                    "ru.tele2.twa",
                    "ru.rtk.it",  // Rostelecom
                    "com.tinkoff.mvno",  // Tinkoff Mobile
                    "com.yota.android",  // Yota
                    
                    // Food delivery
                    "com.deliveryclub",
                    "com.deliveryclub.partner",
                    "com.samokat.android",
                    "ru.chibbis.base",
                    "ru.pizza.android",
                    
                    // Sports/Fitness
                    "com.nike.ntc",  // Nike Training
                    "com.nike.nrc",  // Nike Run
                    "com.strava",
                    "com.endomondo.android",
                    "com.sports.tracker",
                    
                    // News
                    "ru.rambler.news",
                    "ru.rambler.mail",
                    "com.gismeteo.gismeteo",
                    
                    // Other popular
                    "com.ubercab.driver",  // Uber Driver
                    "com.whatsapp.gbmods.gbwhatsapp",  // GBWhatsApp (popular mod)
                    "com.whatsapp.gbmods.gbwhatsapp2",
                    "com.gbwhatsapp",  // Another GBWhatsApp
                    
                    // MAX streaming
                    "com.max.app",  // MAX (HBO Max)
                    "com.wb.max",
                    "com.max.go",   // MAX GO
                    "com.hbo.max",  // HBO Max
                    
                    // Messenger MAX (popular messenger)
                    "com.max.messenger",
                    "com.max.messenger.app",
                    "ru.max.messenger",
                    "com.maxim.messenger",
                    
                    // Classifieds
                    "ru.avito",  // Avito
                    "ru.avito.debug",
                    "com.avito",
                    
                    // Camera apps
                    "com.relens.android",  // ReLens
                    "com.relens",
                    "com.adobe.lightroom",
                    "com.vsco.cam",
                    "com.vsco.android",
                    
                    // Sushi/Seafood delivery
                    "ru.sushisea",  // Sushisea
                    "com.sushisea",
                    
                    // More Chinese apps
                    "com.tencent.mobileqqi",  // QQ International
                    "com.tencent.qqlite",  // QQ Lite
                    "com.tencent.tim",  // TIM
                    "com.baidu.BaiduMap",
                    "com.baidu.searchbox",
                    "com.jingdong.app.mall",  // JD.com
                    "com.suning.mobile.ebuy",  // Suning
                    "com.tmall.wireless",  // Tmall
                    "com.taobao.taobao",  // Taobao
                    "com.eg.android.AlipayGphone",  // Alipay
                    "com.netease.cloudmusic",  // NetEase Music
                    "com.kugou.android",  // Kugou
                    "com.kuwo.player",  // Kuwo
                    "com.xiami.muslim",  // Xiami
                    
                    // Banking (major banks - add more as needed)
                    "ru.sberbankmobile",
                    "com.idamob.tinkoff.android",
                    "ru.alfabank.mobile.android",
                    "ru.vtb24.mobilebanking.android",
                    "com.raiffeisen",
                    "ru.raiffeisennews",
                    "com.tcsbank",
                    "ru.otkritie",
                    "com.vtb.mobilebank",
                    "ru.gazprombank.android.mobilebank",
                    "ru.smpbank",
                    "ru.psb",
                    "ru.nspk.mirpay",
                    
                    // Payment systems
                    "com.paypal.android.p2pmobile",
                    "com.venmo",
                    "com.google.android.apps.walletnfcrel",
                    "com.yandex.pay",
                    
                    // E-commerce
                    "com.amazon.mShop.android.shopping",
                    "com.amazon.dee.app",
                    "com.wildberries.ru",
                    "ru.ozon.app.android",
                    "com.alibaba.aliexpresshd",
                    "com.ebay.mobile",
                    "com.alibaba.intl.android.apps.poseidon",
                    
                    // Streaming
                    "com.netflix.mediaclient",
                    "com.spotify.music",
                    "com.apple.android.music",
                    "com.yandex.music",
                    "ru.yandex.music",
                    "ru.yandex.music.plugin",
                    "com.vkontakte.android",
                    "com.vk.music",
                    "com.kinopoisk",
                    "com.ok.android",
                    
                    // Yandex apps
                    "ru.yandex.searchplugin",
                    "ru.yandex.yandexmaps",
                    "ru.yandex.yandexnavi",
                    "ru.yandex.taxi",
                    "ru.yandex.uber",
                    "ru.yandex.disk",
                    "ru.yandex.mail",
                    "ru.yandex.weather",
                    "ru.yandex.translate",
                    "ru.yandex.browser",
                    "ru.yandex.searchapp",
                    "ru.yandex.speechkit",
                    "com.yandex.browser",
                    "com.yandex.launcher",
                    "ru.yandex.drive",
                    "ru.yandex.metro",
                    "ru.yandex.rasp",
                    "ru.yandex.auto",
                    "ru.yandex.broker",
                    "ru.yandex.money",
                    "ru.yandex.yandexmoney",
                    "ru.yandex.yandexpay",
                    "ru.yandex.yandexapp",
                    "ru.yandex.lavka",
                    "ru.yandex.food",
                    "ru.yandex.eda",
                    
                    // Shopping/Food
                    "com.yandex.food",
                    "com.yandex.eda",
                    "com.yandex.lavka",
                    "com.sberbank.sdakit.service",
                    
                    // Transport
                    "com.ubercab",
                    "com.yandex.taxi",
                    "com.taxsee.taxsee",
                    "ru.citymobil.driver",
                    
                    // Messengers
                    "com.microsoft.teams",
                    "com.microsoft.skype.teams",
                    "com.slack",
                    "com.zoom.us",
                    
                    // Utilities
                    "com.dropbox.android",
                    "com.microsoft.office.word",
                    "com.microsoft.office.excel",
                    "com.microsoft.office.powerpoint",
                    "com.adobe.reader"
                )
                
                // Get current trusted apps for checking
                val currentTrustedApps = trustedApps.replayCache.firstOrNull() ?: emptyList()
                val trustedPackageNames = currentTrustedApps.map { it.packageName }.toSet()
                
                // Check each app
                for (packageInfo in packages) {
                    val packageName = packageInfo.packageName.lowercase()
                    val appName = pm.getApplicationLabel(packageInfo).toString()
                    val isSystemApp = (packageInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                                       (packageInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                    val isTrusted = trustedPackageNames.contains(packageInfo.packageName) || 
                                    trustedPackageNames.any { packageInfo.packageName == it }
                    
                    // Skip trusted apps and system apps immediately
                    if (trustedApps.replayCache.firstOrNull()?.any { it.packageName == packageInfo.packageName } == true || 
                        trustedPackageNames.any { packageName == it.lowercase() } ||
                        isSystemApp ||
                        isTrusted) {
                        // Add as safe app
                        val scanResult = com.guardian.app.ui.screens.AppScanResult(
                            packageName = packageInfo.packageName,
                            appName = appName,
                            isThreat = false,
                            threatType = if (isTrusted) "Доверенное приложение" else "",
                            isSystemApp = isSystemApp
                        )
                        scanResultsData.add(scanResult)
                        continue
                    }
                    
                    var riskScore = 0
                    val threatReasons = mutableListOf<String>()
                    var isThreat = false
                    
                    // Check 1: Blacklist match
                    if (blacklisted.contains(packageInfo.packageName)) {
                        riskScore += 10
                        threatReasons.add("В черном списке")
                        isThreat = true
                    }
                    
                    // Check 2: Malware signatures in package name
                    for ((signature, score) in malwareSignatures) {
                        if (packageName.contains(signature.lowercase())) {
                            riskScore += score
                            threatReasons.add("Сигнатура: $signature")
                        }
                    }
                    
                    // Check 3: Suspicious patterns in package name
                    for ((pattern, score) in suspiciousPatterns) {
                        if (packageName.contains(pattern.lowercase())) {
                            riskScore += score
                            threatReasons.add("Подозрительный паттерн: $pattern")
                        }
                    }
                    
                    // Check 4: Analyze permissions
                    try {
                        val packageInfoFull = pm.getPackageInfo(packageInfo.packageName, PackageManager.GET_PERMISSIONS)
                        val requestedPermissions = packageInfoFull.requestedPermissions?.toList() ?: emptyList()
                        
                        // Critical permissions that indicate high risk
                        val criticalPerms = listOf(
                            "android.permission.BIND_ACCESSIBILITY_SERVICE",
                            "android.permission.SYSTEM_ALERT_WINDOW"
                        )
                        
                        // Dangerous permissions
                        val dangerousPerms = listOf(
                            "android.permission.READ_SMS",
                            "android.permission.SEND_SMS",
                            "android.permission.RECEIVE_SMS",
                            "android.permission.READ_CALL_LOG",
                            "android.permission.PROCESS_OUTGOING_CALLS",
                            "android.permission.CALL_PHONE",
                            "android.permission.READ_CONTACTS",
                            "android.permission.RECORD_AUDIO",
                            "android.permission.CAMERA",
                            "android.permission.ACCESS_FINE_LOCATION"
                        )
                        
                        val criticalCount = requestedPermissions.count { criticalPerms.contains(it) }
                        val dangerousCount = requestedPermissions.count { dangerousPerms.contains(it) }
                        
                        // High risk: accessibility service + SMS permissions (classic banking trojan pattern)
                        // Only flag if app is NOT from a trusted developer
                        val isTrustedDeveloper = trustedPackages.any { packageName.startsWith(it) }
                        if (criticalCount > 0 && dangerousCount >= 3 && !isTrustedDeveloper) {
                            riskScore += 8
                            threatReasons.add("Критические разрешения (возможный банковский троян)")
                        }
                        
                        // Medium risk: many dangerous permissions (only for non-system, non-trusted apps)
                        if (dangerousCount >= 7 && !isSystemApp && !isTrustedDeveloper) {
                            riskScore += 4
                            threatReasons.add("Много опасных разрешений ($dangerousCount)")
                        }
                        
                        // Check for SMS fraud pattern
                        val hasSmsPerms = requestedPermissions.any { it.contains("SMS") }
                        val hasCallPerms = requestedPermissions.any { it.contains("CALL") }
                        val hasContactPerms = requestedPermissions.contains("android.permission.READ_CONTACTS")
                        
                        if (hasSmsPerms && hasCallPerms && hasContactPerms && !isSystemApp && !isTrustedDeveloper) {
                            // Check if it's not a known messaging app (extended list)
                            val isKnownMessaging = listOf("whatsapp", "telegram", "signal", "viber", "messages", "sms", 
                                "phone", "dialer", "contacts", "facebook", "instagram", "twitter", "snapchat", "discord",
                                "skype", "linkedin", "wechat", "line", "kakao", "imo", "zalo")
                                .any { packageName.contains(it) }
                            
                            if (!isKnownMessaging) {
                                riskScore += 5
                                threatReasons.add("SMS/Call шпионаж")
                            }
                        }
                        
                    } catch (e: Exception) { }
                    
                    // Check 5: Heuristics - app name doesn't match package name pattern
                    // Many malware apps have innocent names but suspicious packages
                    if (!isSystemApp && riskScore >= 5) {
                        // Already flagged, additional verification
                        val isTrusted = trustedPackages.any { packageName.startsWith(it) }
                        if (!isTrusted && riskScore >= 8) {
                            isThreat = true
                        }
                    }
                    
                    // Determine if it's a threat based on risk score
                    // Higher threshold (9 instead of 7) to reduce false positives
                    isThreat = isThreat || riskScore >= 9
                    
                    // Create scan result
                    val scanResult = com.guardian.app.ui.screens.AppScanResult(
                        packageName = packageInfo.packageName,
                        appName = appName,
                        isThreat = isThreat,
                        threatType = if (isThreat) threatReasons.firstOrNull() ?: "Подозрительное приложение" else "",
                        isSystemApp = isSystemApp
                    )
                    scanResultsData.add(scanResult)
                    
                    if (isThreat) {
                        threatsFound++
                        detectedThreats.add(Triple(appName, threatReasons.firstOrNull() ?: "Угроза обнаружена", riskScore))
                        
                        repository.addEvent(
                            EventType.APP_BLOCKED,
                            "⚠️ ${if (riskScore >= 9) "ВРЕДОНОСНОЕ" else "Подозрительное"} приложение",
                            "$appName - ${threatReasons.joinToString(", ")} (риск: $riskScore/10)",
                            packageInfo.packageName
                        )
                        
                        // Show notification for high-risk threats
                        if (riskScore >= 8) {
                            NotificationHelper.showThreatFoundNotification(
                                getApplication(),
                                appName,
                                threatReasons.firstOrNull() ?: "Высокий риск"
                            )
                        }
                    }
                    
                    // Small delay for UI feedback
                    kotlinx.coroutines.delay(1)
                }
                
                // Save scan results for display
                _scanResults.value = scanResultsData
                
                // Update stats with threats found
                repository.updateScanStatsWithThreats(packages.size, threatsFound)
                
                // Save scan to history
                repository.addScanHistory(ScanHistory(
                    appsScanned = packages.size,
                    threatsFound = threatsFound,
                    scanType = ScanType.LOCAL
                ))
                
                // Show notification
                NotificationHelper.showScanCompleteNotification(getApplication(), threatsFound, packages.size)
                
                if (threatsFound > 0) {
                    repository.addEvent(
                        EventType.SCAN_COMPLETED,
                        "⚠️ Обнаружено $threatsFound угроз",
                        "Проверено ${packages.size} приложений"
                    )
                } else {
                    repository.addEvent(
                        EventType.SCAN_COMPLETED,
                        "✅ Сканирование завершено",
                        "Угроз не обнаружено (${packages.size} приложений)"
                    )
                }
            } catch (e: Exception) {
                repository.addEvent(
                    EventType.SCAN_COMPLETED,
                    "❌ Ошибка сканирования",
                    "${e.message}"
                )
            }
        }
    }
    
    // Scan results for UI display
    private val _scanResults = MutableStateFlow<List<com.guardian.app.ui.screens.AppScanResult>>(emptyList())
    val scanResults: StateFlow<List<com.guardian.app.ui.screens.AppScanResult>> = _scanResults.asStateFlow()
    
    fun clearScanResults() {
        _scanResults.value = emptyList()
    }
    
    // Reset stats
    fun resetStats() {
        viewModelScope.launch {
            repository.resetStats()
        }
    }
    
    // VirusTotal scan methods
    fun isVirusTotalApiKeyConfigured(): Boolean = virusTotalService.isApiKeyConfigured()
    
    fun setVirusTotalApiKey(apiKey: String) {
        virusTotalService.setApiKey(apiKey)
    }
    
    fun startVirusTotalScan() {
        if (!virusTotalService.isApiKeyConfigured()) {
            viewModelScope.launch {
                repository.addEvent(
                    EventType.SCAN_COMPLETED,
                    "⚠️ VirusTotal Not Configured",
                    "Please add your API key in settings"
                )
            }
            return
        }
        
        viewModelScope.launch {
            _isVirusTotalScanning.value = true
            _virusTotalResults.value = emptyMap()
            
            try {
                val pm = getApplication<Application>().packageManager
                val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                val packageNames = packages.map { it.packageName }
                
                var threatsFound = 0
                val resultsMap = mutableMapOf<String, VirusTotalResult>()
                var shouldStopScanning = false
                
                for (index in packageNames.indices) {
                    if (shouldStopScanning) break
                    
                    val packageName = packageNames[index]
                    _virusTotalProgress.value = Pair(index + 1, packageNames.size)
                    
                    val appName = try {
                        pm.getApplicationLabel(
                            packages.first { it.packageName == packageName }
                        ).toString()
                    } catch (e: Exception) { packageName }
                    
                    when (val result = virusTotalService.scanApp(packageName)) {
                        is ScanResult.Success -> {
                            resultsMap[packageName] = result.result
                            
                            if (result.result.isInfected) {
                                threatsFound++
                                repository.addEvent(
                                    EventType.APP_BLOCKED,
                                    "🦠 VirusTotal Threat",
                                    "$appName - ${result.result.malwareName ?: "detected by ${result.result.detectedBy} scanners"}",
                                    packageName
                                )
                            }
                        }
                        is ScanResult.Error -> {
                            // Log error but continue scanning
                        }
                        is ScanResult.NotFound -> {
                            // App not in VirusTotal database
                        }
                        is ScanResult.RateLimited -> {
                            repository.addEvent(
                                EventType.SCAN_COMPLETED,
                                "⏳ Rate Limited",
                                "VirusTotal API rate limit reached. Try again later."
                            )
                            shouldStopScanning = true
                        }
                    }
                    
                    // Rate limiting for free API (4 requests/min)
                    if (index < packageNames.size - 1 && !shouldStopScanning) {
                        kotlinx.coroutines.delay(16000)
                    }
                }
                
                _virusTotalResults.value = resultsMap
                
                // Update stats with VT threats
                repository.updateScanStatsWithThreats(resultsMap.size, threatsFound)
                
                // Save scan to history
                repository.addScanHistory(ScanHistory(
                    appsScanned = resultsMap.size,
                    threatsFound = threatsFound,
                    scanType = ScanType.VIRUS_TOTAL
                ))
                
                // Show notification
                NotificationHelper.showScanCompleteNotification(getApplication(), threatsFound, resultsMap.size)
                
                repository.addEvent(
                    EventType.SCAN_COMPLETED,
                    if (threatsFound > 0) "⚠️ VirusTotal Scan Complete" else "✅ VirusTotal Scan Complete",
                    "Scanned ${resultsMap.size} apps - $threatsFound threats found"
                )
                
            } catch (e: Exception) {
                repository.addEvent(
                    EventType.SCAN_COMPLETED,
                    "❌ VirusTotal Scan Failed",
                    "Error: ${e.message}"
                )
            } finally {
                _isVirusTotalScanning.value = false
                _virusTotalProgress.value = Pair(0, 0)
            }
        }
    }
    
    fun getVirusTotalResult(packageName: String): VirusTotalResult? {
        return _virusTotalResults.value[packageName]
    }
    
    // Scan single app with VirusTotal
    fun scanSingleAppWithVirusTotal(packageName: String, appName: String) {
        if (!virusTotalService.isApiKeyConfigured()) {
            viewModelScope.launch {
                repository.addEvent(
                    EventType.SCAN_COMPLETED,
                    "⚠️ VirusTotal Not Configured",
                    "Please add your API key in settings"
                )
            }
            return
        }
        
        viewModelScope.launch {
            _isVirusTotalScanning.value = true
            _virusTotalProgress.value = Pair(1, 1)
            
            try {
                when (val result = virusTotalService.scanApp(packageName)) {
                    is ScanResult.Success -> {
                        _virusTotalResults.value = _virusTotalResults.value + (packageName to result.result)
                        
                        if (result.result.isInfected) {
                            repository.addEvent(
                                EventType.APP_BLOCKED,
                                "🦠 VirusTotal Threat",
                                "$appName - ${result.result.malwareName ?: "detected by ${result.result.detectedBy} scanners"}",
                                packageName
                            )
                            NotificationHelper.showThreatFoundNotification(
                                getApplication(),
                                appName,
                                result.result.malwareName ?: "Detected by ${result.result.detectedBy} scanners"
                            )
                        } else {
                            repository.addEvent(
                                EventType.SCAN_COMPLETED,
                                "✅ $appName Safe",
                                "VirusTotal: ${result.result.detectedBy}/${result.result.totalScanners} scanners"
                            )
                        }
                    }
                    is ScanResult.Error -> {
                        repository.addEvent(
                            EventType.SCAN_COMPLETED,
                            "❌ Scan Error",
                            "Error scanning $appName: ${result.message}"
                        )
                    }
                    is ScanResult.NotFound -> {
                        repository.addEvent(
                            EventType.SCAN_COMPLETED,
                            "ℹ️ $appName",
                            "App not found in VirusTotal database"
                        )
                    }
                    is ScanResult.RateLimited -> {
                        repository.addEvent(
                            EventType.SCAN_COMPLETED,
                            "⏳ Rate Limited",
                            "VirusTotal API rate limit reached. Try again later."
                        )
                    }
                }
            } catch (e: Exception) {
                repository.addEvent(
                    EventType.SCAN_COMPLETED,
                    "❌ Scan Failed",
                    "Error: ${e.message}"
                )
            } finally {
                _isVirusTotalScanning.value = false
                _virusTotalProgress.value = Pair(0, 0)
            }
        }
    }
    
    // APK Scanner - scan a single APK file using URI
    fun scanApkUri(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val pm = context.packageManager
                
                // Open input stream from URI and copy to temp file
                val tempFile = java.io.File.createTempFile("scan_", ".apk", context.cacheDir)
                
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    } ?: run {
                        onResult(false, "❌ Не удалось открыть файл")
                        return@launch
                    }
                } catch (e: Exception) {
                    onResult(false, "❌ Ошибка чтения файла: ${e.message}")
                    return@launch
                }
                
                // Get package info from temp APK file
                val packageInfo = pm.getPackageArchiveInfo(tempFile.absolutePath, PackageManager.GET_PERMISSIONS)
                
                // Clean up temp file
                tempFile.delete()
                
                if (packageInfo == null) {
                    onResult(false, "❌ Не удалось прочитать APK файл")
                    return@launch
                }
                
                val packageName = packageInfo.packageName
                val appName = packageInfo.applicationInfo?.let { appInfo -> pm.getApplicationLabel(appInfo).toString() } ?: packageName
                
                // Scan via VirusTotal if API key is configured
                if (isVirusTotalApiKeyConfigured()) {
                    val result = virusTotalService.scanApp(packageName)
                    
                    when (result) {
                        is ScanResult.Success -> {
                            if (result.result.isInfected) {
                                NotificationHelper.showThreatFoundNotification(
                                    context,
                                    appName,
                                    result.result.malwareName ?: "Detected by ${result.result.detectedBy} scanners"
                                )
                                onResult(true, "⚠️ Угроза найдена: ${result.result.malwareName ?: "detected by ${result.result.detectedBy} scanners"}")
                            } else {
                                onResult(false, "✅ Безопасно (${result.result.detectedBy}/${result.result.totalScanners} сканеров)")
                            }
                        }
                        is ScanResult.NotFound -> {
                            onResult(false, "ℹ️ Приложение не найдено в базе VirusTotal")
                        }
                        is ScanResult.Error -> {
                            onResult(false, "Ошибка: ${result.message}")
                        }
                        is ScanResult.RateLimited -> {
                            onResult(false, "⏳ Лимит API. Попробуйте позже.")
                        }
                    }
                } else {
                    // Basic scan without VirusTotal
                    val dangerousPerms = packageInfo.requestedPermissions?.filter { perm: String ->
                        perm.contains("READ_SMS") || perm.contains("SEND_SMS") || 
                        perm.contains("READ_CONTACTS") || perm.contains("CAMERA") ||
                        perm.contains("RECORD_AUDIO") || perm.contains("ACCESS_FINE_LOCATION")
                    } ?: emptyList()
                    
                    if (dangerousPerms.isNotEmpty()) {
                        NotificationHelper.showThreatFoundNotification(context, appName, "Опасные разрешения: ${dangerousPerms.size}")
                        onResult(true, "⚠️ Опасные разрешения: ${dangerousPerms.joinToString { perm: String -> perm.substringAfterLast(".") }}")
                    } else {
                        onResult(false, "✅ Безопасно (нет опасных разрешений)")
                    }
                }
                
                // Save to history
                repository.addScanHistory(ScanHistory(
                    appsScanned = 1,
                    threatsFound = 0,
                    scanType = ScanType.LOCAL
                ))
                
            } catch (e: Exception) {
                onResult(false, "Ошибка: ${e.message}")
            }
        }
    }
}
