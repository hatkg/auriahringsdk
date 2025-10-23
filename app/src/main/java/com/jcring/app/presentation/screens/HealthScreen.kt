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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jcring.app.data.bluetooth.RealBluetoothManager
import com.jcring.app.presentation.ui.components.MeasurementModal
import com.jcring.app.presentation.ui.components.MeasurementType
import com.jcring.app.presentation.ui.components.MeasurementState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HealthScreen(bluetoothManager: RealBluetoothManager) {
    val isConnected by bluetoothManager.isConnected.collectAsState()
    val heartRate by bluetoothManager.heartRate.collectAsState()
    val spO2 by bluetoothManager.spO2.collectAsState()
    val temperature by bluetoothManager.temperature.collectAsState()
    val steps by bluetoothManager.steps.collectAsState()
    val calories by bluetoothManager.calories.collectAsState()
    val batteryLevel by bluetoothManager.batteryLevel.collectAsState()
    
    // Estados dos modais de medição
    var showHeartRateModal by remember { mutableStateOf(false) }
    var showSpO2Modal by remember { mutableStateOf(false) }
    var showTemperatureModal by remember { mutableStateOf(false) }
    
    var heartRateMeasurementState by remember { mutableStateOf(MeasurementState.PREPARING) }
    var spO2MeasurementState by remember { mutableStateOf(MeasurementState.PREPARING) }
    var temperatureMeasurementState by remember { mutableStateOf(MeasurementState.PREPARING) }
    
    var isMeasuringHeartRate by remember { mutableStateOf(false) }
    var isMeasuringSpO2 by remember { mutableStateOf(false) }
    var showHeartRateHistory by remember { mutableStateOf(false) }
    var showSpO2History by remember { mutableStateOf(false) }
    
    // 🔥 MONITORAR dados em tempo real e completar medições automaticamente
    LaunchedEffect(heartRate) {
        if (heartRate != null && heartRateMeasurementState == MeasurementState.MEASURING) {
            heartRateMeasurementState = MeasurementState.COMPLETED
        }
    }
    
    LaunchedEffect(spO2) {
        if (spO2 != null && spO2MeasurementState == MeasurementState.MEASURING) {
            spO2MeasurementState = MeasurementState.COMPLETED
        }
    }
    
    LaunchedEffect(temperature) {
        if (temperature != null && temperatureMeasurementState == MeasurementState.MEASURING) {
            temperatureMeasurementState = MeasurementState.COMPLETED
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Monitoramento de Saúde",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        if (!isConnected) {
            item {
                DisconnectedCard()
            }
        } else {
            // Real-time measurements section
            item {
                Text(
                    text = "Medições em Tempo Real",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // Heart Rate Section
            item {
                HealthMeasurementCard(
                    title = "Frequência Cardíaca",
                    value = heartRate?.toString() ?: "--",
                    unit = "bpm",
                    icon = Icons.Default.Favorite,
                    color = Color(0xFFE91E63),
                    status = getHeartRateStatus(heartRate),
                    isActive = isMeasuringHeartRate,
                    onMeasure = {
                        showHeartRateModal = true
                    },
                    onShowHistory = { showHeartRateHistory = true }
                )
            }
            
            // SpO2 Section
            item {
                HealthMeasurementCard(
                    title = "Saturação de Oxigênio",
                    value = spO2?.toString() ?: "--",
                    unit = "%",
                    icon = Icons.Default.Air,
                    color = Color(0xFF2196F3),
                    status = getSpO2Status(spO2),
                    isActive = isMeasuringSpO2,
                    onMeasure = {
                        showSpO2Modal = true
                    },
                    onShowHistory = { showSpO2History = true }
                )
            }
            
            // Temperature Section
            item {
                HealthMeasurementCard(
                    title = "Temperatura Corporal",
                    value = temperature?.let { "%.1f".format(it) } ?: "--",
                    unit = "°C",
                    icon = Icons.Default.Thermostat,
                    color = Color(0xFFFF9800),
                    status = getTemperatureStatus(temperature),
                    isActive = false, // Temperatura é contínua
                    onMeasure = {
                        showTemperatureModal = true
                    },
                    onShowHistory = { 
                        // TODO: Implementar histórico de temperatura
                    }
                )
            }
            
            // Activity Section
            item {
                ActivityCard(steps, calories)
            }
            
            // Sleep Section
            item {
                SleepCard(bluetoothManager)
            }
            
            // Device Status
            item {
                DeviceStatusCard(batteryLevel, bluetoothManager)
            }
        }
    }
    
    // History dialogs
    if (showHeartRateHistory) {
        HistoryDialog(
            title = "Histórico de Frequência Cardíaca",
            onDismiss = { showHeartRateHistory = false }
        )
    }
    
    if (showSpO2History) {
        HistoryDialog(
            title = "Histórico de SpO2",
            onDismiss = { showSpO2History = false }
        )
    }
    
    // 🔥 MODAIS DE MEDIÇÃO COM ANIMAÇÃO CONTÍNUA
    // Modal de frequência cardíaca
    MeasurementModal(
        measurementType = MeasurementType.HEART_RATE,
        isVisible = showHeartRateModal,
        onDismiss = { 
            showHeartRateModal = false
            heartRateMeasurementState = MeasurementState.PREPARING
        },
        onStartMeasurement = {
            heartRateMeasurementState = MeasurementState.MEASURING
            bluetoothManager.startHeartRateMeasurement { success ->
                heartRateMeasurementState = if (success) MeasurementState.MEASURING else MeasurementState.ERROR
            }
        },
        onStopMeasurement = {
            heartRateMeasurementState = MeasurementState.PREPARING
        },
        currentValue = if (heartRateMeasurementState == MeasurementState.COMPLETED) heartRate?.toString() else null,
        state = heartRateMeasurementState
    )
    
    // Modal de SpO2
    MeasurementModal(
        measurementType = MeasurementType.SPO2,
        isVisible = showSpO2Modal,
        onDismiss = { 
            showSpO2Modal = false
            spO2MeasurementState = MeasurementState.PREPARING
        },
        onStartMeasurement = {
            spO2MeasurementState = MeasurementState.MEASURING
            bluetoothManager.startSpO2Measurement { success ->
                spO2MeasurementState = if (success) MeasurementState.MEASURING else MeasurementState.ERROR
            }
        },
        onStopMeasurement = {
            spO2MeasurementState = MeasurementState.PREPARING
        },
        currentValue = if (spO2MeasurementState == MeasurementState.COMPLETED) spO2?.toString() else null,
        state = spO2MeasurementState
    )
    
    // Modal de temperatura
    MeasurementModal(
        measurementType = MeasurementType.TEMPERATURE,
        isVisible = showTemperatureModal,
        onDismiss = { 
            showTemperatureModal = false
            temperatureMeasurementState = MeasurementState.PREPARING
        },
        onStartMeasurement = {
            temperatureMeasurementState = MeasurementState.MEASURING
            // Temperatura não tem comando específico no SDK, usar dados contínuos
        },
        onStopMeasurement = {
            temperatureMeasurementState = MeasurementState.PREPARING
        },
        currentValue = if (temperatureMeasurementState == MeasurementState.COMPLETED) temperature?.toString() else null,
        state = temperatureMeasurementState
    )
}

@Composable
private fun DisconnectedCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.BluetoothDisabled,
                contentDescription = "Disconnected",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Anel J2301B não conectado",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "Conecte o anel para acessar funcionalidades de saúde",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun HealthMeasurementCard(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    color: Color,
    status: String,
    isActive: Boolean,
    onMeasure: () -> Unit,
    onShowHistory: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
                            .size(40.dp)
                            .background(color, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodySmall,
                            color = getStatusColor(status)
                        )
                    }
                }
                
                Text(
                    text = "Última atualização: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Current value display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isActive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = color
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Medindo...",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Text(
                        text = "$value $unit",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onMeasure,
                    modifier = Modifier.weight(1f),
                    enabled = !isActive,
                    colors = ButtonDefaults.buttonColors(containerColor = color)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Medir Agora")
                }
                
                OutlinedButton(
                    onClick = onShowHistory,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Histórico")
                }
            }
        }
    }
}



