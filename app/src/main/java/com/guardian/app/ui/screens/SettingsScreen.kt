package com.guardian.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GuardianBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "Настройки",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Настройка защиты",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Protection Section
        Text(
            text = "ЗАЩИТА",
            style = MaterialTheme.typography.labelMedium,
            color = GuardianPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        SettingsItem(
            icon = Icons.Default.Shield,
            iconTint = GuardianGreen,
            title = "Активная защита",
            subtitle = if (isProtectionEnabled) "Мониторинг включен" else "Мониторинг выключен",
            trailing = {
                Switch(
                    checked = isProtectionEnabled,
                    onCheckedChange = { viewModel.setProtectionEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = GuardianGreen,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = GuardianSurfaceVariant
                    )
                )
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Appearance Section
        Text(
            text = "ОФОРМЛЕНИЕ",
            style = MaterialTheme.typography.labelMedium,
            color = GuardianPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        SettingsItem(
            icon = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
            iconTint = if (isDarkTheme) GuardianYellow else GuardianBlue,
            title = "Тёмная тема",
            subtitle = if (isDarkTheme) "Включена" else "Выключена",
            trailing = {
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { viewModel.setDarkTheme(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = GuardianPrimary,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = GuardianSurfaceVariant
                    )
                )
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Security Section
        Text(
            text = "БЕЗОПАСНОСТЬ",
            style = MaterialTheme.typography.labelMedium,
            color = GuardianPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        SettingsItem(
            icon = Icons.Default.Usb,
            iconTint = if (isUsbMonitorEnabled) GuardianBlue else GuardianSurfaceVariant,
            title = "Мониторинг USB",
            subtitle = if (isUsbMonitorEnabled) "Включен" else "Выключен",
            trailing = {
                Switch(
                    checked = isUsbMonitorEnabled,
                    onCheckedChange = { viewModel.setUsbMonitorEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = GuardianBlue,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = GuardianSurfaceVariant
                    )
                )
            }
        )
        
        SettingsItem(
            icon = Icons.Default.Sms,
            iconTint = GuardianBlue,
            title = "Фильтр SMS",
            subtitle = "Блокировка мошеннических SMS",
            trailing = {
                Switch(
                    checked = viewModel.isSmsFilterEnabled.collectAsState().value,
                    onCheckedChange = { viewModel.setSmsFilterEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = GuardianBlue,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = GuardianSurfaceVariant
                    )
                )
            }
        )
        
        SettingsItem(
            icon = Icons.Default.Phone,
            iconTint = GuardianPink,
            title = "Фильтр вызовов",
            subtitle = "Блокировка подозрительных вызовов",
            trailing = {
                Switch(
                    checked = viewModel.isCallFilterEnabled.collectAsState().value,
                    onCheckedChange = { viewModel.setCallFilterEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = GuardianPink,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = GuardianSurfaceVariant
                    )
                )
            }
        )
        
        SettingsItem(
            icon = Icons.Default.Apps,
            iconTint = GuardianYellow,
            title = "Мониторинг приложений",
            subtitle = "Отслеживание установки приложений",
            trailing = {
                Switch(
                    checked = viewModel.isAppMonitorEnabled.collectAsState().value,
                    onCheckedChange = { viewModel.setAppMonitorEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = GuardianYellow,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = GuardianSurfaceVariant
                    )
                )
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // VirusTotal Section
        Text(
            text = "VIRUSTOTAL",
            style = MaterialTheme.typography.labelMedium,
            color = GuardianPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        SettingsItem(
            icon = Icons.Default.BugReport,
            iconTint = if (viewModel.isVirusTotalApiKeyConfigured()) GuardianGreen else GuardianYellow,
            title = "API ключ VirusTotal",
            subtitle = if (viewModel.isVirusTotalApiKeyConfigured()) "Настроен" else "Нажмите для настройки",
            onClick = { showApiKeyDialog = true }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Data Section
        Text(
            text = "ДАННЫЕ",
            style = MaterialTheme.typography.labelMedium,
            color = GuardianPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        SettingsItem(
            icon = Icons.Default.Refresh,
            iconTint = GuardianRed,
            title = "Сброс статистики",
            subtitle = "Очистить все сохраненные данные",
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
                color = Color.DarkGray
            )
        }
        
        // Reset Dialog
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                containerColor = GuardianSurface,
                title = { Text("Сбросить статистику?", color = Color.White) },
                text = { Text("Это очистит всю статистику и историю событий.", color = Color.Gray) },
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
                        Text("Отмена", color = Color.Gray)
                    }
                }
            )
        }
        
        // API Key Dialog
        if (showApiKeyDialog) {
            AlertDialog(
                onDismissRequest = { showApiKeyDialog = false },
                containerColor = GuardianSurface,
                title = { 
                    Text(
                        "API ключ VirusTotal", 
                        color = Color.White, 
                        fontWeight = FontWeight.Bold 
                    ) 
                },
                text = {
                    Column {
                        Text(
                            "Введите ваш API ключ VirusTotal для облачного сканирования.",
                            color = Color.Gray
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
                                        tint = Color.Gray
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = GuardianBlue,
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = GuardianBlue,
                                unfocusedLabelColor = Color.Gray
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
                        Text("Отмена", color = Color.Gray)
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
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GuardianSurface)
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
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            trailing?.invoke()
        }
    }
}
