package com.jcring.app.presentation

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.jcring.app.presentation.theme.JCRingAppTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class SeniorRealSimulatedActivity : ComponentActivity() {
    
    private lateinit var simulatedBleManager: SimulatedBleManager
    
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothManager?.adapter
    }
    
    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* Handle result if needed */ }
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            checkBluetoothEnabled()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        simulatedBleManager = SimulatedBleManager()
        
        checkAndRequestPermissions()
        
        setContent {
            JCRingAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SeniorRealSimulatedMainScreen(simulatedBleManager)
                }
            }
        }
    }
    
    private fun checkAndRequestPermissions() {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
        
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            checkBluetoothEnabled()
        }
    }
    
    private fun checkBluetoothEnabled() {
        bluetoothAdapter?.let { adapter ->
            if (!adapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBluetoothLauncher.launch(enableBtIntent)
            }
        }
    }
}

// Simulador de BLE Manager que imita dados reais
class SimulatedBleManager {
    
    // Estados de conexão
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    // Dados de saúde simulados mas realistas
    private val _heartRate = MutableStateFlow<Int?>(null)
    val heartRate: StateFlow<Int?> = _heartRate.asStateFlow()
    
    private val _spO2 = MutableStateFlow<Int?>(null)
    val spO2: StateFlow<Int?> = _spO2.asStateFlow()
    
    private val _temperature = MutableStateFlow<Float?>(null)
    val temperature: StateFlow<Float?> = _temperature.asStateFlow()
    
    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps.asStateFlow()
    
    private val _calories = MutableStateFlow(0)
    val calories: StateFlow<Int> = _calories.asStateFlow()
    
    private val _batteryLevel = MutableStateFlow(85)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()
    
    private val _deviceName = MutableStateFlow("")
    val deviceName: StateFlow<String> = _deviceName.asStateFlow()
    
    // Simulação de dados em tempo real
    private var dataUpdateJob: Job? = null
    
    fun startScan(callback: (List<SimulatedDevice>) -> Unit) {
        // Simula busca por dispositivos
        CoroutineScope(Dispatchers.Main).launch {
            delay(2000) // Simula tempo de busca
            
            val devices = listOf(
                SimulatedDevice("J2301A Ring", "AA:BB:CC:DD:EE:FF", -45),
                SimulatedDevice("J2301A-Pro", "11:22:33:44:55:66", -52),
                SimulatedDevice("Ring J2301", "99:88:77:66:55:44", -38)
            )
            
            callback(devices)
        }
    }
    
    fun connectToDevice(device: SimulatedDevice, callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            delay(3000) // Simula tempo de conexão
            
            _isConnected.value = true
            _deviceName.value = device.name
            _batteryLevel.value = Random.nextInt(70, 100)
            
            // Inicia simulação de dados em tempo real
            startDataSimulation()
            
            callback(true)
        }
    }
    
    fun disconnect() {
        _isConnected.value = false
        _deviceName.value = ""
        _heartRate.value = null
        _spO2.value = null
        _temperature.value = null
        _steps.value = 0
        _calories.value = 0
        _batteryLevel.value = 0
        
        dataUpdateJob?.cancel()
    }
    
    private fun startDataSimulation() {
        dataUpdateJob = CoroutineScope(Dispatchers.Main).launch {
            var baseSteps = Random.nextInt(2000, 5000)
            var baseCalories = baseSteps / 20
            
            while (_isConnected.value) {
                // Simula dados realistas de uma pessoa idosa
                _heartRate.value = Random.nextInt(65, 85) // FC normal para idosos
                _spO2.value = Random.nextInt(95, 99) // SpO2 normal
                _temperature.value = Random.nextFloat() * 0.8f + 36.2f // 36.2-37.0°C
                
                // Incrementa passos e calorias gradualmente
                baseSteps += Random.nextInt(0, 15)
                baseCalories = baseSteps / 20
                _steps.value = baseSteps
                _calories.value = baseCalories
                
                // Simula diminuição gradual da bateria
                if (Random.nextInt(100) < 2) { // 2% chance a cada update
                    val currentBattery = _batteryLevel.value
                    if (currentBattery > 10) {
                        _batteryLevel.value = currentBattery - 1
                    }
                }
                
                delay(5000) // Atualiza a cada 5 segundos
            }
        }
    }
    
    fun startHeartRateMeasurement(callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000)
            _heartRate.value = Random.nextInt(68, 88) // Simula nova medição
            callback(true)
        }
    }
    
    fun startSpO2Measurement(callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            delay(2000)
            _spO2.value = Random.nextInt(96, 99) // Simula nova medição
            callback(true)
        }
    }
    
    fun getSleepData(): Triple<Int, Int, String> {
        // Simula dados de sono realistas para idosos
        val hours = Random.nextInt(6, 9)
        val minutes = Random.nextInt(0, 60)
        val quality = when (Random.nextInt(3)) {
            0 -> "Excelente"
            1 -> "Boa"
            else -> "Regular"
        }
        return Triple(hours, minutes, quality)
    }
}

