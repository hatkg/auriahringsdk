package com.jcring.app.presentation.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.jcring.app.data.bluetooth.RealBluetoothManager
import com.jcring.app.data.logger.LogManager
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(bluetoothManager: RealBluetoothManager) {
    val isConnected by bluetoothManager.isConnected.collectAsState()
    val deviceName by bluetoothManager.deviceName.collectAsState()
    val batteryLevel by bluetoothManager.batteryLevel.collectAsState()
    
    var showDeviceInfo by remember { mutableStateOf(false) }
    var showPersonalInfo by remember { mutableStateOf(false) }
    var showHealthAlerts by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Configurações",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Device status section
        item {
            DeviceStatusCard(
                isConnected = isConnected,
                deviceName = deviceName,
                batteryLevel = batteryLevel,
                bluetoothManager = bluetoothManager,
                onShowDeviceInfo = { showDeviceInfo = true }
            )
        }
        
        // Settings categories
        item {
            SettingsCategory(
                title = "Dispositivo",
                settings = getDeviceSettings { setting ->
                    when (setting.id) {
                        "device_info" -> showDeviceInfo = true
                        "sync_data" -> { /* Sync data */ }
                        "firmware_update" -> { /* Check firmware update */ }
                        "reset_device" -> { /* Reset device */ }
                    }
                }
            )
        }
        
        item {
            SettingsCategory(
                title = "Perfil & Saúde",
                settings = getHealthSettings { setting ->
                    when (setting.id) {
                        "personal_info" -> showPersonalInfo = true
                        "health_alerts" -> showHealthAlerts = true
                        "measurement_intervals" -> { /* Configure intervals */ }
                        "health_goals" -> { /* Set health goals */ }
                    }
                }
            )
        }
        
        item {
            SettingsCategory(
                title = "Dados & Privacidade",
                settings = getDataSettings { setting ->
                    when (setting.id) {
                        "export_data" -> { /* Export data */ }
                        "clear_data" -> { /* Clear data */ }
                        "privacy_settings" -> { /* Privacy settings */ }
                        "data_sharing" -> { /* Data sharing settings */ }
                    }
                }
            )
        }
        
        item {
            SettingsCategory(
                title = "Aplicativo",
                settings = getAppSettings { setting ->
                    when (setting.id) {
                        "notifications" -> { /* Notification settings */ }
                        "theme" -> { /* Theme settings */ }
                        "language" -> { /* Language settings */ }
                        "about" -> { /* About app */ }
                    }
                }
            )
        }
        
        // Quick actions
        item {
            QuickActionsCard(bluetoothManager)
        }
    }
    
    // Dialogs
    if (showDeviceInfo) {
        DeviceInfoDialog(
            deviceName = deviceName,
            batteryLevel = batteryLevel,
            onDismiss = { showDeviceInfo = false }
        )
    }
    
    if (showPersonalInfo) {
        PersonalInfoDialog(
            onDismiss = { showPersonalInfo = false }
        )
    }
    
    if (showHealthAlerts) {
        HealthAlertsDialog(
            onDismiss = { showHealthAlerts = false }
        )
    }
}

@Composable
private fun DeviceStatusCard(
    isConnected: Boolean,
    deviceName: String,
    batteryLevel: Int,
    bluetoothManager: RealBluetoothManager,
    onShowDeviceInfo: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) Color(0xFF4CAF50).copy(alpha = 0.1f) 
                           else MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (isConnected) Color(0xFF4CAF50) else Color(0xFFFF5722),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                            contentDescription = "Bluetooth Status",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = if (isConnected) deviceName.ifEmpty { "J2301B Ring" } else "Dispositivo Desconectado",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isConnected) "Conectado e sincronizando" else "Toque para conectar",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFFF5722)
                        )
                    }
                }
                
                if (isConnected) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Battery4Bar,
                            contentDescription = "Battery",
                            tint = when {
                                batteryLevel > 50 -> Color(0xFF4CAF50)
                                batteryLevel > 20 -> Color(0xFFFF9800)
                                else -> Color(0xFFFF5722)
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "$batteryLevel%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            if (isConnected) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onShowDeviceInfo,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Info do Dispositivo")
                    }
                    
                    Button(
                        onClick = { bluetoothManager.disconnect() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                    ) {
                        Icon(Icons.Default.BluetoothDisabled, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Desconectar")
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { /* Navigate to connection screen */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Conectar Dispositivo")
                }
            }
        }
    }
}

