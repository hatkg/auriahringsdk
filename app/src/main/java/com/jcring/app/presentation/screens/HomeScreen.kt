package com.jcring.app.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import com.jcring.app.data.bluetooth.RealBluetoothManager
import com.jcring.app.data.bluetooth.BluetoothDevice
import com.jcring.app.data.logger.LogManager
import com.jcring.app.presentation.ui.components.MeasurementModal
import com.jcring.app.presentation.ui.components.MeasurementType
import com.jcring.app.presentation.ui.components.MeasurementState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import android.widget.Toast

@Composable
fun HomeScreen(bluetoothManager: RealBluetoothManager) {
    val isConnected by bluetoothManager.isConnected.collectAsState()
    val deviceName by bluetoothManager.deviceName.collectAsState()
    val heartRate by bluetoothManager.heartRate.collectAsState()
    val spO2 by bluetoothManager.spO2.collectAsState()
    val temperature by bluetoothManager.temperature.collectAsState()
    val steps by bluetoothManager.steps.collectAsState()
    val calories by bluetoothManager.calories.collectAsState()
    val batteryLevel by bluetoothManager.batteryLevel.collectAsState()
    val hrv by bluetoothManager.hrv.collectAsState()
    val stress by bluetoothManager.stress.collectAsState()
    
    
    // Debug logs para verificar se os dados estão sendo recebidos
    LaunchedEffect(heartRate, spO2, temperature, steps, calories, batteryLevel) {
        Log.d("HomeScreen", "📊 Dados atuais - HR: $heartRate, SpO2: $spO2, Temp: $temperature, Steps: $steps, Cal: $calories, Bat: $batteryLevel")
    }
    
    // Solicitar dados automaticamente quando conecta
    LaunchedEffect(isConnected) {
        if (isConnected) {
            Log.d("HomeScreen", "🔗 Dispositivo conectado! Solicitando dados...")
            delay(2000) // Aguarda estabilizar conexão
            bluetoothManager.requestAllHealthData()
        }
    }
    
    var showDeviceSearch by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var foundDevices by remember { mutableStateOf(emptyList<BluetoothDevice>()) }
    
    // Estados dos modais de medição
    var showHeartRateModal by remember { mutableStateOf(false) }
    var showSpO2Modal by remember { mutableStateOf(false) }
    var showTemperatureModal by remember { mutableStateOf(false) }
    var showHrvModal by remember { mutableStateOf(false) }
    var showStressModal by remember { mutableStateOf(false) }
    
    // Estados das medições
    var heartRateMeasurementState by remember { mutableStateOf(MeasurementState.PREPARING) }
    var spO2MeasurementState by remember { mutableStateOf(MeasurementState.PREPARING) }
    var temperatureMeasurementState by remember { mutableStateOf(MeasurementState.PREPARING) }
    var hrvMeasurementState by remember { mutableStateOf(MeasurementState.PREPARING) }
    var stressMeasurementState by remember { mutableStateOf(MeasurementState.PREPARING) }
    
    // Progresso das medições
    var heartRateProgress by remember { mutableStateOf(0f) }
    var spO2Progress by remember { mutableStateOf(0f) }
    var temperatureProgress by remember { mutableStateOf(0f) }
    var hrvProgress by remember { mutableStateOf(0f) }
    var stressProgress by remember { mutableStateOf(0f) }
    
    // 🔥 MONITORAR dados em tempo real e completar medições automaticamente
    LaunchedEffect(heartRate) {
        if (heartRate != null && heartRateMeasurementState == MeasurementState.MEASURING) {
            Log.d("HomeScreen", "✅ HEART RATE RECEBIDO: $heartRate - Completando medição!")
            heartRateMeasurementState = MeasurementState.COMPLETED
        }
    }
    
    LaunchedEffect(spO2) {
        if (spO2 != null && spO2MeasurementState == MeasurementState.MEASURING) {
            Log.d("HomeScreen", "✅ SPO2 RECEBIDO: $spO2 - Completando medição!")
            spO2MeasurementState = MeasurementState.COMPLETED
        }
    }
    
    LaunchedEffect(temperature) {
        if (temperature != null && temperatureMeasurementState == MeasurementState.MEASURING) {
            Log.d("HomeScreen", "✅ TEMPERATURE RECEBIDA: $temperature - Completando medição!")
            temperatureMeasurementState = MeasurementState.COMPLETED
        }
    }
    
    // Efeitos para simular progresso das medições
    LaunchedEffect(heartRateMeasurementState) {
        if (heartRateMeasurementState == MeasurementState.MEASURING) {
            for (i in 1..30) {
                delay(100)
                heartRateProgress = i / 30f
            }
            if (heartRateMeasurementState == MeasurementState.MEASURING) {
                heartRateMeasurementState = MeasurementState.COMPLETED
            }
        }
    }
    
    LaunchedEffect(spO2MeasurementState) {
        if (spO2MeasurementState == MeasurementState.MEASURING) {
            for (i in 1..60) {
                delay(100)
                spO2Progress = i / 60f
            }
            if (spO2MeasurementState == MeasurementState.MEASURING) {
                spO2MeasurementState = MeasurementState.COMPLETED
            }
        }
    }
    
    LaunchedEffect(temperatureMeasurementState) {
        if (temperatureMeasurementState == MeasurementState.MEASURING) {
            for (i in 1..20) {
                delay(150)
                temperatureProgress = i / 20f
            }
            if (temperatureMeasurementState == MeasurementState.MEASURING) {
                temperatureMeasurementState = MeasurementState.COMPLETED
            }
        }
    }
    
    LaunchedEffect(stressMeasurementState) {
        if (stressMeasurementState == MeasurementState.MEASURING) {
            for (i in 1..40) {
                delay(125)
                stressProgress = i / 40f
            }
            if (stressMeasurementState == MeasurementState.MEASURING) {
                stressMeasurementState = MeasurementState.COMPLETED
            }
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with current time and device status
        item {
            HeaderCard(isConnected, deviceName, batteryLevel)
        }
        
        // Connection section
        if (!isConnected) {
            item {
                ConnectionSection(
                    onSearchClick = { 
                        showDeviceSearch = true
                        isSearching = true
                        bluetoothManager.startScan { devices ->
                            foundDevices = devices
                            isSearching = false
                        }
                    }
                )
            }
        }
        
        // Main health metrics - only show when connected
        if (isConnected) {
            item {
                Text(
                    text = "Métricas de Saúde",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(getHealthMetrics(heartRate, spO2, temperature, steps, calories)) { metric ->
                        HealthMetricCard(metric)
                    }
                }
            }
            
            // Quick actions
            item {
                Text(
                    text = "Ações Rápidas",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                QuickActionsGrid(
                    bluetoothManager = bluetoothManager,
                    onHeartRateClick = { showHeartRateModal = true },
                    onSpO2Click = { showSpO2Modal = true },
                    onTemperatureClick = { showTemperatureModal = true },
                    onStressClick = { showStressModal = true }
                )
            }
            
            // Recent measurements
            item {
                Text(
                    text = "Últimas Medições",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                RecentMeasurementsCard(heartRate, spO2, temperature)
            }
        }
        
        // Botão de export logs - SEMPRE VISÍVEL para debug
        item {
            DebugExportButton()
        }
    }
    
    // Device search dialog
    if (showDeviceSearch) {
        DeviceSearchDialog(
            isSearching = isSearching,
            devices = foundDevices,
            onDeviceSelected = { device ->
                bluetoothManager.connectToDevice(device) { success ->
                    if (success) {
                        showDeviceSearch = false
                        foundDevices = emptyList()
                    }
                }
            },
            onDismiss = { 
                showDeviceSearch = false
                foundDevices = emptyList()
                bluetoothManager.StopDeviceScan()
            }
        )
    }
    
    // Modais de medição manual
    
    // Modal de frequência cardíaca
    MeasurementModal(
        measurementType = MeasurementType.HEART_RATE,
        isVisible = showHeartRateModal,
        onDismiss = { 
            showHeartRateModal = false
            heartRateMeasurementState = MeasurementState.PREPARING
            heartRateProgress = 0f
        },
        onStartMeasurement = {
            heartRateMeasurementState = MeasurementState.MEASURING
            bluetoothManager.startHeartRateMeasurement { success ->
                heartRateMeasurementState = if (success) MeasurementState.MEASURING else MeasurementState.ERROR
            }
        },
        onStopMeasurement = {
            heartRateMeasurementState = MeasurementState.PREPARING
            heartRateProgress = 0f
        },
        currentValue = if (heartRateMeasurementState == MeasurementState.COMPLETED) heartRate?.toString() else null,
        progress = heartRateProgress,
        state = heartRateMeasurementState
    )
    
    // Modal de SpO2
    MeasurementModal(
        measurementType = MeasurementType.SPO2,
        isVisible = showSpO2Modal,
        onDismiss = { 
            showSpO2Modal = false
            spO2MeasurementState = MeasurementState.PREPARING
            spO2Progress = 0f
        },
        onStartMeasurement = {
            spO2MeasurementState = MeasurementState.MEASURING
            bluetoothManager.startSpO2Measurement { success ->
                spO2MeasurementState = if (success) MeasurementState.MEASURING else MeasurementState.ERROR
            }
        },
        onStopMeasurement = {
            spO2MeasurementState = MeasurementState.PREPARING
            spO2Progress = 0f
        },
        currentValue = if (spO2MeasurementState == MeasurementState.COMPLETED) spO2?.toString() else null,
        progress = spO2Progress,
        state = spO2MeasurementState
    )
    
    // Modal de temperatura
    MeasurementModal(
        measurementType = MeasurementType.TEMPERATURE,
        isVisible = showTemperatureModal,
        onDismiss = { 
            showTemperatureModal = false
            temperatureMeasurementState = MeasurementState.PREPARING
            temperatureProgress = 0f
        },
        onStartMeasurement = {
            temperatureMeasurementState = MeasurementState.MEASURING
            // Temperatura é automática no anel, só requestamos os dados
            bluetoothManager.requestAllHealthData()
        },
        onStopMeasurement = {
            temperatureMeasurementState = MeasurementState.PREPARING
            temperatureProgress = 0f
        },
        currentValue = if (temperatureMeasurementState == MeasurementState.COMPLETED) 
            temperature?.let { "%.1f".format(it) } else null,
        progress = temperatureProgress,
        state = temperatureMeasurementState
    )
    
    // Modal de HRV/Stress
    MeasurementModal(
        measurementType = MeasurementType.STRESS,
        isVisible = showStressModal,
        onDismiss = { 
            showStressModal = false
            stressMeasurementState = MeasurementState.PREPARING
            stressProgress = 0f
        },
        onStartMeasurement = {
            stressMeasurementState = MeasurementState.MEASURING
            bluetoothManager.measureStress { success ->
                stressMeasurementState = if (success) MeasurementState.MEASURING else MeasurementState.ERROR
            }
        },
        onStopMeasurement = {
            stressMeasurementState = MeasurementState.PREPARING
            stressProgress = 0f
        },
        currentValue = if (stressMeasurementState == MeasurementState.COMPLETED) stress?.toString() else null,
        progress = stressProgress,
        state = stressMeasurementState
    )
}

@Composable
private fun HeaderCard(isConnected: Boolean, deviceName: String, batteryLevel: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.surface
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
                Column {
                    Text(
                        text = "JC Ring Health",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("pt", "BR")).format(Date()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isConnected) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (isConnected) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Battery4Bar,
                            contentDescription = "Battery",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "$batteryLevel%",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }
            }
            
            if (isConnected) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "📱 Conectado ao $deviceName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun ConnectionSection(onSearchClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.BluetoothSearching,
                contentDescription = "Search",
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Conecte seu anel para visualizar dados de saúde em tempo real",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(20.dp))
            
            Button(
                onClick = onSearchClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Procurar Anel J2301B")
            }
        }
    }
}

