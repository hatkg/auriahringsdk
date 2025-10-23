package com.jcring.app.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.jcring.app.data.bluetooth.RealBluetoothManager
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GlucoseScreen(bluetoothManager: RealBluetoothManager) {
    val isConnected by bluetoothManager.isConnected.collectAsState()
    val glucose by bluetoothManager.glucose.collectAsState()
    
    var isMeasuring by remember { mutableStateOf(false) }
    var measurementTimer by remember { mutableStateOf(0) }
    
    // Measurement timer
    LaunchedEffect(isMeasuring) {
        if (isMeasuring) {
            while (measurementTimer < 30 && isMeasuring) { // 30 second measurement
                delay(1000)
                measurementTimer++
            }
            if (isMeasuring) {
                isMeasuring = false
                measurementTimer = 0
            }
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Glicose",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (isConnected) {
                    Text(
                        text = "J2301B Conectado",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
        
        if (!isConnected) {
            item {
                DisconnectedGlucoseCard()
            }
        } else {
            // Current glucose reading
            item {
                CurrentGlucoseCard(
                    glucose = glucose,
                    isMeasuring = isMeasuring,
                    measurementTimer = measurementTimer,
                    onStartMeasurement = {
                        isMeasuring = true
                        measurementTimer = 0
                        bluetoothManager.measureGlucose { success ->
                            if (!success) {
                                isMeasuring = false
                                measurementTimer = 0
                            }
                        }
                    },
                    onStopMeasurement = {
                        isMeasuring = false
                        measurementTimer = 0
                    }
                )
            }
            
            // Glucose information
            item {
                GlucoseInfoCard()
            }
            
            // Glucose ranges
            item {
                GlucoseRangesCard()
            }
            
            // Recent measurements
            item {
                RecentGlucoseMeasurements()
            }
            
            // Tips and recommendations
            item {
                GlucoseTipsCard()
            }
        }
    }
}

@Composable
private fun DisconnectedGlucoseCard() {
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
                text = "Conecte o anel para medir a glicose via sensor PPG",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun CurrentGlucoseCard(
    glucose: Float?,
    isMeasuring: Boolean,
    measurementTimer: Int,
    onStartMeasurement: () -> Unit,
    onStopMeasurement: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF795548).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
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
                            .background(Color(0xFF795548), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bloodtype,
                            contentDescription = "Glucose",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Glicose Estimada",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = getGlucoseStatus(glucose),
                            style = MaterialTheme.typography.bodyMedium,
                            color = getGlucoseStatusColor(glucose)
                        )
                    }
                }
                
                Text(
                    text = "Última: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Glucose value display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFF795548).copy(alpha = 0.1f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isMeasuring) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            progress = measurementTimer / 30f,
                            modifier = Modifier.size(80.dp),
                            color = Color(0xFF795548),
                            strokeWidth = 8.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Medindo...",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${30 - measurementTimer}s restantes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = glucose?.let { "%.0f".format(it) } ?: "--",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF795548)
                        )
                        Text(
                            text = "mg/dL",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF795548)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Control buttons
            if (isMeasuring) {
                Button(
                    onClick = onStopMeasurement,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Parar Medição")
                }
            } else {
                Button(
                    onClick = onStartMeasurement,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF795548))
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Medir Glicose")
                }
            }
            
            // Important note
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Valores estimativos via sensor PPG. Não substitui exames laboratoriais.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE65100)
                    )
                }
            }
        }
    }
}

@Composable
private fun GlucoseInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Como Funciona",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "O anel J2301B utiliza tecnologia PPG (fotopletismografia) para estimar os níveis de glicose através da análise do fluxo sanguíneo. Esta é uma estimativa não invasiva que pode ajudar no monitoramento, mas não substitui testes de glicemia tradicionais.",
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun GlucoseRangesCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Faixas de Referência",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val ranges = listOf(
                GlucoseRange("Normal (Jejum)", "70-99 mg/dL", Color(0xFF4CAF50)),
                GlucoseRange("Pré-diabetes", "100-125 mg/dL", Color(0xFFFF9800)),
                GlucoseRange("Diabetes", "≥126 mg/dL", Color(0xFFFF5722)),
                GlucoseRange("Hipoglicemia", "<70 mg/dL", Color(0xFFE91E63))
            )
            
            ranges.forEach { range ->
                GlucoseRangeItem(range)
                if (range != ranges.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun GlucoseRangeItem(range: GlucoseRange) {
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
                    .size(12.dp)
                    .background(range.color, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = range.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        
        Text(
            text = range.range,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RecentGlucoseMeasurements() {
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
                Text(
                    text = "Medições Recentes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                TextButton(onClick = { /* Navigate to full history */ }) {
                    Text("Ver Histórico")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mock recent measurements
            val recentMeasurements = listOf(
                GlucoseMeasurement("95 mg/dL", "Hoje, 08:30", "Normal"),
                GlucoseMeasurement("102 mg/dL", "Ontem, 14:15", "Levemente elevado"),
                GlucoseMeasurement("88 mg/dL", "Ontem, 08:45", "Normal"),
                GlucoseMeasurement("110 mg/dL", "2 dias, 20:30", "Elevado")
            )
            
            recentMeasurements.forEach { measurement ->
                RecentGlucoseItem(measurement)
                if (measurement != recentMeasurements.last()) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun RecentGlucoseItem(measurement: GlucoseMeasurement) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = measurement.value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = measurement.time,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = measurement.status,
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                measurement.status.contains("Normal") -> Color(0xFF4CAF50)
                measurement.status.contains("elevado") -> Color(0xFFFF9800)
                else -> Color(0xFFFF5722)
            }
        )
    }
}

@Composable
private fun GlucoseTipsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Tips",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Dicas para Controle da Glicose",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2E7D32)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val tips = listOf(
                "🥗 Mantenha uma dieta equilibrada",
                "🚶 Pratique exercícios regularmente",
                "💧 Beba bastante água",
                "😴 Durma adequadamente (7-9 horas)",
                "🧘 Gerencie o estresse",
                "📊 Monitore regularmente"
            )
            
            tips.forEach { tip ->
                Text(
                    text = tip,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

// Data classes
data class GlucoseRange(
    val label: String,
    val range: String,
    val color: Color
)

data class GlucoseMeasurement(
    val value: String,
    val time: String,
    val status: String
)

// Helper functions
private fun getGlucoseStatus(glucose: Float?): String {
    return when {
        glucose == null -> "Não medido"
        glucose < 70 -> "Hipoglicemia"
        glucose <= 99 -> "Normal"
        glucose <= 125 -> "Pré-diabetes"
        else -> "Diabético"
    }
}

private fun getGlucoseStatusColor(glucose: Float?): Color {
    return when {
        glucose == null -> Color(0xFF9E9E9E)
        glucose < 70 -> Color(0xFFE91E63)
        glucose <= 99 -> Color(0xFF4CAF50)
        glucose <= 125 -> Color(0xFFFF9800)
        else -> Color(0xFFFF5722)
    }
}