package com.guardian.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guardian.app.ui.theme.*
import com.guardian.app.viewmodel.GuardianViewModel

@Composable
fun HomeScreen(
    viewModel: GuardianViewModel,
    onNavigateToEvents: () -> Unit = {}
) {
    val isProtectionEnabled by viewModel.isProtectionEnabled.collectAsState()
    val isUsbMonitorEnabled by viewModel.isUsbMonitorEnabled.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val events by viewModel.events.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Check USB debugging state
    val isUsbDebuggingEnabled = viewModel.isUsbDebuggingEnabled
    
    // Theme-aware colors
    val backgroundColor = if (isDarkTheme) GuardianBackground else GuardianBackgroundLight
    val surfaceColor = if (isDarkTheme) GuardianSurface else GuardianSurfaceLight
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1E293B)
    val grayText = if (isDarkTheme) Color.Gray else Color(0xFF64748B)
    
    val scanProgress = if (stats.appsScanned > 0) {
        (stats.appsScanned.toFloat() / 200 * 100).coerceIn(0f, 100f)
    } else 0f
    
    val recentEvents = events.take(5)
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Guardian",
                    style = MaterialTheme.typography.headlineLarge,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isProtectionEnabled) "Защита активна" else "Защита отключена",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isProtectionEnabled) GuardianGreen else GuardianRed
                )
            }
            
            Switch(
                checked = isProtectionEnabled,
                onCheckedChange = { viewModel.setProtectionEnabled(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = textColor,
                    checkedTrackColor = GuardianGreen,
                    uncheckedThumbColor = textColor,
                    uncheckedTrackColor = GuardianSurfaceVariant
                )
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Main Shield
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                if (isProtectionEnabled) GuardianPrimary.copy(alpha = 0.3f) else GuardianRed.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            
            Card(
                modifier = Modifier
                    .size(160.dp)
                    .shadow(24.dp, CircleShape),
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = GuardianSurfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isProtectionEnabled) Icons.Default.Shield else Icons.Default.ShieldMoon,
                            contentDescription = "Shield",
                            modifier = Modifier.size(56.dp),
                            tint = if (isProtectionEnabled) GuardianPrimary else GuardianRed
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isProtectionEnabled) "БЕЗОПАСНО" else "ВЫКЛ",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isProtectionEnabled) GuardianGreen else GuardianRed
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) GuardianSurface else GuardianSurfaceLight),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Статус сканирования",
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${stats.appsScanned} приложений",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GuardianGreen
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LinearProgressIndicator(
                    progress = { scanProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = GuardianGreen,
                    trackColor = GuardianSurfaceVariant,
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (stats.lastScanTime > 0) "Последнее: ${formatTime(stats.lastScanTime)}" else "Не сканировалось",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "${stats.threatsBlocked} угроз найдено",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (stats.threatsBlocked > 0) GuardianRed else Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = { viewModel.startScan() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkTheme) GuardianPrimary else GuardianPrimaryLight
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Быстрая проверка")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // USB Debugging Warning Card
        if (isUsbDebuggingEnabled) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = viewModel.openDeveloperSettings()
                        context.startActivity(intent)
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) GuardianRed.copy(alpha = 0.15f) else Color(0xFFFEF2F2)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isDarkTheme) GuardianRed.copy(alpha = 0.3f) else GuardianRed.copy(alpha = 0.2f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (isDarkTheme) GuardianRed.copy(alpha = 0.2f) else GuardianRed.copy(alpha = 0.1f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = if (isDarkTheme) GuardianRed else GuardianRedLight
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "USB отладка включена",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isDarkTheme) GuardianRed else GuardianRedLight,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Нажмите, чтобы открыть настройки разработчика и выключить",
                            style = MaterialTheme.typography.bodySmall,
                            color = grayText
                        )
                    }
                    
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = if (isDarkTheme) GuardianRed else GuardianRedLight
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Stats Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Usb,
                iconTint = if (isUsbDebuggingEnabled) GuardianRed else if (isUsbMonitorEnabled) GuardianGreen else Color.Gray,
                title = "USB",
                value = if (isUsbDebuggingEnabled) "⚠️" else if (isUsbMonitorEnabled) "ВКЛ" else "ВЫКЛ",
                isDarkTheme = isDarkTheme,
                onClick = { }
            )
            
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Warning,
                iconTint = if (stats.threatsBlocked > 0) GuardianRed else if (isDarkTheme) GuardianGreen else GuardianGreenLight,
                title = "Угрозы",
                value = stats.threatsBlocked.toString(),
                isDarkTheme = isDarkTheme,
                onClick = { }
            )
            
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CheckCircle,
                iconTint = if (isDarkTheme) GuardianGreen else GuardianGreenLight,
                title = "Безопасно",
                value = (stats.appsScanned - stats.threatsBlocked).coerceAtLeast(0).toString(),
                isDarkTheme = isDarkTheme,
                onClick = { }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Recent Activity
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToEvents() }
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Последняя активность",
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Перейти к журналу",
                tint = grayText
            )
        }
        
        if (recentEvents.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) GuardianSurface else GuardianSurfaceLight),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = grayText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Нет активности",
                        style = MaterialTheme.typography.bodyMedium,
                        color = grayText
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recentEvents.forEach { event ->
                    EventItem(
                        title = event.title,
                        description = event.description,
                        time = formatTime(event.timestamp),
                        isDarkTheme = isDarkTheme
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Quick Actions
        Text(
            text = "Быстрые действия",
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Security,
                title = "Полное сканирование",
                subtitle = "Проверить все",
                iconTint = if (isDarkTheme) GuardianGreen else GuardianGreenLight,
                textColor = textColor,
                isDarkTheme = isDarkTheme,
                onClick = { viewModel.startScan() }
            )
            
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Refresh,
                title = "Сброс",
                subtitle = "Очистить статистику",
                iconTint = GuardianRed,
                textColor = textColor,
                isDarkTheme = isDarkTheme,
                onClick = { viewModel.resetStats() }
            )
        }
    }
}