@Composable
private fun HealthMetricCard(metric: HealthMetric) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = metric.backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = metric.icon,
                    contentDescription = metric.title,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = metric.status,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium
                )
            }
            
            Column {
                Text(
                    text = metric.value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = metric.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun QuickActionsGrid(
    bluetoothManager: RealBluetoothManager,
    onHeartRateClick: () -> Unit,
    onSpO2Click: () -> Unit,
    onTemperatureClick: () -> Unit,
    onStressClick: () -> Unit
) {
    val actions = listOf(
        QuickAction("Sincronizar Dados", Icons.Default.Sync, Color(0xFF2196F3)) {
            bluetoothManager.requestAllHealthData()
        },
        QuickAction("Medir Freq. Cardíaca", Icons.Default.Favorite, Color(0xFFE91E63)) {
            onHeartRateClick()
        },
        QuickAction("Medir SpO2", Icons.Default.Air, Color(0xFF03DAC5)) {
            onSpO2Click()
        },
        QuickAction("Medir Temperatura", Icons.Default.Thermostat, Color(0xFFFF9800)) {
            onTemperatureClick()
        },
        QuickAction("Solicitar Bateria", Icons.Default.Battery4Bar, Color(0xFF4CAF50)) {
            bluetoothManager.requestBatteryLevel()
        },
        QuickAction("Medição de Estresse", Icons.Default.Psychology, Color(0xFF9C27B0)) {
            onStressClick()
        }
    )
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(actions) { action ->
            QuickActionCard(action)
        }
    }
}

@Composable
private fun QuickActionCard(action: QuickAction) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable { action.onClick() },
        colors = CardDefaults.cardColors(containerColor = action.color)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.title,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = action.title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun RecentMeasurementsCard(heartRate: Int?, spO2: Int?, temperature: Float?) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Última Atualização: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MeasurementItem("❤️", "FC", heartRate?.toString() ?: "--", "bpm")
                MeasurementItem("🫁", "SpO2", spO2?.toString() ?: "--", "%")
                MeasurementItem("🌡️", "Temp", temperature?.let { "%.1f".format(it) } ?: "--", "°C")
            }
        }
    }
}

