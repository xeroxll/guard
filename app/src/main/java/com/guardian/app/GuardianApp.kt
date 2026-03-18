package com.guardian.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class GuardianApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ALERTS,
                    "Security Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Important security notifications"
                },
                NotificationChannel(
                    CHANNEL_BLOCKED,
                    "Blocked Apps",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications about blocked applications"
                }
            )
            
            val manager = getSystemService(NotificationManager::class.java)
            channels.forEach { manager.createNotificationChannel(it) }
        }
    }
    
    companion object {
        lateinit var instance: GuardianApp
            private set
        
        const val CHANNEL_ALERTS = "alerts"
        const val CHANNEL_BLOCKED = "blocked"
    }
}
