package com.guardian.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.guardian.app.ui.theme.*
import com.guardian.app.viewmodel.GuardianViewModel

@Composable
fun SettingsScreen(viewModel: GuardianViewModel) {
    val isProtectionEnabled by viewModel.isProtectionEnabled.collectAsState()
    val isUsbMonitorEnabled by viewModel.isUsbMonitorEnabled.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }
    
    // Theme-aware colors
    val backgroundColor = if (isDarkTheme) GuardianBackground else GuardianBackgroundLight
    val surfaceColor = if (isDarkTheme) GuardianSurface else GuardianSurfaceLight
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1E293B)
    val grayText = if (isDarkTheme) Color.Gray else Color(0xFF64748B)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        Text(
            text = "Настройки",
            style = MaterialTheme.typography.headlineLarge,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Настройка защиты",
            style = MaterialTheme.typography.bodyMedium,
            color = grayText
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Protection Section
        Text(
            text = "ЗАЩИТА",
            style = MaterialTheme.typography.labelMedium,
            color = if (isDarkTheme) GuardianPrimary else GuardianPrimaryLight,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        SettingsItem(
            icon = Icons.Default.Shield,
            iconTint = if (isDarkTheme) GuardianGreen else GuardianGreenLight,
            title = "Активная защита",
            subtitle = if (isProtectionEnabled) "Мониторинг включен" else "Мониторинг выключен",
            isDarkTheme = isDarkTheme,
            trailing = {
                Switch(
                    checked = isProtectionEnabled,
                    onCheckedChange = { viewModel.setProtectionEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = if (isDarkTheme) GuardianGreen else GuardianGreenLight,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = if (isDarkTheme) GuardianSurfaceVariant else GuardianSurfaceVariantLight
                    )
                )
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Appearance Section
        Text(
            text = "ОФОРМЛЕНИЕ",
            style = MaterialTheme.typography.labelMedium,
            color = if (isDarkTheme) GuardianPrimary else GuardianPrimaryLight,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        SettingsItem(
            icon = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
            iconTint = if (isDarkTheme) GuardianYellow else GuardianBlueLight,
            title = "Тёмная тема",
            subtitle = if (isDarkTheme) "Включена" else "Выключена",
            isDarkTheme = isDarkTheme,
            trailing = {
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { viewModel.setDarkTheme(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = if (isDarkTheme) GuardianPrimary else GuardianPrimaryLight,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = if (isDarkTheme) GuardianSurfaceVariant else GuardianSurfaceVariantLight
                    )
                )
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Security Section
        Text(
            text = "БЕЗОПАСНОСТЬ",
            style = MaterialTheme.typography.labelMedium,
            color = if (isDarkTheme) GuardianPrimary else GuardianPrimaryLight,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        SettingsItem(
            icon = Icons.Default.Usb,
            iconTint = if (isUsbMonitorEnabled) (if (isDarkTheme) GuardianBlue else GuardianBlueLight) else Color.Gray,
            title = "Мониторинг USB",
            subtitle = if (isUsbMonitorEnabled) "Включен" else "Выключен",
            isDarkTheme = isDarkTheme,
            trailing = {
                Switch(
                    checked = isUsbMonitorEnabled,
                    onCheckedChange = { viewModel.setUsbMonitorEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = if (isDarkTheme) GuardianBlue else GuardianBlueLight,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = if (isDarkTheme) GuardianSurfaceVariant else GuardianSurfaceVariantLight
                    )
                )
            }
        )
        
        // SMS and Call filters removed for Google Play compliance
        
        SettingsItem(
            icon = Icons.Default.Apps,
            iconTint = if (isDarkTheme) GuardianYellow else GuardianYellowLight,
            title = "Мониторинг приложений",
            subtitle = "Отслеживание установки приложений",
            isDarkTheme = isDarkTheme,
            trailing = {
                Switch(
                    checked = viewModel.isAppMonitorEnabled.collectAsState().value,
                    onCheckedChange = { viewModel.setAppMonitorEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = if (isDarkTheme) GuardianYellow else GuardianYellowLight,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = if (isDarkTheme) GuardianSurfaceVariant else GuardianSurfaceVariantLight
                    )
                )
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // VirusTotal Section
        Text(
            text = "VIRUSTOTAL",
            style = MaterialTheme.typography.labelMedium,
            color = if (isDarkTheme) GuardianPrimary else GuardianPrimaryLight,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        SettingsItem(
            icon = Icons.Default.BugReport,
            iconTint = if (viewModel.isVirusTotalApiKeyConfigured()) (if (isDarkTheme) GuardianGreen else GuardianGreenLight) else (if (isDarkTheme) GuardianYellow else GuardianYellowLight),
            title = "API ключ VirusTotal",
            subtitle = if (viewModel.isVirusTotalApiKeyConfigured()) "Настроен" else "Нажмите для настройки",
            isDarkTheme = isDarkTheme,
            onClick = { showApiKeyDialog = true }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Data Section
        Text(
            text = "ДАННЫЕ",
            style = MaterialTheme.typography.labelMedium,
            color = if (isDarkTheme) GuardianPrimary else GuardianPrimaryLight,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        SettingsItem(
            icon = Icons.Default.Refresh,
            iconTint = GuardianRed,
            title = "Сброс статистики",
            subtitle = "Очистить все сохраненные данные",
            isDarkTheme = isDarkTheme,
            onClick = { showResetDialog = true }
        )
        
        // Version
        Spacer(modifier = Modifier.weight(1f))
        
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Guardian v1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = grayText
            )
        }
        
        // Reset Dialog
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                containerColor = surfaceColor,
                title = { Text("Сбросить статистику?", color = textColor) },
                text = { Text("Это очистит всю статистику и историю событий.", color = grayText) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.resetStats()
                            showResetDialog = false
                        }
                    ) {
                        Text("Сбросить", color = GuardianRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("Отмена", color = grayText)
                    }
                }
            )
        }
        
        // API Key Dialog
        if (showApiKeyDialog) {
            AlertDialog(
                onDismissRequest = { showApiKeyDialog = false },
                containerColor = surfaceColor,
                title = { 
                    Text(
                        "API ключ VirusTotal", 
                        color = textColor, 
                        fontWeight = FontWeight.Bold 
                    ) 
                },
                text = {
                    Column {
                        Text(
                            "Введите ваш API ключ VirusTotal для облачного сканирования.",
                            color = grayText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Получить бесплатный ключ на virustotal.com",
                            color = GuardianBlue,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            label = { Text("API ключ") },
                            singleLine = true,
                            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { showApiKey = !showApiKey }) {
                                    Icon(
                                        imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showApiKey) "Скрыть" else "Показать",
                                        tint = grayText
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = GuardianBlue,
                                unfocusedBorderColor = grayText,
                                focusedLabelColor = GuardianBlue,
                                unfocusedLabelColor = grayText
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (apiKeyInput.isNotBlank()) {
                                viewModel.setVirusTotalApiKey(apiKeyInput)
                            }
                            showApiKeyDialog = false
                            apiKeyInput = ""
                        }
                    ) {
                        Text("Сохранить", color = GuardianGreen)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showApiKeyDialog = false
                        apiKeyInput = ""
                    }) {
                        Text("Отмена", color = grayText)
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    isDarkTheme: Boolean,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val cardBackground = if (isDarkTheme) GuardianSurface else GuardianSurfaceLight
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1E293B)
    val grayText = if (isDarkTheme) Color.Gray else Color(0xFF64748B)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
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
                    .size(40.dp)
                    .background(iconTint.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = textColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = grayText
                )
            }
            
            trailing?.invoke()
        }
    }
}
