package com.guardian.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.guardian.app.data.model.AppStats
import com.guardian.app.data.model.BlacklistedApp
import com.guardian.app.data.model.EventType
import com.guardian.app.data.model.ScanHistory
import com.guardian.app.data.model.ScanType
import com.guardian.app.data.model.SecurityEvent
import com.guardian.app.data.model.TrustedApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "guardian_prefs")

class GuardianRepository(private val context: Context) {
    
    companion object {
        // Main protection toggle
        private val PROTECTION_ENABLED = booleanPreferencesKey("protection_enabled")
        
        // Theme toggle
        private val DARK_THEME = booleanPreferencesKey("dark_theme")
        
        // Individual module toggles - SMS and Call filters removed
        private val USB_MONITOR_ENABLED = booleanPreferencesKey("usb_monitor_enabled")
        private val APP_MONITOR_ENABLED = booleanPreferencesKey("app_monitor_enabled")
        
        private val USB_DEBUG_ENABLED = booleanPreferencesKey("usb_debug_enabled")
        private val BLACKLIST = stringPreferencesKey("blacklist")
        private val TRUSTED_APPS = stringPreferencesKey("trusted_apps")
        private val EVENTS = stringPreferencesKey("events")
        private val STATS = stringPreferencesKey("stats")
        private val SCAN_HISTORY = stringPreferencesKey("scan_history")
    }
    
