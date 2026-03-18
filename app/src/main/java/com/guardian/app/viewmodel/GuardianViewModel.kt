package com.guardian.app.viewmodel

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
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
    
    // Data
    val blacklist = repository.blacklist.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
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
        }
    }
    
    fun removeFromBlacklist(id: String) {
        viewModelScope.launch {
            repository.removeFromBlacklist(id)
        }
    }
    
    // Uninstall app - returns intent to start uninstall dialog
    fun getUninstallIntent(packageName: String): android.content.Intent {
        return android.content.Intent(android.content.Intent.ACTION_DELETE).apply {
            data = android.net.Uri.parse("package:$packageName")
        }
    }
    
    // Scan with real threat detection
    fun startScan() {
        viewModelScope.launch {
            try {
                val pm = getApplication<Application>().packageManager
                val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                val blacklisted = blacklist.value.map { it.packageName }.toSet()
                
                var threatsFound = 0
                val detectedThreats = mutableListOf<Pair<String, String>>()
                
                // Known dangerous packages (real malware patterns) - more specific
                val dangerousPatterns = listOf(
                    // Actual malware patterns
                    "trojan", "malware", "virus", "backdoor", "spyware",
                    "keylog", "ransomware", "botnet", "worm", "rootkit",
                    "stealer", "miner", "dropper", "injector", "hook",
                    // Suspicious fake apps
                    "free.vip", "free.premium", "free.pro", "hack.tool",
                    "crack.tool", "bypass.premium", "cheat.game",
                    // Known malware packages
                    "com.android.vending.billing", // Fake billing
                    "com.svidersk", "com.zmzpass", "com.crypt",
                    "com.hack", "com.keylog", "com.spy",
                    ".hacker", ".cracker", ".bypass"
                )
                
                // Check each app
                for (packageInfo in packages) {
                    val packageName = packageInfo.packageName.lowercase()
                    val appName = pm.getApplicationLabel(packageInfo).toString()
                    
                    // Check 1: Blacklist match
                    if (blacklisted.contains(packageInfo.packageName)) {
                        threatsFound++
                        detectedThreats.add(appName to "Blacklisted app")
                        repository.addEvent(
                            EventType.APP_BLOCKED,
                            "⚠️ Blacklisted App",
                            "$appName - in your blocklist",
                            packageInfo.packageName
                        )
                        continue
                    }
                    
                    // Check 2: Dangerous package name patterns
                    val isDangerousPattern = dangerousPatterns.any { pattern ->
                        packageName.contains(pattern.lowercase())
                    }
                    
                    // Check 3: Suspicious permissions (dangerous SMS/Call/Camera perms)
                    val hasSuspiciousPerms = try {
                        val packageInfoFull = pm.getPackageInfo(packageInfo.packageName, PackageManager.GET_PERMISSIONS)
                        val requestedPermissions = packageInfoFull.requestedPermissions?.toList() ?: emptyList()
                        val dangerousPerms = listOf(
                            "android.permission.READ_SMS",
                            "android.permission.RECEIVE_SMS", 
                            "android.permission.SEND_SMS",
                            "android.permission.READ_CALL_LOG",
                            "android.permission.READ_CONTACTS",
                            "android.permission.CAMERA",
                            "android.permission.RECORD_AUDIO",
                            "android.permission.ACCESS_FINE_LOCATION",
                            "android.permission.READ_PHONE_STATE",
                            "android.permission.PROCESS_OUTGOING_CALLS"
                        )
                        // Only flag if 3+ dangerous perms
                        requestedPermissions.count { dangerousPerms.contains(it) } >= 3
                    } catch (e: Exception) { false }
                    
                    // Check 4: System apps trying to do unusual things
                    val isSystemApp = (packageInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val isUpdatedSystemApp = (packageInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                    
                    // Flag as threat if: blacklist match OR (dangerous name pattern AND not system) OR (very dangerous perms)
                    // Only flag permissions for non-system apps to avoid false positives
                    val hasVeryDangerousPerms = try {
                        val packageInfoFull = pm.getPackageInfo(packageInfo.packageName, PackageManager.GET_PERMISSIONS)
                        val requestedPermissions = packageInfoFull.requestedPermissions?.toList() ?: emptyList()
                        val veryDangerousPerms = listOf(
                            "android.permission.READ_SMS",
                            "android.permission.SEND_SMS",
                            "android.permission.RECEIVE_SMS",
                            "android.permission.READ_CALL_LOG",
                            "android.permission.PROCESS_OUTGOING_CALLS"
                        )
                        // Only flag if has 2+ VERY dangerous perms
                        requestedPermissions.count { veryDangerousPerms.contains(it) } >= 2
                    } catch (e: Exception) { false }
                    
                    if (isDangerousPattern || hasVeryDangerousPerms) {
                        threatsFound++
                        val reason = when {
                            isDangerousPattern -> "Подозрительное имя пакета"
                            hasVeryDangerousPerms -> "Опасные разрешения"
                            else -> "Потенциальная угроза"
                        }
                        detectedThreats.add(appName to reason)
                        repository.addEvent(
                            EventType.APP_BLOCKED,
                            "⚠️ Подозрительное приложение",
                            "$appName - $reason",
                            packageInfo.packageName
                        )
                    }
                    
                    // Small delay to simulate real scanning (longer for realism)
                    kotlinx.coroutines.delay(2)
                }
                
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
                        "⚠️ Scan Complete",
                        "Found $threatsFound potential threats in ${packages.size} apps"
                    )
                } else {
                    repository.addEvent(
                        EventType.SCAN_COMPLETED,
                        "✅ Scan Complete",
                        "${packages.size} apps scanned - all safe"
                    )
                }
            } catch (e: Exception) {
                repository.addEvent(
                    EventType.SCAN_COMPLETED,
                    "❌ Scan Failed",
                    "Error: ${e.message}"
                )
            }
        }
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
    
    // APK Scanner - scan a single APK file
    fun scanApk(apkPath: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val pm = context.packageManager
                
                // Get package info from APK
                val packageInfo = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_PERMISSIONS)
                
                if (packageInfo == null) {
                    onResult(false, "Не удалось прочитать APK файл")
                    return@launch
                }
                
                val packageName = packageInfo.packageName
                val appName = packageInfo.applicationInfo?.let { pm.getApplicationLabel(it).toString() } ?: packageName
                
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
                    val dangerousPerms = packageInfo.requestedPermissions?.filter { perm ->
                        perm.contains("READ_SMS") || perm.contains("SEND_SMS") || 
                        perm.contains("READ_CONTACTS") || perm.contains("CAMERA") ||
                        perm.contains("RECORD_AUDIO") || perm.contains("ACCESS_FINE_LOCATION")
                    } ?: emptyList()
                    
                    if (dangerousPerms.isNotEmpty()) {
                        NotificationHelper.showThreatFoundNotification(context, appName, "Опасные разрешения: ${dangerousPerms.size}")
                        onResult(true, "⚠️ Опасные разрешения: ${dangerousPerms.joinToString { it.substringAfterLast(".") }}")
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
