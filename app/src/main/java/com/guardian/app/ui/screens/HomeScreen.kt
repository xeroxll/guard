package com.guardian.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
    
    // Theme-aware colors
    val backgroundColor = if (isDarkTheme) GuardianBackground else GuardianBackgroundLight
    val surfaceColor = if (isDarkTheme) GuardianSurface else GuardianSurfaceLight
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1E293B)
    val grayText = if (isDarkTheme) Color.Gray else Color(0xFF64748B)
    
    val scanProgress = if (stats.appsScanned > 0) {
        (stats.appsScanned.toFloat() / 200 * 100).coerceIn(0f, 100f)
    } else 0f
    
    val recentEvents = events.take(5)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
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
            colors = CardDefaults.cardColors(containerColor = GuardianSurface)
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
                        containerColor = GuardianPrimary
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
        
        // Stats Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Usb,
                iconTint = if (isUsbMonitorEnabled) GuardianGreen else GuardianSurfaceVariant,
                title = "USB",
                value = if (isUsbMonitorEnabled) "ВКЛ" else "ВЫКЛ",
                onClick = { viewModel.setUsbMonitorEnabled(!isUsbMonitorEnabled) }
            )
            
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Warning,
                iconTint = GuardianRed,
                title = "Угрозы",
                value = stats.threatsBlocked.toString(),
                onClick = { }
            )
            
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CheckCircle,
                iconTint = GuardianGreen,
                title = "Безопасно",
                value = (stats.appsScanned - stats.threatsBlocked).coerceAtLeast(0).toString(),
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
                colors = CardDefaults.cardColors(containerColor = GuardianSurface)
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
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Нет активности",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
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
                        time = formatTime(event.timestamp)
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
                iconTint = GuardianGreen,
                onClick = { viewModel.startScan() }
            )
            
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Refresh,
                title = "Сброс",
                subtitle = "Очистить статистику",
                iconTint = GuardianRed,
                onClick = { viewModel.resetStats() }
            )
        }
    }
}

@Composable
private fun EventItem(
    title: String,
    description: String,
    time: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = GuardianSurface)
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
                    color = textColor
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            Text(
                text = time,
                style = MaterialTheme.typography.labelSmall,
                color = Color.DarkGray
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
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GuardianSurface)
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
                color = textColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
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
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GuardianSurface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTint.copy(alpha = 0.2f)),
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
                    color = Color.Gray
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