@Composable
private fun ActivityCard(steps: Int, calories: Int) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Atividade de Hoje",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActivityMetric(
                    icon = Icons.Default.DirectionsWalk,
                    value = steps.toString(),
                    label = "Passos",
                    color = Color(0xFF4CAF50),
                    progress = (steps / 10000f).coerceIn(0f, 1f)
                )
                
                ActivityMetric(
                    icon = Icons.Default.LocalFireDepartment,
                    value = calories.toString(),
                    label = "Calorias",
                    color = Color(0xFFFF5722),
                    progress = (calories / 2000f).coerceIn(0f, 1f)
                )
                
                ActivityMetric(
                    icon = Icons.Default.Timer,
                    value = "${(steps / 120).coerceAtLeast(0)}", // Rough estimate
                    label = "Minutos Ativos",
                    color = Color(0xFF9C27B0),
                    progress = ((steps / 120f) / 60f).coerceIn(0f, 1f)
                )
            }
        }
    }
}

@Composable
private fun ActivityMetric(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    progress: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = progress,
                modifier = Modifier.size(60.dp),
                color = color,
                strokeWidth = 6.dp
            )
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SleepCard(bluetoothManager: RealBluetoothManager) {
    val sleepData = bluetoothManager.getSleepData()
    
    Card(
        modifier = Modifier.fillMaxWidth()
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
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = "Sleep",
                        tint = Color(0xFF9C27B0),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Sono da Última Noite",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Qualidade: ${sleepData.third}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Text(
                    text = "${sleepData.first}h ${sleepData.second}min",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9C27B0)
                )
            }
        }
    }
}