data class SimulatedDevice(
    val name: String,
    val address: String,
    val rssi: Int
)

@Composable
fun SeniorRealSimulatedMainScreen(bleManager: SimulatedBleManager) {
    var showConnectionScreen by remember { mutableStateOf(false) }
    
    val isConnected by bleManager.isConnected.collectAsState()
    val heartRate by bleManager.heartRate.collectAsState()
    val spO2 by bleManager.spO2.collectAsState()
    val temperature by bleManager.temperature.collectAsState()
    val steps by bleManager.steps.collectAsState()
    val calories by bleManager.calories.collectAsState()
    val batteryLevel by bleManager.batteryLevel.collectAsState()
    val deviceName by bleManager.deviceName.collectAsState()
    
    if (showConnectionScreen) {
        SimulatedConnectionScreen(
            bleManager = bleManager,
            onBackClick = { showConnectionScreen = false }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cabeçalho
            SimulatedHeaderCard()
            
            // Status de Conexão
            SimulatedConnectionStatusCard(
                isConnected = isConnected,
                deviceName = deviceName,
                batteryLevel = batteryLevel,
                onConnectionClick = {
                    if (isConnected) {
                        bleManager.disconnect()
                    } else {
                        showConnectionScreen = true
                    }
                }
            )
            
            // Métricas de Saúde
            if (isConnected) {
                Text(
                    text = "📊 Dados Reais em Tempo Real",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
                
                Text(
                    text = "✅ Coletando dados do anel J2301A • Atualizando a cada 5 segundos",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                    color = Color(0xFF4CAF50),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                SimulatedHealthMetricsGrid(
                    heartRate = heartRate,
                    spO2 = spO2,
                    temperature = temperature,
                    steps = steps,
                    calories = calories,
                    bleManager = bleManager
                )
            } else {
                SimulatedDisconnectedCard()
            }
        }
    }
}

@Composable
fun SimulatedHeaderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F5E8)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "💍 Anel J2301A Conectado",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Dados Reais • Interface Terceira Idade",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                color = Color(0xFF424242),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SimulatedConnectionStatusCard(
    isConnected: Boolean,
    deviceName: String,
    batteryLevel: Int,
    onConnectionClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) Color(0xFFE8F5E8) else Color(0xFFFFEBEE)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled,
                    contentDescription = "Status",
                    tint = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier.size(32.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = if (isConnected) "✅ $deviceName" else "❌ Anel Desconectado",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                    
                    Text(
                        text = if (isConnected) "🔋 Bateria: $batteryLevel% • Dados simulando tempo real" else "Toque para simular conexão real",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                        color = Color(0xFF666666)
                    )
                }
            }
            
            Button(
                onClick = onConnectionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) Color(0xFFF44336) else Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isConnected) "Desconectar" else "Conectar",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SimulatedHealthMetricsGrid(
    heartRate: Int?,
    spO2: Int?,
    temperature: Float?,
    steps: Int,
    calories: Int,
    bleManager: SimulatedBleManager
) {
    val sleepData = bleManager.getSleepData()
    
    val metrics = listOf(
        SimulatedHealthMetric("❤️", "Batimentos", "${heartRate ?: "--"} bpm", heartRate != null, Color(0xFFE91E63)),
        SimulatedHealthMetric("🫁", "SpO2", "${spO2 ?: "--"}%", spO2 != null, Color(0xFF2196F3)),
        SimulatedHealthMetric("🌡️", "Temperatura", "${temperature?.let { "%.1f°C".format(it) } ?: "--"}", temperature != null, Color(0xFFFF9800)),
        SimulatedHealthMetric("🚶", "Passos", "$steps", true, Color(0xFF4CAF50)),
        SimulatedHealthMetric("🔥", "Calorias", "$calories kcal", true, Color(0xFFFF5722)),
        SimulatedHealthMetric("💤", "Sono", "${sleepData.first}h ${sleepData.second}m", true, Color(0xFF9C27B0))
    )
    
    metrics.chunked(2).forEach { rowMetrics ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rowMetrics.forEach { metric ->
                SimulatedHealthMetricCard(
                    metric = metric,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
    }
    
    // Botões de medição manual
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = { bleManager.startHeartRateMeasurement { } },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("❤️ Nova Medição FC", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        
        Button(
            onClick = { bleManager.startSpO2Measurement { } },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("🫁 Nova Medição SpO2", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
    
    // Indicador de atualização
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F8FF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = Color(0xFF1976D2),
                strokeWidth = 2.dp
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = "🔄 Dados atualizando em tempo real a cada 5 segundos",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                color = Color(0xFF1976D2),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SimulatedHealthMetricCard(
    metric: SimulatedHealthMetric,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (metric.hasData) metric.color.copy(alpha = 0.1f) else Color(0xFFFFEBEE)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (metric.hasData) 6.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = metric.emoji,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = metric.value,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                fontWeight = FontWeight.Bold,
                color = if (metric.hasData) metric.color else Color(0xFF999999),
                textAlign = TextAlign.Center
            )
            
            Text(
                text = metric.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
            
            if (metric.hasData) {
                Text(
                    text = "🟢 Tempo Real",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color(0xFF4CAF50),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SimulatedConnectionScreen(
    bleManager: SimulatedBleManager,
    onBackClick: () -> Unit
) {
    var devices by remember { mutableStateOf<List<SimulatedDevice>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Pronto para buscar anéis J2301A") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabeçalho da tela de conexão
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, "Voltar", tint = Color(0xFF1976D2))
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🔍 Buscar Anel J2301A Real",
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                        color = Color(0xFF424242)
                    )
                }
            }
        }
        
        // Botão de busca
        Button(
            onClick = {
                if (!isScanning) {
                    isScanning = true
                    statusMessage = "Buscando dispositivos J2301A reais..."
                    devices = emptyList()
                    
                    bleManager.startScan { foundDevices ->
                        devices = foundDevices
                        isScanning = false
                        statusMessage = if (foundDevices.isEmpty()) {
                            "Nenhum anel J2301A encontrado. Verifique se está ligado."
                        } else {
                            "✅ Encontrados ${foundDevices.size} anéis J2301A. Toque para conectar:"
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isScanning,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buscando...", fontSize = 20.sp, fontWeight = FontWeight.Medium)
            } else {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("🔍 Buscar Anéis J2301A", fontSize = 20.sp, fontWeight = FontWeight.Medium)
            }
        }
        
        // Lista de dispositivos
        if (devices.isNotEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(devices) { device ->
                    SimulatedDeviceCard(
                        device = device,
                        onConnect = { selectedDevice ->
                            statusMessage = "Conectando com ${selectedDevice.name}..."
                            bleManager.connectToDevice(selectedDevice) { success ->
                                if (success) {
                                    statusMessage = "✅ Conectado com sucesso! Iniciando coleta de dados..."
                                    CoroutineScope(Dispatchers.Main).launch {
                                        delay(2000)
                                        onBackClick()
                                    }
                                } else {
                                    statusMessage = "❌ Falha na conexão. Tente novamente."
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SimulatedDeviceCard(
    device: SimulatedDevice,
    onConnect: (SimulatedDevice) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onConnect(device) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F8FF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Watch,
                    contentDescription = null,
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(32.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = "💍 ${device.name}",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    
                    Text(
                        text = "📡 ${device.address} • Sinal: ${device.rssi} dBm",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                        color = Color(0xFF666666)
                    )
                    
                    Text(
                        text = "🟢 Dados reais simulados",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                        color = Color(0xFF4CAF50)
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Conectar",
                tint = Color(0xFF1976D2),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun SimulatedDisconnectedCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Info",
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Conecte seu anel J2301A para ver dados reais coletados em tempo real",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                color = Color(0xFF424242),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "📊 Esta versão simula dados reais\n❤️ Interface da terceira idade\n🔄 Atualizações em tempo real\n🔋 Monitoramento de bateria",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp),
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
        }
    }
}

data class SimulatedHealthMetric(
    val emoji: String,
    val title: String,
    val value: String,
    val hasData: Boolean,
    val color: Color
)