@Composable
private fun EventItem(
    title: String,
    description: String,
    time: String,
    textColor: Color = Color.White,
    isDarkTheme: Boolean = true
) {
    val actualTextColor = if (isDarkTheme) Color.White else Color(0xFF1E293B)
    val grayText = if (isDarkTheme) Color.Gray else Color(0xFF64748B)
    
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) GuardianSurface else GuardianSurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            title.contains("⚠️") || title.contains("Угроза") || title.contains("Threat") -> GuardianRed.copy(alpha = 0.2f)
                            title.contains("📱") -> GuardianBlue.copy(alpha = 0.2f)
                            title.contains("✅") || title.contains("Безопасно") -> GuardianGreen.copy(alpha = 0.2f)
                            else -> GuardianSurfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        title.contains("USB") -> Icons.Default.Usb
                        title.contains("App") || title.contains("Приложение") -> Icons.Default.Apps
                        title.contains("Scan") || title.contains("Скан") -> Icons.Default.Search
                        title.contains("Вирус") || title.contains("Virus") -> Icons.Default.BugReport
                        else -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = when {
                        title.contains("⚠️") || title.contains("Угроза") || title.contains("Threat") -> GuardianRed
                        title.contains("📱") -> GuardianBlue
                        title.contains("✅") || title.contains("Безопасно") -> GuardianGreen
                        else -> Color.Gray
                    },
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.replace(Regex("[📱🔍✅⚠️🦠⏳❌]"), "").trim(),
                    style = MaterialTheme.typography.titleSmall,
                    color = actualTextColor
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = grayText
                )
            }
            
            Text(
                text = time,
                style = MaterialTheme.typography.labelSmall,
                color = grayText
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    textColor: Color = Color.White,
    isDarkTheme: Boolean = true,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) GuardianSurface else GuardianSurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = if (isDarkTheme) Color.White else Color(0xFF1E293B),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = if (isDarkTheme) Color.Gray else Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    textColor: Color = Color.White,
    isDarkTheme: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) GuardianSurface else GuardianSurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = textColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkTheme) Color.Gray else Color(0xFF64748B)
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    if (timestamp == 0L) return "Никогда"
    val sdf = java.text.SimpleDateFormat("dd MMM HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
