package com.guardian.app.ui.screens

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
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
    val isVirusTotalScanning by viewModel.isVirusTotalScanning.collectAsState()
    val virusTotalProgress by viewModel.virusTotalProgress.collectAsState()
    val virusTotalResults by viewModel.virusTotalResults.collectAsState()
    
    var isScanning by remember { mutableStateOf(false) }
    var scanResults by remember { mutableStateOf<List<AppScanResult>>(emptyList()) }
    var showVirusTotalDialog by remember { mutableStateOf(false) }
    var showAllApps by remember { mutableStateOf(false) }
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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GuardianBackground)
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Сканер",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Проверка установленных приложений",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Scan Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick Scan Button
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // Glow effect
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(
                                    if (isScanning) GuardianPrimary.copy(alpha = 0.3f) else GuardianGreen.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                
                Card(
                    modifier = Modifier
                        .size(120.dp)
                        .clickable(enabled = !isScanning && !isVirusTotalScanning) {
                            isScanning = true
                            scanResults = emptyList()
                            
                            scope.launch {
                                val pm = context.packageManager
                                val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                                val blacklistedPackages = blacklist.map { it.packageName }.toSet()
                                
                                val threats = mutableListOf<AppScanResult>()
                                
                                for (app in installedApps) {
                                    val packageName = app.packageName
                                    val appName = pm.getApplicationLabel(app).toString()
                                    val isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                                    
                                    var isThreat = false
                                    var threatType = ""
                                    
                                    // Check blacklist
                                    if (blacklistedPackages.contains(packageName)) {
                                        isThreat = true
                                        threatType = "В черном списке"
                                    }
                                    
                                    // Check VirusTotal results
                                    val vtResult = virusTotalResults[packageName]
                                    if (vtResult != null && vtResult.isInfected) {
                                        isThreat = true
                                        threatType = "VirusTotal: ${vtResult.malwareName ?: "Обнаружено"}"
                                    }
                                    
                                    threats.add(
                                        AppScanResult(
                                            packageName = packageName,
                                            appName = appName,
                                            isThreat = isThreat,
                                            threatType = threatType,
                                            isSystemApp = isSystemApp,
                                            virusTotalResult = vtResult
                                        )
                                    )
                                    
                                    kotlinx.coroutines.delay(1)
                                }
                                
                                scanResults = threats
                                isScanning = false
                                viewModel.startScan()
                            }
                        },
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = GuardianSurface)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isScanning) Icons.Default.Refresh else Icons.Default.Security,
                            contentDescription = "Быстрая проверка",
                            modifier = Modifier
                                .size(48.dp)
                                .then(if (isScanning) Modifier.rotate(rotation) else Modifier),
                            tint = if (isScanning) GuardianPrimary else GuardianGreen
                        )
                    }
                }
            }
            
            // VirusTotal Button
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(
                                    if (isVirusTotalScanning) GuardianYellow.copy(alpha = 0.3f) else GuardianBlue.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                
                Card(
                    modifier = Modifier
                        .size(120.dp)
                        .clickable(enabled = !isScanning && !isVirusTotalScanning) {
                            showVirusTotalDialog = true
                        },
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = GuardianSurface)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isVirusTotalScanning) Icons.Default.Sync else Icons.Default.BugReport,
                            contentDescription = "VirusTotal",
                            modifier = Modifier
                                .size(48.dp)
                                .then(if (isVirusTotalScanning) Modifier.rotate(rotation) else Modifier),
                            tint = if (isVirusTotalScanning) GuardianYellow else GuardianBlue
                        )
                    }
                }
            }
        }
        
        // Scan Status
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when {
                isScanning -> "Сканирование..."
                isVirusTotalScanning -> "VirusTotal: ${virusTotalProgress.first}/${virusTotalProgress.second}"
                else -> "Нажмите для проверки"
            },
            style = MaterialTheme.typography.titleMedium,
            color = when {
                isScanning -> GuardianPrimary
                isVirusTotalScanning -> GuardianYellow
                else -> Color.Gray
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
                iconTint = GuardianGreen,
                title = "Проверено",
                value = stats.appsScanned.toString()
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Block,
                iconTint = GuardianRed,
                title = "Угрозы",
                value = stats.threatsBlocked.toString()
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Warning,
                iconTint = GuardianYellow,
                title = "В списке",
                value = blacklist.size.toString()
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
                color = Color.White,
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
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(displayedApps) { app ->
                    AppListItem(
                        app = app,
                        isBlacklisted = blacklist.any { it.packageName == app.packageName },
                        onAddToBlacklist = { 
                            viewModel.addToBlacklist(app.appName, app.packageName)
                        },
                        onRemoveFromBlacklist = {
                            val blacklistedApp = blacklist.find { it.packageName == app.packageName }
                            blacklistedApp?.let { viewModel.removeFromBlacklist(it.id) }
                        },
                        onUninstall = {
                            val intent = viewModel.getUninstallIntent(app.packageName)
                            context.startActivity(intent)
                        }
                    )
                }
            }
        } else if (!isScanning && !isVirusTotalScanning && scanResults.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
                            color = Color.Gray
                        )
                    }
                }
            }
        } else if (!isScanning && scanResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Нажмите на кнопку для сканирования",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }
    }
    
    // VirusTotal Dialog
    if (showVirusTotalDialog) {
        AlertDialog(
            onDismissRequest = { showVirusTotalDialog = false },
            containerColor = GuardianSurface,
            title = { 
                Text("VirusTotal", color = Color.White, fontWeight = FontWeight.Bold) 
            },
            text = {
                Column {
                    Text(
                        text = "Сканирование через базу VirusTotal использует облачную проверку файлов по SHA-256 хэшу.",
                        color = Color.Gray
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
                    Text("Отмена", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GuardianSurface)
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
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun AppListItem(
    app: AppScanResult,
    isBlacklisted: Boolean,
    onAddToBlacklist: () -> Unit,
    onRemoveFromBlacklist: () -> Unit,
    onUninstall: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (app.isThreat) GuardianSurface else GuardianSurface
        )
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
                            app.isThreat -> GuardianRed.copy(alpha = 0.2f)
                            app.isSystemApp -> GuardianBlue.copy(alpha = 0.1f)
                            else -> GuardianGreen.copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        app.isThreat -> Icons.Default.Warning
                        app.virusTotalResult != null -> Icons.Default.BugReport
                        app.isSystemApp -> Icons.Default.Android
                        else -> Icons.Default.CheckCircle
                    },
                    contentDescription = null,
                    tint = when {
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
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                if (app.isThreat && app.threatType.isNotEmpty()) {
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
            if (app.isSystemApp && !app.isThreat) {
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
            
            // Add/Remove from blacklist button for threats
            if (app.isThreat || isBlacklisted) {
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
            
            // Uninstall button for threats (non-system apps only)
            if (app.isThreat && !app.isSystemApp) {
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
