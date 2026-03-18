package com.guardian.app.ui.screens

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.guardian.app.data.api.VirusTotalResult
import com.guardian.app.ui.theme.*
import com.guardian.app.viewmodel.GuardianViewModel
import kotlinx.coroutines.launch

// Data class for app scan result
data class AppScanResult(
    val packageName: String,
    val appName: String,
    val isThreat: Boolean,
    val threatType: String,
    val isSystemApp: Boolean,
    val virusTotalResult: VirusTotalResult? = null
)

@Composable
fun ScanScreen(viewModel: GuardianViewModel) {
    val context = LocalContext.current
    val stats by viewModel.stats.collectAsState()
    val blacklist by viewModel.blacklist.collectAsState()
    val trustedApps by viewModel.trustedApps.collectAsState()
    val isVirusTotalScanning by viewModel.isVirusTotalScanning.collectAsState()
    val virusTotalProgress by viewModel.virusTotalProgress.collectAsState()
    val virusTotalResults by viewModel.virusTotalResults.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val scanResultsFromViewModel by viewModel.scanResults.collectAsState()
    
    // Use theme-aware colors
    val backgroundColor = if (isDarkTheme) GuardianBackground else GuardianBackgroundLight
    val surfaceColor = if (isDarkTheme) GuardianSurface else GuardianSurfaceLight
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1E293B)
    val grayText = if (isDarkTheme) Color.Gray else Color(0xFF64748B)
    val cardBackground = if (isDarkTheme) GuardianSurface else GuardianSurfaceLight
    
    var localScanResults by remember { mutableStateOf<List<AppScanResult>>(emptyList()) }
    val scanResults = if (scanResultsFromViewModel.isNotEmpty()) scanResultsFromViewModel else localScanResults
    
    var showVirusTotalDialog by remember { mutableStateOf(false) }
    var showVirusTotalAppPicker by remember { mutableStateOf(false) }
    var showApkDialog by remember { mutableStateOf(false) }
    var apkResult by remember { mutableStateOf<String?>(null) }
    var showAllApps by remember { mutableStateOf(false) }
    var isLocalScanning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
        Text(
            text = "Сканер",
            style = MaterialTheme.typography.headlineLarge,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Проверка установленных приложений на вирусы",
            style = MaterialTheme.typography.bodyMedium,
            color = grayText
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Scan Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Quick Scan Button - Now with real threat detection
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clickable(enabled = !isVirusTotalScanning && !isLocalScanning) {
                        isLocalScanning = true
                        viewModel.startScan()
                        isLocalScanning = false
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(GuardianPrimary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isLocalScanning) Icons.Default.Sync else Icons.Default.Security,
                            contentDescription = "Быстрое сканирование",
                            modifier = Modifier
                                .size(28.dp)
                                .then(if (isLocalScanning) Modifier.rotate(rotation) else Modifier),
                            tint = GuardianPrimary
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Быстрое сканирование",
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Проверка сигнатур и разрешений",
                            style = MaterialTheme.typography.bodySmall,
                            color = grayText
                        )
                    }
                }
            }
            
            // Full VirusTotal Scan Button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clickable(enabled = !isVirusTotalScanning && !isLocalScanning) {
                        showVirusTotalDialog = true
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(GuardianBlue.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isVirusTotalScanning) Icons.Default.Sync else Icons.Default.BugReport,
                            contentDescription = "VirusTotal",
                            modifier = Modifier
                                .size(28.dp)
                                .then(if (isVirusTotalScanning) Modifier.rotate(rotation) else Modifier),
                            tint = GuardianBlue
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "VirusTotal сканирование",
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Облачная проверка всех приложений",
                            style = MaterialTheme.typography.bodySmall,
                            color = grayText
                        )
                    }
                }
            }
            
            // Scan Specific App Button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clickable(enabled = !isVirusTotalScanning && !isLocalScanning) {
                        showVirusTotalAppPicker = true
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(GuardianGreen.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Проверить приложение",
                            modifier = Modifier.size(28.dp),
                            tint = GuardianGreen
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Проверить приложение",
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Выбрать конкретное приложение",
                            style = MaterialTheme.typography.bodySmall,
                            color = grayText
                        )
                    }
                }
            }
            
            // APK Scan Button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clickable(enabled = !isVirusTotalScanning && !isLocalScanning) {
                        showApkDialog = true
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(GuardianYellow.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Android,
                            contentDescription = "Сканировать APK",
                            modifier = Modifier.size(28.dp),
                            tint = GuardianYellow
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Сканировать APK файл",
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Проверить загруженный APK файл",
                            style = MaterialTheme.typography.bodySmall,
                            color = grayText
                        )
                    }
                }
            }
        }
        
        // Scan Status
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when {
                isLocalScanning -> "Быстрое сканирование..."
                isVirusTotalScanning -> "VirusTotal: ${virusTotalProgress.first}/${virusTotalProgress.second}"
                scanResults.isNotEmpty() && scanResults.any { it.isThreat } -> "⚠️ Обнаружены угрозы!"
                scanResults.isNotEmpty() -> "✅ Угроз не обнаружено"
                else -> "Выберите тип сканирования"
            },
            style = MaterialTheme.typography.titleMedium,
            color = when {
                isLocalScanning -> GuardianPrimary
                isVirusTotalScanning -> GuardianYellow
                scanResults.isNotEmpty() && scanResults.any { it.isThreat } -> GuardianRed
                scanResults.isNotEmpty() -> GuardianGreen
                else -> grayText
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Stats Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CheckCircle,
                iconTint = if (isDarkTheme) GuardianGreen else GuardianGreenLight,
                title = "Проверено",
                value = stats.appsScanned.toString(),
                isDarkTheme = isDarkTheme
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Block,
                iconTint = if (isDarkTheme) GuardianRed else GuardianRedLight,
                title = "Угрозы",
                value = stats.threatsBlocked.toString(),
                isDarkTheme = isDarkTheme
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Star,
                iconTint = if (isDarkTheme) GuardianYellow else GuardianYellowLight,
                title = "Доверенные",
                value = trustedApps.size.toString(),
                isDarkTheme = isDarkTheme
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Toggle for showing all apps or only threats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showAllApps) "Все приложения" else "Обнаруженные угрозы",
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
            
            TextButton(onClick = { showAllApps = !showAllApps }) {
                Text(
                    text = if (showAllApps) "Только угрозы" else "Показать все",
                    color = GuardianBlue
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Apps List
        val displayedApps = if (showAllApps) {
            scanResults
        } else {
            scanResults.filter { it.isThreat }
        }
        
        if (displayedApps.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                displayedApps.forEach { app ->
                    AppListItem(
                        app = app,
                        isBlacklisted = blacklist.any { it.packageName == app.packageName },
                        isTrusted = trustedApps.any { it.packageName == app.packageName },
                        onAddToBlacklist = { 
                            viewModel.addToBlacklist(app.appName, app.packageName)
                        },
                        onRemoveFromBlacklist = {
                            val blacklistedApp = blacklist.find { it.packageName == app.packageName }
                            blacklistedApp?.let { viewModel.removeFromBlacklist(it.id) }
                        },
                        onAddToTrusted = {
                            viewModel.addToTrustedApps(app.appName, app.packageName)
                        },
                        onRemoveFromTrusted = {
                            val trustedApp = trustedApps.find { it.packageName == app.packageName }
                            trustedApp?.let { viewModel.removeFromTrustedApps(it.id) }
                        },
                        onUninstall = {
                            val intent = viewModel.getUninstallIntent(app.packageName)
                            context.startActivity(intent)
                        },
                        isDarkTheme = isDarkTheme
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        } else if (!isVirusTotalScanning && scanResults.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = GuardianGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (showAllApps) "Нет приложений" else "Угроз не обнаружено",
                        style = MaterialTheme.typography.titleMedium,
                        color = GuardianGreen
                    )
                    if (stats.lastScanTime > 0) {
                        Text(
                            text = "Последнее сканирование: ${formatTime(stats.lastScanTime)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = grayText
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        } else if (!isVirusTotalScanning && scanResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = grayText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Нажмите на кнопку для сканирования вирусов",
                        style = MaterialTheme.typography.bodyMedium,
                        color = grayText
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    
    // VirusTotal Dialog
    if (showVirusTotalDialog) {
        AlertDialog(
            onDismissRequest = { showVirusTotalDialog = false },
            containerColor = surfaceColor,
            title = { 
                Text("VirusTotal", color = textColor, fontWeight = FontWeight.Bold) 
            },
            text = {
                Column {
                    Text(
                        text = "Сканирование через базу VirusTotal использует облачную проверку файлов по SHA-256 хэшу.",
                        color = grayText
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "⚠️ Лимит бесплатного API: 4 запроса в минуту. Полное сканирование может занять много времени.",
                        color = GuardianYellow,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (!viewModel.isVirusTotalApiKeyConfigured()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⚠️ API ключ не настроен. Добавьте его в Настройках.",
                            color = GuardianRed,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showVirusTotalDialog = false
                        viewModel.startVirusTotalScan()
                    }
                ) {
                    Text("Запустить", color = GuardianBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showVirusTotalDialog = false }) {
                    Text("Отмена", color = grayText)
                }
            }
        )
    }
    
    // VirusTotal App Picker Dialog
    if (showVirusTotalAppPicker) {
        val pm = context.packageManager
        val allApps = remember {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                .sortedBy { pm.getApplicationLabel(it).toString() }
        }
        var searchQuery by remember { mutableStateOf("") }
        val filteredApps = allApps.filter {
            pm.getApplicationLabel(it).toString().lowercase().contains(searchQuery.lowercase()) ||
            it.packageName.lowercase().contains(searchQuery.lowercase())
        }
        
        AlertDialog(
            onDismissRequest = { showVirusTotalAppPicker = false },
            containerColor = surfaceColor,
            title = { 
                Text("Выберите приложение", color = textColor, fontWeight = FontWeight.Bold) 
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Поиск...", color = grayText) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedPlaceholderColor = grayText,
                            unfocusedPlaceholderColor = grayText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.height(300.dp)
                    ) {
                        items(filteredApps) { app ->
                            val appName = pm.getApplicationLabel(app).toString()
                            val packageName = app.packageName
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        showVirusTotalAppPicker = false
                                        viewModel.scanSingleAppWithVirusTotal(packageName, appName)
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDarkTheme) GuardianSurface else GuardianSurfaceLight
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Apps,
                                        contentDescription = null,
                                        tint = if (isDarkTheme) GuardianBlue else GuardianBlueLight,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = appName,
                                            color = textColor,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = packageName,
                                            color = grayText,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = grayText
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showVirusTotalAppPicker = false }) {
                    Text("Отмена", color = grayText)
                }
            }
        )
    }
    
    // APK Scan Dialog with Activity Result Launcher
    if (showApkDialog) {
        val apkPickerLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { selectedUri ->
                // Pass URI directly to ViewModel for scanning
                viewModel.scanApkUri(context, selectedUri) { isThreat, message ->
                    apkResult = message
                }
            }
        }
        
        AlertDialog(
            onDismissRequest = { 
                showApkDialog = false
                apkResult = null
            },
            containerColor = surfaceColor,
            title = { 
                Text("Сканирование APK", color = textColor, fontWeight = FontWeight.Bold) 
            },
            text = {
                Column {
                    Text(
                        text = "Выберите APK файл для проверки на вирусы.",
                        color = grayText
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            apkPickerLauncher.launch("application/vnd.android.package-archive")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GuardianPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Выбрать файл APK")
                    }
                    if (apkResult != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (apkResult!!.contains("⚠️") || apkResult!!.contains("❌")) 
                                    GuardianRed.copy(alpha = 0.1f) 
                                else 
                                    GuardianGreen.copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (apkResult!!.contains("⚠️") || apkResult!!.contains("❌")) 
                                        Icons.Default.Warning 
                                    else 
                                        Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (apkResult!!.contains("⚠️") || apkResult!!.contains("❌")) 
                                        GuardianRed 
                                    else 
                                        GuardianGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = apkResult!!,
                                    color = if (apkResult!!.contains("⚠️") || apkResult!!.contains("❌")) 
                                        GuardianRed 
                                    else 
                                        GuardianGreen,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    showApkDialog = false 
                    apkResult = null
                }) {
                    Text("Закрыть", color = grayText)
                }
            }
        )
    }
}

// Helper function to get file path from URI
private fun getPathFromUri(context: Context, uri: Uri): String? {
    return try {
        when {
            DocumentsContract.isDocumentUri(context, uri) -> {
                val docId = DocumentsContract.getDocumentId(uri)
                when {
                    docId.startsWith("raw:") -> docId.substring(4)
                    docId.startsWith("primary:") -> {
                        val split = docId.split(":")
                        android.os.Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                    }
                    else -> null
                }
            }
            "content".equals(uri.scheme, ignoreCase = true) -> {
                // Try to query the content provider
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val columnIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.DATA)
                    if (cursor.moveToFirst()) {
                        cursor.getString(columnIndex)
                    } else null
                }
            }
            "file".equals(uri.scheme, ignoreCase = true) -> uri.path
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) GuardianSurface else GuardianSurfaceLight
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDarkTheme) 0.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = if (isDarkTheme) Color.White else Color(0xFF1E293B),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = if (isDarkTheme) Color.Gray else Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun AppListItem(
    app: AppScanResult,
    isBlacklisted: Boolean,
    isTrusted: Boolean,
    onAddToBlacklist: () -> Unit,
    onRemoveFromBlacklist: () -> Unit,
    onAddToTrusted: () -> Unit,
    onRemoveFromTrusted: () -> Unit,
    onUninstall: () -> Unit,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1E293B)
    val grayText = if (isDarkTheme) Color.Gray else Color(0xFF64748B)
    val cardBackground = if (isDarkTheme) GuardianSurface else GuardianSurfaceLight
    
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon placeholder
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when {
                            isTrusted -> GuardianGreen.copy(alpha = 0.2f)
                            app.isThreat -> GuardianRed.copy(alpha = 0.2f)
                            app.isSystemApp -> GuardianBlue.copy(alpha = 0.1f)
                            else -> GuardianGreen.copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isTrusted -> Icons.Default.CheckCircle
                        app.isThreat -> Icons.Default.Warning
                        app.virusTotalResult != null -> Icons.Default.BugReport
                        app.isSystemApp -> Icons.Default.Android
                        else -> Icons.Default.CheckCircle
                    },
                    contentDescription = null,
                    tint = when {
                        isTrusted -> GuardianGreen
                        app.isThreat -> GuardianRed
                        app.virusTotalResult != null -> GuardianYellow
                        app.isSystemApp -> GuardianBlue
                        else -> GuardianGreen
                    },
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleSmall,
                    color = textColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = grayText
                )
                if (isTrusted) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "✓ Доверенное",
                        style = MaterialTheme.typography.bodySmall,
                        color = GuardianGreen
                    )
                } else if (app.isThreat && app.threatType.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = app.threatType,
                        style = MaterialTheme.typography.bodySmall,
                        color = GuardianRed
                    )
                }
                if (app.virusTotalResult != null && !app.virusTotalResult.isInfected) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Безопасно (${app.virusTotalResult.detectedBy}/${app.virusTotalResult.totalScanners})",
                        style = MaterialTheme.typography.bodySmall,
                        color = GuardianGreen
                    )
                }
            }
            
            // System app badge
            if (app.isSystemApp && !app.isThreat && !isTrusted) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = GuardianBlue.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "Системное",
                        style = MaterialTheme.typography.labelSmall,
                        color = GuardianBlue,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            
            // Trusted badge
            if (isTrusted) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = GuardianGreen.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "Доверенное",
                        style = MaterialTheme.typography.labelSmall,
                        color = GuardianGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            
            // Add/Remove from blacklist button for threats
            if ((app.isThreat || isBlacklisted) && !isTrusted) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = if (isBlacklisted) onRemoveFromBlacklist else onAddToBlacklist,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isBlacklisted) Icons.Default.CheckCircle else Icons.Default.Block,
                        contentDescription = if (isBlacklisted) "Удалить из черного списка" else "Добавить в черный список",
                        tint = if (isBlacklisted) GuardianGreen else GuardianRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // Trust / Untrust button (checkmark) - shown for threats or any app
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = if (isTrusted) onRemoveFromTrusted else onAddToTrusted,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isTrusted) Icons.Default.CheckCircle else Icons.Default.CheckCircle,
                    contentDescription = if (isTrusted) "Удалить из доверенных" else "Добавить в доверенные",
                    tint = if (isTrusted) GuardianGreen else grayText,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // Uninstall button for threats (non-system apps only)
            if (app.isThreat && !app.isSystemApp && !isTrusted) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onUninstall,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить приложение",
                        tint = GuardianRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("dd MMM HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
