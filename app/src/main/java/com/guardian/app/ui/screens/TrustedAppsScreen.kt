package com.guardian.app.ui.screens

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.guardian.app.data.model.TrustedApp
import com.guardian.app.ui.theme.*
import com.guardian.app.viewmodel.GuardianViewModel

@Composable
fun TrustedAppsScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val trustedApps by viewModel.trustedApps.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    
    val backgroundColor = if (isDarkTheme) GuardianBackground else GuardianBackgroundLight
    val surfaceColor = if (isDarkTheme) GuardianSurface else GuardianSurfaceLight
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1E293B)
    val grayText = if (isDarkTheme) Color.Gray else Color(0xFF64748B)
    
    var showDeleteDialog by remember { mutableStateOf<TrustedApp?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Назад",
                    tint = textColor
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Доверенные приложения",
                    style = MaterialTheme.typography.headlineSmall,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${trustedApps.size} приложений в белом списке",
                    style = MaterialTheme.typography.bodyMedium,
                    color = grayText
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = GuardianGreen.copy(alpha = 0.1f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                        .background(GuardianGreen.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = GuardianGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Защита доверенных",
                        style = MaterialTheme.typography.titleMedium,
                        color = GuardianGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Приложения из этого списка не будут помечаться как угрозы",
                        style = MaterialTheme.typography.bodySmall,
                        color = grayText
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Trusted apps list
        if (trustedApps.isEmpty()) {
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
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = grayText.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Список пуст",
                        style = MaterialTheme.typography.titleMedium,
                        color = grayText
                    )
                    Text(
                        text = "Добавляйте приложения в доверенные из сканера",
                        style = MaterialTheme.typography.bodySmall,
                        color = grayText
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(trustedApps) { app ->
                    TrustedAppItem(
                        app = app,
                        isDarkTheme = isDarkTheme,
                        onRemove = { showDeleteDialog = app }
                    )
                }
            }
        }
    }
    
    // Delete confirmation dialog
    showDeleteDialog?.let { app ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            containerColor = surfaceColor,
            title = { 
                Text(
                    "Удалить из доверенных?", 
                    color = textColor,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = { 
                Column {
                    Text(
                        text = "Приложение \"${app.name}\" будет удалено из белого списка.",
                        color = grayText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "При следующем сканировании оно может быть помечено как угроза, если вызывает подозрения.",
                        color = GuardianYellow,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeFromTrustedApps(app.id)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Удалить", color = GuardianRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Отмена", color = grayText)
                }
            }
        )
    }
}

@Composable
private fun TrustedAppItem(
    app: TrustedApp,
    isDarkTheme: Boolean,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1E293B)
    val grayText = if (isDarkTheme) Color.Gray else Color(0xFF64748B)
    val cardBackground = if (isDarkTheme) GuardianSurface else GuardianSurfaceLight
    
    // Load app icon
    val appIcon by remember(app.packageName) {
        mutableStateOf(loadAppIcon(context, app.packageName))
    }
    
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
            // App icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GuardianGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon!!.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = GuardianGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = textColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = grayText
                )
            }
            
            // Trusted badge
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = GuardianGreen.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "Доверенное",
                    style = MaterialTheme.typography.labelSmall,
                    color = GuardianGreen,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Remove button
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Удалить из доверенных",
                    tint = GuardianRed,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun loadAppIcon(context: Context, packageName: String): Drawable? {
    return try {
        val pm = context.packageManager
        pm.getApplicationIcon(packageName)
    } catch (e: Exception) {
        null
    }
}