@Composable
private fun MeasurementItem(emoji: String, label: String, value: String, unit: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$value $unit",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceSearchDialog(
    isSearching: Boolean,
    devices: List<BluetoothDevice>,
    onDeviceSelected: (BluetoothDevice) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Procurando Anel J2301B") },
        text = {
            Column {
                if (isSearching) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Procurando dispositivos...")
                    }
                } else if (devices.isEmpty()) {
                    Text("Nenhum anel J2301B encontrado. Verifique se o anel está ligado e próximo.")
                } else {
                    Text("Anéis encontrados:")
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    devices.forEach { device ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDeviceSelected(device) }
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = device.name ?: "Dispositivo Desconhecido",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = device.address,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (device.rssi != 0) {
                                    Text(
                                        text = "Sinal: ${device.rssi} dBm",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
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

// Data classes
data class HealthMetric(
    val title: String,
    val value: String,
    val icon: ImageVector,
    val backgroundColor: Color,
    val status: String
)

data class QuickAction(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

// Helper functions
private fun getHealthMetrics(
    heartRate: Int?,
    spO2: Int?,
    temperature: Float?,
    steps: Int,
    calories: Int
): List<HealthMetric> {
    return listOf(
        HealthMetric(
            title = "Frequência Cardíaca",
            value = "${heartRate ?: "--"} bpm",
            icon = Icons.Default.Favorite,
            backgroundColor = Color(0xFFE91E63),
            status = when {
                heartRate == null -> "N/A"
                heartRate < 60 -> "Baixa"
                heartRate > 100 -> "Alta"
                else -> "Normal"
            }
        ),
        HealthMetric(
            title = "Saturação O2",
            value = "${spO2 ?: "--"}%",
            icon = Icons.Default.Air,
            backgroundColor = Color(0xFF2196F3),
            status = when {
                spO2 == null -> "N/A"
                spO2 < 95 -> "Baixa"
                else -> "Normal"
            }
        ),
        HealthMetric(
            title = "Temperatura",
            value = temperature?.let { "%.1f°C".format(it) } ?: "--°C",
            icon = Icons.Default.Thermostat,
            backgroundColor = Color(0xFFFF9800),
            status = when {
                temperature == null -> "N/A"
                temperature < 36.0 -> "Baixa"
                temperature > 37.5 -> "Alta"
                else -> "Normal"
            }
        ),
        HealthMetric(
            title = "Passos",
            value = steps.toString(),
            icon = Icons.Default.DirectionsWalk,
            backgroundColor = Color(0xFF4CAF50),
            status = when {
                steps < 5000 -> "Baixo"
                steps > 10000 -> "Excelente"
                else -> "Bom"
            }
        ),
        HealthMetric(
            title = "Calorias",
            value = "${calories} kcal",
            icon = Icons.Default.LocalFireDepartment,
            backgroundColor = Color(0xFFFF5722),
            status = "Hoje"
        )
    )
}

@Composable
private fun DebugExportButton() {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF9C27B0))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = "Debug",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "EXPORTAR LOGS DEBUG",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                text = "(${LogManager.getLogCount()} logs coletados)",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = {
                    val filePath = LogManager.exportLogs(context)
                    if (filePath != null) {
                        Toast.makeText(
                            context, 
                            "✅ Logs salvos em Downloads/${filePath.substringAfterLast("/")}", 
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(context, "❌ Erro ao exportar logs", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text(
                    text = "GERAR ARQUIVO DE LOGS",
                    color = Color(0xFF9C27B0),
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = "Clique para salvar os logs em Downloads e me envie o arquivo!",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}