    // Theme State
    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { it[DARK_THEME] ?: true }
    
    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[DARK_THEME] = enabled }
    }
    
    // Main Protection State
    val isProtectionEnabled: Flow<Boolean> = context.dataStore.data.map { it[PROTECTION_ENABLED] ?: true }
    
    suspend fun setProtectionEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PROTECTION_ENABLED] = enabled }
    }
    
    // Individual Module States - SMS and Call filters removed for Google Play
    val isUsbMonitorEnabled: Flow<Boolean> = context.dataStore.data.map { it[USB_MONITOR_ENABLED] ?: true }
    val isAppMonitorEnabled: Flow<Boolean> = context.dataStore.data.map { it[APP_MONITOR_ENABLED] ?: true }
    
    suspend fun setUsbMonitorEnabled(enabled: Boolean) {
        context.dataStore.edit { it[USB_MONITOR_ENABLED] = enabled }
    }
    
    // SMS and Call filter setters removed for Google Play compliance
    
    suspend fun setAppMonitorEnabled(enabled: Boolean) {
        context.dataStore.edit { it[APP_MONITOR_ENABLED] = enabled }
    }
    
    // USB Debug State
    val isUsbDebugEnabled: Flow<Boolean> = context.dataStore.data.map { it[USB_DEBUG_ENABLED] ?: false }
    
    suspend fun setUsbDebugEnabled(enabled: Boolean) {
        context.dataStore.edit { it[USB_DEBUG_ENABLED] = enabled }
    }
    
    // Blacklist
    val blacklist: Flow<List<BlacklistedApp>> = context.dataStore.data.map { prefs ->
        parseBlacklist(prefs[BLACKLIST] ?: "[]")
    }
    
    suspend fun addToBlacklist(app: BlacklistedApp) {
        context.dataStore.edit { prefs ->
            val current = parseBlacklist(prefs[BLACKLIST] ?: "[]").toMutableList()
            if (current.none { it.packageName == app.packageName }) {
                current.add(app)
                prefs[BLACKLIST] = blacklistToJson(current)
            }
        }
    }
    
    suspend fun removeFromBlacklist(id: String) {
        context.dataStore.edit { prefs ->
            val current = parseBlacklist(prefs[BLACKLIST] ?: "[]").toMutableList()
            current.removeAll { it.id == id }
            prefs[BLACKLIST] = blacklistToJson(current)
        }
    }
    
    suspend fun isPackageBlacklisted(packageName: String): Boolean {
        var result = false
        context.dataStore.edit { prefs ->
            result = parseBlacklist(prefs[BLACKLIST] ?: "[]").any { it.packageName == packageName }
        }
        return result
    }
    
    // Trusted Apps (Whitelist)
    val trustedApps: Flow<List<TrustedApp>> = context.dataStore.data.map { prefs ->
        parseTrustedApps(prefs[TRUSTED_APPS] ?: "[]")
    }
    
    suspend fun addToTrustedApps(app: TrustedApp) {
        context.dataStore.edit { prefs ->
            val current = parseTrustedApps(prefs[TRUSTED_APPS] ?: "[]").toMutableList()
            if (current.none { it.packageName == app.packageName }) {
                current.add(app)
                prefs[TRUSTED_APPS] = trustedAppsToJson(current)
            }
        }
    }
    
    suspend fun removeFromTrustedApps(id: String) {
        context.dataStore.edit { prefs ->
            val current = parseTrustedApps(prefs[TRUSTED_APPS] ?: "[]").toMutableList()
            current.removeAll { it.id == id }
            prefs[TRUSTED_APPS] = trustedAppsToJson(current)
        }
    }
    
    suspend fun isPackageTrusted(packageName: String): Boolean {
        var result = false
        context.dataStore.edit { prefs ->
            result = parseTrustedApps(prefs[TRUSTED_APPS] ?: "[]").any { it.packageName == packageName }
        }
        return result
    }
    
    // Events
    val events: Flow<List<SecurityEvent>> = context.dataStore.data.map { prefs ->
        parseEvents(prefs[EVENTS] ?: "[]")
    }
    
    suspend fun addEvent(type: EventType, title: String, description: String, packageName: String? = null) {
        context.dataStore.edit { prefs ->
            val current = parseEvents(prefs[EVENTS] ?: "[]").toMutableList()
            val event = SecurityEvent(type = type, title = title, description = description, packageName = packageName)
            current.add(0, event)
            val trimmed = current.take(100)
            prefs[EVENTS] = eventsToJson(trimmed)
        }
    }
    
    // Stats
    val stats: Flow<AppStats> = context.dataStore.data.map { prefs ->
        parseStats(prefs[STATS] ?: "{}")
    }
    
    suspend fun incrementBlockedCount() {
        context.dataStore.edit { prefs ->
            val current = parseStats(prefs[STATS] ?: "{}")
            prefs[STATS] = """{"threatsBlocked":${current.threatsBlocked + 1},"appsScanned":${current.appsScanned},"lastScanTime":${current.lastScanTime}}"""
        }
    }
    
    suspend fun updateScanStats(appsScanned: Int) {
        context.dataStore.edit { prefs ->
            val current = parseStats(prefs[STATS] ?: "{}")
            prefs[STATS] = """{"threatsBlocked":${current.threatsBlocked},"appsScanned":$appsScanned,"lastScanTime":${System.currentTimeMillis()}}"""
        }
    }
    
    suspend fun updateScanStatsWithThreats(appsScanned: Int, threatsFound: Int) {
        context.dataStore.edit { prefs ->
            prefs[STATS] = """{"threatsBlocked":$threatsFound,"appsScanned":$appsScanned,"lastScanTime":${System.currentTimeMillis()}}"""
        }
    }
    
    suspend fun resetBlockedCount() {
        context.dataStore.edit { prefs ->
            val current = parseStats(prefs[STATS] ?: "{}")
            prefs[STATS] = """{"threatsBlocked":0,"appsScanned":${current.appsScanned},"lastScanTime":${current.lastScanTime}}"""
        }
    }
    
    suspend fun resetStats() {
        context.dataStore.edit { prefs ->
            prefs[STATS] = """{"threatsBlocked":0,"appsScanned":0,"lastScanTime":0}"""
        }
    }
    
    // Scan History
    val scanHistory: Flow<List<ScanHistory>> = context.dataStore.data.map { prefs ->
        parseScanHistory(prefs[SCAN_HISTORY] ?: "[]")
    }
    
    suspend fun addScanHistory(scanHistory: ScanHistory) {
        context.dataStore.edit { prefs ->
            val current = parseScanHistory(prefs[SCAN_HISTORY] ?: "[]").toMutableList()
            current.add(0, scanHistory)
            val trimmed = current.take(50) // Keep last 50 scans
            prefs[SCAN_HISTORY] = scanHistoryToJson(trimmed)
        }
    }
    
    private fun parseScanHistory(json: String): List<ScanHistory> {
        return try {
            if (json == "[]" || json.isEmpty()) return emptyList()
            val list = mutableListOf<ScanHistory>()
            val items = json.removePrefix("[").removeSuffix("]").split("},{")
            items.forEach { item ->
                val clean = item.removePrefix("{").removeSuffix("}")
                val id = extractValue(clean, "id")
                val timestamp = extractValue(clean, "timestamp").toLongOrNull() ?: 0L
                val appsScanned = extractValue(clean, "appsScanned").toIntOrNull() ?: 0
                val threatsFound = extractValue(clean, "threatsFound").toIntOrNull() ?: 0
                val scanTypeStr = extractValue(clean, "scanType")
                val scanType = try { ScanType.valueOf(scanTypeStr) } catch (e: Exception) { ScanType.LOCAL }
                if (id.isNotEmpty()) {
                    list.add(ScanHistory(id, timestamp, appsScanned, threatsFound, scanType))
                }
            }
            list
        } catch (e: Exception) { emptyList() }
    }
    
    private fun scanHistoryToJson(list: List<ScanHistory>): String {
        if (list.isEmpty()) return "[]"
        return "[" + list.joinToString(",") { 
            """{"id":"${it.id}","timestamp":${it.timestamp},"appsScanned":${it.appsScanned},"threatsFound":${it.threatsFound},"scanType":"${it.scanType.name}"}""" 
        } + "]"
    }
    
    private fun parseTrustedApps(json: String): List<TrustedApp> {
        return try {
            if (json == "[]" || json.isEmpty()) return emptyList()
            val list = mutableListOf<TrustedApp>()
            val items = json.removePrefix("[").removeSuffix("]").split("},{")
            items.forEach { item ->
                val clean = item.removePrefix("{").removeSuffix("}")
                val id = extractValue(clean, "id")
                val name = extractValue(clean, "name")
                val packageName = extractValue(clean, "packageName")
                val addedAt = extractValue(clean, "addedAt").toLongOrNull() ?: 0L
                if (id.isNotEmpty() && name.isNotEmpty()) {
                    list.add(TrustedApp(id, name, packageName, addedAt))
                }
            }
            list
        } catch (e: Exception) { emptyList() }
    }
    
    private fun trustedAppsToJson(list: List<TrustedApp>): String {
        if (list.isEmpty()) return "[]"
        return "[" + list.joinToString(",") { 
            """{"id":"${it.id}","name":"${it.name}","packageName":"${it.packageName}","addedAt":${it.addedAt}}""" 
        } + "]"
    }
    
    // JSON Helpers
    private fun parseBlacklist(json: String): List<BlacklistedApp> {
        return try {
            if (json == "[]" || json.isEmpty()) return emptyList()
            val list = mutableListOf<BlacklistedApp>()
            val items = json.removePrefix("[").removeSuffix("]").split("},{")
            items.forEach { item ->
                val clean = item.removePrefix("{").removeSuffix("}")
                val id = extractValue(clean, "id")
                val name = extractValue(clean, "name")
                val packageName = extractValue(clean, "packageName")
                val addedAt = extractValue(clean, "addedAt").toLongOrNull() ?: 0L
                if (id.isNotEmpty() && name.isNotEmpty()) {
                    list.add(BlacklistedApp(id, name, packageName, addedAt))
                }
            }
            list
        } catch (e: Exception) { emptyList() }
    }
    
    private fun blacklistToJson(list: List<BlacklistedApp>): String {
        if (list.isEmpty()) return "[]"
        return "[" + list.joinToString(",") { 
            """{"id":"${it.id}","name":"${it.name}","packageName":"${it.packageName}","addedAt":${it.addedAt}}""" 
        } + "]"
    }
    
    private fun parseEvents(json: String): List<SecurityEvent> {
        return try {
            if (json == "[]" || json.isEmpty()) return emptyList()
            val list = mutableListOf<SecurityEvent>()
            val items = json.removePrefix("[").removeSuffix("]").split("},{")
            items.forEach { item ->
                val clean = item.removePrefix("{").removeSuffix("}")
                val id = extractValue(clean, "id")
                val typeStr = extractValue(clean, "type")
                val title = extractValue(clean, "title")
                val description = extractValue(clean, "description")
                val packageName = extractValue(clean, "packageName").takeIf { it.isNotEmpty() }
                val timestamp = extractValue(clean, "timestamp").toLongOrNull() ?: 0L
                if (id.isNotEmpty() && title.isNotEmpty()) {
                    val type = try { EventType.valueOf(typeStr) } catch (e: Exception) { EventType.SCAN_COMPLETED }
                    list.add(SecurityEvent(id, type, title, description, packageName, timestamp))
                }
            }
            list
        } catch (e: Exception) { emptyList() }
    }
    
    private fun eventsToJson(list: List<SecurityEvent>): String {
        if (list.isEmpty()) return "[]"
        return "[" + list.joinToString(",") { 
            """{"id":"${it.id}","type":"${it.type.name}","title":"${it.title}","description":"${it.description}","packageName":"${it.packageName ?: ""}","timestamp":${it.timestamp}}""" 
        } + "]"
    }
    
    private fun parseStats(json: String): AppStats {
        return try {
            AppStats(
                threatsBlocked = extractValue(json, "threatsBlocked").toIntOrNull() ?: 0,
                appsScanned = extractValue(json, "appsScanned").toIntOrNull() ?: 0,
                lastScanTime = extractValue(json, "lastScanTime").toLongOrNull() ?: 0L
            )
        } catch (e: Exception) { AppStats() }
    }
    
    private fun extractValue(json: String, key: String): String {
        val patterns = listOf("\"$key\":\"", "\"$key\":", "$key\":\"", "$key\":")
        for (pattern in patterns) {
            val start = json.indexOf(pattern)
            if (start != -1) {
                val valueStart = start + pattern.length
                var valueEnd = json.indexOf(",", valueStart)
                if (valueEnd == -1) valueEnd = json.indexOf("}", valueStart)
                if (valueEnd == -1) valueEnd = json.length
                return json.substring(valueStart, valueEnd).trim('"', ' ', '}')
            }
        }
        return ""
    }
}