@Composable
private fun DeviceStatusCard(batteryLevel: Int, bluetoothManager: RealBluetoothManager) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Status do Dispositivo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DeviceStatusItem("🔋", "Bateria", "$batteryLevel%")
                DeviceStatusItem("📡", "Sinal", "Forte")
                DeviceStatusItem("🔄", "Sincronização", "Ativa")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { bluetoothManager.requestBatteryLevel() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Atualizar Status")
            }
        }
    }
}

@Composable
private fun DeviceStatusItem(emoji: String, label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = emoji, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryDialog(
    title: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text("Histórico dos últimos 7 dias:")
                Spacer(modifier = Modifier.height(16.dp))
                
                // Mock historical data for demonstration
                val mockData = listOf(
                    "Hoje: 72 bpm (Normal)",
                    "Ontem: 75 bpm (Normal)", 
                    "2 dias: 69 bpm (Normal)",
                    "3 dias: 78 bpm (Normal)",
                    "4 dias: 71 bpm (Normal)",
                    "5 dias: 73 bpm (Normal)",
                    "6 dias: 76 bpm (Normal)"
                )
                
                mockData.forEach { data ->
                    Text(
                        text = data,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

// Helper functions
private fun getHeartRateStatus(heartRate: Int?): String {
    return when {
        heartRate == null -> "Não medido"
        heartRate < 60 -> "Baixa (Bradicardia)"
        heartRate > 100 -> "Alta (Taquicardia)"
        else -> "Normal"
    }
}

private fun getSpO2Status(spO2: Int?): String {
    return when {
        spO2 == null -> "Não medido"
        spO2 < 95 -> "Baixa (Consulte médico)"
        spO2 >= 98 -> "Excelente"
        else -> "Normal"
    }
}

private fun getTemperatureStatus(temperature: Float?): String {
    return when {
        temperature == null -> "Não medida"
        temperature < 36.0 -> "Hipotermia"
        temperature > 37.5 -> "Febre"
        temperature > 38.0 -> "Febre alta"
        else -> "Normal"
    }
}

private fun getStatusColor(status: String): Color {
    return when {
        status.contains("Normal") || status.contains("Excelente") -> Color(0xFF4CAF50)
        status.contains("Baixa") || status.contains("Alta") || status.contains("Febre") -> Color(0xFFFF5722)
        else -> Color(0xFFFF9800)
    }
}

private fun getTemperatureStatusColor(temperature: Float?): Color {
    return when {
        temperature == null -> Color(0xFF9E9E9E)
        temperature < 36.0 || temperature > 37.5 -> Color(0xFFFF5722)
        else -> Color(0xFF4CAF50)
    }
}