@Composable
private fun SettingsCategory(
    title: String,
    settings: List<SettingItem>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            settings.forEach { setting ->
                SettingItemRow(setting)
                if (setting != settings.last()) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingItemRow(setting: SettingItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { setting.onClick() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(setting.color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = setting.icon,
                    contentDescription = setting.title,
                    tint = setting.color,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = setting.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (setting.subtitle.isNotEmpty()) {
                    Text(
                        text = setting.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Go",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun QuickActionsCard(bluetoothManager: RealBluetoothManager) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Ações Rápidas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Primeira linha de botões
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.Sync,
                    label = "Sincronizar",
                    color = Color(0xFF2196F3),
                    onClick = { bluetoothManager.requestAllHealthData() }
                )
                
                QuickActionButton(
                    icon = Icons.Default.Battery4Bar,
                    label = "Bateria",
                    color = Color(0xFF4CAF50),
                    onClick = { bluetoothManager.requestBatteryLevel() }
                )
                
                QuickActionButton(
                    icon = Icons.Default.Vibration,
                    label = "Vibrar",
                    color = Color(0xFFFF9800),
                    onClick = { /* Send vibration */ }
                )
                
                QuickActionButton(
                    icon = Icons.Default.RestartAlt,
                    label = "Reset",
                    color = Color(0xFFFF5722),
                    onClick = { /* Reset device */ }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Segunda linha - Botão de Debug
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        val filePath = LogManager.exportLogs(context)
                        if (filePath != null) {
                            Toast.makeText(context, "Logs exportados para: Downloads/${filePath.substringAfterLast("/")}", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Erro ao exportar logs", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Export Logs"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("EXPORTAR LOGS DEBUG (${LogManager.getLogCount()})")
                }
            }
            
            Text(
                text = "Use este botão para exportar logs de debug e me enviar por favor!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceInfoDialog(
    deviceName: String,
    batteryLevel: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Informações do Dispositivo") },
        text = {
            Column {
                InfoRow("Nome", deviceName.ifEmpty { "J2301B Ring" })
                InfoRow("Modelo", "J2301B")
                InfoRow("Versão Firmware", "1.2.4")
                InfoRow("Versão Hardware", "2.1")
                InfoRow("Bateria", "$batteryLevel%")
                InfoRow("Última Sincronização", SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()))
                InfoRow("Tempo Conectado", "2h 35min")
                InfoRow("Status", "Operacional")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonalInfoDialog(onDismiss: () -> Unit) {
    var age by remember { mutableStateOf("65") }
    var height by remember { mutableStateOf("170") }
    var weight by remember { mutableStateOf("70") }
    var gender by remember { mutableStateOf("Masculino") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Informações Pessoais") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Idade") },
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = { Text("Altura (cm)") },
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Peso (kg)") },
                    singleLine = true
                )
                
                // Gender selection would go here
                Text(
                    text = "Gênero: $gender",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HealthAlertsDialog(onDismiss: () -> Unit) {
    var heartRateAlerts by remember { mutableStateOf(true) }
    var spo2Alerts by remember { mutableStateOf(true) }
    var temperatureAlerts by remember { mutableStateOf(true) }
    var glucoseAlerts by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alertas de Saúde") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Frequência Cardíaca")
                    Switch(
                        checked = heartRateAlerts,
                        onCheckedChange = { heartRateAlerts = it }
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Saturação de Oxigênio")
                    Switch(
                        checked = spo2Alerts,
                        onCheckedChange = { spo2Alerts = it }
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Temperatura")
                    Switch(
                        checked = temperatureAlerts,
                        onCheckedChange = { temperatureAlerts = it }
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Glicose")
                    Switch(
                        checked = glucoseAlerts,
                        onCheckedChange = { glucoseAlerts = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// Data classes
data class SettingItem(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

// Helper functions
private fun getDeviceSettings(onSettingClick: (SettingItem) -> Unit): List<SettingItem> {
    return listOf(
        SettingItem(
            id = "device_info",
            title = "Informações do Dispositivo",
            subtitle = "Modelo, firmware, bateria",
            icon = Icons.Default.Info,
            color = Color(0xFF2196F3),
            onClick = { onSettingClick(SettingItem("device_info", "", "", Icons.Default.Info, Color.Blue) {}) }
        ),
        SettingItem(
            id = "sync_data",
            title = "Sincronizar Dados",
            subtitle = "Atualizar dados do anel",
            icon = Icons.Default.Sync,
            color = Color(0xFF4CAF50),
            onClick = { onSettingClick(SettingItem("sync_data", "", "", Icons.Default.Sync, Color.Green) {}) }
        ),
        SettingItem(
            id = "firmware_update",
            title = "Atualização de Firmware",
            subtitle = "Verificar atualizações",
            icon = Icons.Default.SystemUpdate,
            color = Color(0xFFFF9800),
            onClick = { onSettingClick(SettingItem("firmware_update", "", "", Icons.Default.SystemUpdate, Color(0xFFFF9800)) {}) }
        ),
        SettingItem(
            id = "reset_device",
            title = "Resetar Dispositivo",
            subtitle = "Restaurar configurações",
            icon = Icons.Default.RestartAlt,
            color = Color(0xFFFF5722),
            onClick = { onSettingClick(SettingItem("reset_device", "", "", Icons.Default.RestartAlt, Color.Red) {}) }
        )
    )
}

private fun getHealthSettings(onSettingClick: (SettingItem) -> Unit): List<SettingItem> {
    return listOf(
        SettingItem(
            id = "personal_info",
            title = "Informações Pessoais",
            subtitle = "Idade, peso, altura",
            icon = Icons.Default.Person,
            color = Color(0xFF9C27B0),
            onClick = { onSettingClick(SettingItem("personal_info", "", "", Icons.Default.Person, Color.Magenta) {}) }
        ),
        SettingItem(
            id = "health_alerts",
            title = "Alertas de Saúde",
            subtitle = "Configurar notificações",
            icon = Icons.Default.NotificationImportant,
            color = Color(0xFFE91E63),
            onClick = { onSettingClick(SettingItem("health_alerts", "", "", Icons.Default.NotificationImportant, Color.Red) {}) }
        ),
        SettingItem(
            id = "measurement_intervals",
            title = "Intervalos de Medição",
            subtitle = "Frequência de coleta",
            icon = Icons.Default.Timer,
            color = Color(0xFF00BCD4),
            onClick = { onSettingClick(SettingItem("measurement_intervals", "", "", Icons.Default.Timer, Color.Cyan) {}) }
        ),
        SettingItem(
            id = "health_goals",
            title = "Metas de Saúde",
            subtitle = "Passos, calorias, exercícios",
            icon = Icons.Default.EmojiEvents,
            color = Color(0xFFFFD700),
            onClick = { onSettingClick(SettingItem("health_goals", "", "", Icons.Default.EmojiEvents, Color.Yellow) {}) }
        )
    )
}

private fun getDataSettings(onSettingClick: (SettingItem) -> Unit): List<SettingItem> {
    return listOf(
        SettingItem(
            id = "export_data",
            title = "Exportar Dados",
            subtitle = "PDF, CSV, compartilhar",
            icon = Icons.Default.FileDownload,
            color = Color(0xFF4CAF50),
            onClick = { onSettingClick(SettingItem("export_data", "", "", Icons.Default.FileDownload, Color.Green) {}) }
        ),
        SettingItem(
            id = "clear_data",
            title = "Limpar Dados",
            subtitle = "Remover histórico local",
            icon = Icons.Default.Delete,
            color = Color(0xFFFF5722),
            onClick = { onSettingClick(SettingItem("clear_data", "", "", Icons.Default.Delete, Color.Red) {}) }
        ),
        SettingItem(
            id = "privacy_settings",
            title = "Configurações de Privacidade",
            subtitle = "Controle de dados",
            icon = Icons.Default.Security,
            color = Color(0xFF795548),
            onClick = { onSettingClick(SettingItem("privacy_settings", "", "", Icons.Default.Security, Color.Gray) {}) }
        ),
        SettingItem(
            id = "data_sharing",
            title = "Compartilhamento",
            subtitle = "Família, médicos",
            icon = Icons.Default.Share,
            color = Color(0xFF2196F3),
            onClick = { onSettingClick(SettingItem("data_sharing", "", "", Icons.Default.Share, Color.Blue) {}) }
        )
    )
}

private fun getAppSettings(onSettingClick: (SettingItem) -> Unit): List<SettingItem> {
    return listOf(
        SettingItem(
            id = "notifications",
            title = "Notificações",
            subtitle = "Alertas e lembretes",
            icon = Icons.Default.Notifications,
            color = Color(0xFFFF9800),
            onClick = { onSettingClick(SettingItem("notifications", "", "", Icons.Default.Notifications, Color(0xFFFF9800)) {}) }
        ),
        SettingItem(
            id = "theme",
            title = "Tema",
            subtitle = "Claro, escuro, automático",
            icon = Icons.Default.Palette,
            color = Color(0xFF9C27B0),
            onClick = { onSettingClick(SettingItem("theme", "", "", Icons.Default.Palette, Color.Magenta) {}) }
        ),
        SettingItem(
            id = "language",
            title = "Idioma",
            subtitle = "Português (Brasil)",
            icon = Icons.Default.Language,
            color = Color(0xFF00BCD4),
            onClick = { onSettingClick(SettingItem("language", "", "", Icons.Default.Language, Color.Cyan) {}) }
        ),
        SettingItem(
            id = "about",
            title = "Sobre o App",
            subtitle = "Versão, suporte, créditos",
            icon = Icons.Default.Info,
            color = Color(0xFF607D8B),
            onClick = { onSettingClick(SettingItem("about", "", "", Icons.Default.Info, Color.Gray) {}) }
        )
    )
}