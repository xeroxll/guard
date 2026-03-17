package com.guardian.app

import android.provider.Settings
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise

class SecurityModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String {
        return "SecurityModule"
    }

    @ReactMethod
    fun isDeveloperOptionsEnabled(promise: Promise) {
        try {
            val devOptions = Settings.Global.getInt(
                reactApplicationContext.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0
            ) != 0
            promise.resolve(devOptions)
        } catch (e: Exception) {
            promise.reject("ERR_SECURITY", e.message)
        }
    }

    @ReactMethod
    fun isUsbDebuggingEnabled(promise: Promise) {
        try {
            val usbDebugging = Settings.Global.getInt(
                reactApplicationContext.contentResolver,
                Settings.Global.ADB_ENABLED, 0
            ) != 0
            promise.resolve(usbDebugging)
        } catch (e: Exception) {
            promise.reject("ERR_SECURITY", e.message)
        }
    }
}
