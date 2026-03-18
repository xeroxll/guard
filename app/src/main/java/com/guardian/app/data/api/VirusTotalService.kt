package com.guardian.app.data.api

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.util.Log
import com.guardian.app.data.model.VirusTotalResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class VirusTotalService(private val context: Context) {
    
    companion object {
        private const val TAG = "VirusTotalService"
        private const val BASE_URL = "https://www.virustotal.com/"
        private const val PREFS_NAME = "virustotal_prefs"
        private const val KEY_API_KEY = "api_key"
    }
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // Get API key from SharedPreferences or use default
    private fun getApiKey(): String {
        return prefs.getString(KEY_API_KEY, "") ?: ""
    }
    
    // Save API key to SharedPreferences
    fun setApiKey(apiKey: String) {
        prefs.edit().putString(KEY_API_KEY, apiKey.trim()).apply()
    }
    
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }
    
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    private val api: VirusTotalApi by lazy {
        retrofit.create(VirusTotalApi::class.java)
    }
    
    /**
     * Validate API key by making a test request
     */
    suspend fun validateApiKey(): ScanResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext ScanResult.Error("API key not set. Please configure your VirusTotal API key in Settings.")
        }
        
        try {
            // Try to get info about a known file hash
            // Using a test hash that should exist in VirusTotal database
            val testHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855" // empty file SHA256
            val response = api.getFileReport(apiKey, testHash)
            
            if (response.error != null) {
                return@withContext when {
                    response.error.contains("quota", ignoreCase = true) -> ScanResult.RateLimited
                    response.error.contains("Unauthorized", ignoreCase = true) -> 
                        ScanResult.Error("Invalid API key. Please check your key and try again.")
                    else -> ScanResult.Error(response.error)
                }
            }
            
            ScanResult.Success(
                VirusTotalResult(
                    isInfected = false,
                    detectedBy = 0,
                    totalScanners = 0,
                    malwareName = null,
                    scanDate = null,
                    permalink = null
                )
            )
        } catch (e: retrofit2.HttpException) {
            when (e.code()) {
                401 -> ScanResult.Error("Invalid API key. Please check your key.")
                429 -> ScanResult.RateLimited
                else -> ScanResult.Error("HTTP Error: ${e.code()}")
            }
        } catch (e: Exception) {
            ScanResult.Error(e.message ?: "Unknown error validating API key")
        }
    }
    
    /**
     * Scans an installed app using VirusTotal API
     */
    suspend fun scanApp(packageName: String): ScanResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        
        if (apiKey.isBlank()) {
            return@withContext ScanResult.Error("API key not configured. Please add it in Settings.")
        }
        
        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val apkPath = appInfo.sourceDir
            
            val sha256Hash = calculateFileHash(apkPath)
            if (sha256Hash == null) {
                return@withContext ScanResult.Error("Could not calculate file hash")
            }
            
            Log.d(TAG, "Checking hash: $sha256Hash for package: $packageName")
            
            val response = api.getFileReport(apiKey, sha256Hash)
            
            if (response.error != null) {
                Log.e(TAG, "API Error: ${response.error}")
                return@withContext when {
                    response.error.contains("quota", ignoreCase = true) -> ScanResult.RateLimited
                    response.error.contains("not found", ignoreCase = true) -> ScanResult.NotFound
                    response.error.contains("Unauthorized", ignoreCase = true) -> 
                        ScanResult.Error("Invalid API key")
                    else -> ScanResult.Error(response.error)
                }
            }
            
            val data = response.data
            if (data == null) {
                return@withContext ScanResult.NotFound
            }
            
            val attributes = data.attributes
            val stats = attributes?.lastAnalysisStats
            
            if (stats == null) {
                return@withContext ScanResult.NotFound
            }
            
            val detectedCount = (stats.malicious ?: 0) + (stats.suspicious ?: 0)
            val totalScanners = listOfNotNull(
                stats.malicious, stats.suspicious, stats.undetected, 
                stats.harmless, stats.timeout, stats.typeUnsupported
            ).sum()
            
            val malwareName = if (detectedCount > 0) {
                attributes.lastAnalysisResults?.values
                    ?.filter { it.category == "malicious" || it.category == "suspicious" }
                    ?.firstNotNullOfOrNull { it.result } ?: "Detected by $detectedCount scanners"
            } else null
            
            val result = VirusTotalResult(
                isInfected = detectedCount > 0,
                detectedBy = detectedCount,
                totalScanners = totalScanners,
                malwareName = malwareName,
                scanDate = attributes.lastAnalysisDate?.let {
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date(it * 1000))
                },
                permalink = "https://www.virustotal.com/gui/file/$sha256Hash"
            )
            
            Log.d(TAG, "Scan result for $packageName: infected=${result.isInfected}, detected=${result.detectedBy}/${result.totalScanners}")
            
            ScanResult.Success(result)
            
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP Error: ${e.code()} - ${e.message()}")
            when (e.code()) {
                429 -> ScanResult.RateLimited
                401 -> ScanResult.Error("Invalid API key")
                404 -> ScanResult.NotFound
                else -> ScanResult.Error("HTTP Error: ${e.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning app: ${e.message}", e)
            ScanResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Get list of installed apps with their package info
     */
    fun getInstalledApps(): List<Pair<String, String>> {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        
        return packages.map { appInfo ->
            val appName = pm.getApplicationLabel(appInfo).toString()
            appName to appInfo.packageName
        }.sortedBy { it.first }
    }
    
    /**
     * Calculate SHA-256 hash of a file
     */
    private fun calculateFileHash(filePath: String): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val file = java.io.File(filePath)
            val inputStream = java.io.FileInputStream(file)
            
            val buffer = ByteArray(8192)
            var read: Int
            
            while (inputStream.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }
            
            inputStream.close()
            
            val hashBytes = digest.digest()
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating hash: ${e.message}", e)
            null
        }
    }
    
    /**
     * Check if API key is configured
     */
    fun isApiKeyConfigured(): Boolean = getApiKey().isNotBlank()
}
