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
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.jcring.app.data.bluetooth.RealBluetoothManager
import com.jcring.app.data.bluetooth.BluetoothDevice
import com.jcring.app.presentation.theme.JCRingAppTheme
import kotlinx.coroutines.*

class SeniorHealthRealActivity : ComponentActivity() {
    
    private lateinit var bluetoothManager: RealBluetoothManager
    
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
        
        bluetoothManager = RealBluetoothManager.getInstance(this)
        bluetoothManager.initialize()
        
        checkAndRequestPermissions()
        
        setContent {
            JCRingAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SeniorHealthRealMainScreen(bluetoothManager)
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

@Composable
fun SeniorHealthRealMainScreen(bluetoothManager: RealBluetoothManager) {
    var showConnectionScreen by remember { mutableStateOf(false) }
    
    val isConnected by bluetoothManager.isConnected.collectAsState()
    val heartRate by bluetoothManager.heartRate.collectAsState()
    val spO2 by bluetoothManager.spO2.collectAsState()
    val temperature by bluetoothManager.temperature.collectAsState()
    val steps by bluetoothManager.steps.collectAsState()
    val calories by bluetoothManager.calories.collectAsState()
    val batteryLevel by bluetoothManager.batteryLevel.collectAsState()
    val deviceName by bluetoothManager.deviceName.collectAsState()
    
    if (showConnectionScreen) {
        ConnectionScreen(
            bluetoothManager = bluetoothManager,
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
            HeaderCard()
            
            // Status de Conexão
            ConnectionStatusCard(
                isConnected = isConnected,
                deviceName = deviceName,
                batteryLevel = batteryLevel,
                onConnectionClick = {
                    if (isConnected) {
                        bluetoothManager.disconnect()
                    } else {
                        showConnectionScreen = true
                    }
                }
            )
            
            // Métricas de Saúde
            if (isConnected) {
                Text(
                    text = "📊 Dados de Saúde em Tempo Real",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
                
                RealHealthMetricsGrid(
                    heartRate = heartRate,
                    spO2 = spO2,
                    temperature = temperature,
                    steps = steps,
                    calories = calories,
                    bluetoothManager = bluetoothManager
                )
            } else {
                DisconnectedCard()
            }
        }
    }
}

@Composable
fun HeaderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "💍 Anel Inteligente J2301A",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Monitoramento Real de Saúde • Terceira Idade",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                color = Color(0xFF424242),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ConnectionStatusCard(
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
                        text = if (isConnected) "🔋 Bateria: $batteryLevel% • Dados reais" else "Toque para buscar dispositivos",
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
fun RealHealthMetricsGrid(
    heartRate: Int?,
    spO2: Int?,
    temperature: Float?,
    steps: Int,
    calories: Int,
    bluetoothManager: RealBluetoothManager
) {
    val sleepData = bluetoothManager.getSleepData()
    
    val metrics = listOf(
        HealthMetric("❤️", "Batimentos", "${heartRate ?: "--"} bpm", heartRate != null),
        HealthMetric("🫁", "SpO2", "${spO2 ?: "--"}%", spO2 != null),
        HealthMetric("🌡️", "Temperatura", "${temperature?.let { "%.1f°C".format(it) } ?: "--"}", temperature != null),
        HealthMetric("🚶", "Passos", "$steps", true),
        HealthMetric("🔥", "Calorias", "$calories kcal", true),
        HealthMetric("💤", "Sono", "${sleepData.first}h ${sleepData.second}m", true)
    )
    
    metrics.chunked(2).forEach { rowMetrics ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rowMetrics.forEach { metric ->
                RealHealthMetricCard(
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
            onClick = { bluetoothManager.startHeartRateMeasurement { } },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("❤️ Medir FC", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        
        Button(
            onClick = { bluetoothManager.startSpO2Measurement { } },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("🫁 Medir SpO2", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun RealHealthMetricCard(
    metric: HealthMetric,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (metric.hasData) Color(0xFFF5F5F5) else Color(0xFFFFEBEE)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (metric.hasData) 4.dp else 2.dp
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
                color = if (metric.hasData) Color(0xFF1976D2) else Color(0xFF999999),
                textAlign = TextAlign.Center
            )
            
            Text(
                text = metric.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
            
            if (!metric.hasData) {
                Text(
                    text = "Aguardando...",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                    color = Color(0xFF999999),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ConnectionScreen(
    bluetoothManager: RealBluetoothManager,
    onBackClick: () -> Unit
) {
    var devices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Pronto para buscar dispositivos") }
    
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
                        text = "🔍 Buscar Anel J2301A",
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
                    statusMessage = "Buscando dispositivos..."
                    devices = emptyList()
                    
                    bluetoothManager.startScan { foundDevices ->
                        devices = foundDevices
                        isScanning = false
                        statusMessage = if (foundDevices.isEmpty()) {
                            "Nenhum anel encontrado. Certifique-se que está ligado e próximo."
                        } else {
                            "Encontrados ${foundDevices.size} dispositivos. Toque para conectar:"
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
                Text("Buscar Dispositivos", fontSize = 20.sp, fontWeight = FontWeight.Medium)
            }
        }
        
        // Lista de dispositivos
        if (devices.isNotEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(devices) { device ->
                    DeviceCard(
                        device = device,
                        onConnect = { selectedDevice ->
                            statusMessage = "Conectando com ${selectedDevice.name}..."
                            bluetoothManager.connectToDevice(selectedDevice) { success ->
                                if (success) {
                                    statusMessage = "Conectado com sucesso!"
                                    // Auto-voltar após conexão bem-sucedida
                                    CoroutineScope(Dispatchers.Main).launch {
                                        delay(1000)
                                        onBackClick()
                                    }
                                } else {
                                    statusMessage = "Falha na conexão. Tente novamente."
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
fun DeviceCard(
    device: BluetoothDevice,
    onConnect: (BluetoothDevice) -> Unit
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
                        text = device.name ?: "Dispositivo Desconhecido",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    
                    Text(
                        text = device.address,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                        color = Color(0xFF666666)
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
fun DisconnectedCard() {
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
                text = "Conecte seu anel inteligente J2301A para começar a monitorar sua saúde em tempo real",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                color = Color(0xFF424242),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "• Dados coletados em tempo real\n• Medições precisas do anel\n• Interface adaptada para idosos",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp),
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
        }
    }
}

data class HealthMetric(
    val emoji: String,
    val title: String,
    val value: String,
    val hasData: Boolean
)