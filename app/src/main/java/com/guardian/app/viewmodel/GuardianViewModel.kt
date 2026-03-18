package com.guardian.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guardian.app.data.model.AppStats
import com.guardian.app.data.model.BlacklistedApp
import com.guardian.app.data.model.SecurityEvent
import com.guardian.app.data.repository.GuardianRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GuardianViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = GuardianRepository(application)
    
    // UI State
    val isProtectionEnabled = repository.isProtectionEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val isUsbDebugEnabled = repository.isUsbDebugEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val blacklist = repository.blacklist.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val events = repository.events.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val stats = repository.stats.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppStats())
    
    // Actions
    fun toggleProtection() {
        viewModelScope.launch {
            val newValue = !isProtectionEnabled.value
            repository.setProtectionEnabled(newValue)
            
            if (newValue) {
                repository.addEvent(
                    com.guardian.app.data.model.EventType.PROTECTION_ENABLED,
                    "🛡️ Protection Enabled",
                    "Active monitoring started"
                )
            } else {
                repository.addEvent(
                    com.guardian.app.data.model.EventType.PROTECTION_DISABLED,
                    "⚠️ Protection Disabled",
                    "Device is vulnerable"
                )
            }
        }
    }
    
    fun toggleUsbDebug() {
        viewModelScope.launch {
            val newValue = !isUsbDebugEnabled.value
            repository.setUsbDebugEnabled(newValue)
            
            if (newValue) {
                repository.addEvent(
                    com.guardian.app.data.model.EventType.USB_ENABLED,
                    "🔌 USB Debug Enabled",
                    "Security risk detected"
                )
            } else {
                repository.addEvent(
                    com.guardian.app.data.model.EventType.USB_DISABLED,
                    "✅ USB Debug Disabled",
                    "Threat removed"
                )
            }
        }
    }
    
    fun addToBlacklist(name: String, packageName: String) {
        viewModelScope.launch {
            repository.addToBlacklist(BlacklistedApp(name = name, packageName = packageName))
            repository.addEvent(
                com.guardian.app.data.model.EventType.APP_BLOCKED,
                "🚫 App Blocked",
                "$name has been blocked",
                packageName
            )
            repository.incrementBlockedCount()
        }
    }
    
    fun removeFromBlacklist(id: String) {
        viewModelScope.launch {
            repository.removeFromBlacklist(id)
        }
    }
    
    suspend fun isPackageBlocked(packageName: String): Boolean {
        return repository.isPackageBlacklisted(packageName)
    }
    
    fun addEvent(type: com.guardian.app.data.model.EventType, title: String, description: String, packageName: String? = null) {
        viewModelScope.launch {
            repository.addEvent(type, title, description, packageName)
        }
    }
    
    fun resetStats() {
        viewModelScope.launch {
            repository.resetStats()
        }
    }
}
