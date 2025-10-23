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
import com.jcring.app.data.bluetooth.RealBluetoothManager
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(bluetoothManager: RealBluetoothManager) {
    val isConnected by bluetoothManager.isConnected.collectAsState()
    var selectedPeriod by remember { mutableStateOf(TimePeriod.TODAY) }
    var selectedMetric by remember { mutableStateOf(HealthMetricType.HEART_RATE) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Histórico & Análises",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        if (!isConnected) {
            item {
                DisconnectedHistoryCard()
            }
        } else {
            // Period selector
            item {
                PeriodSelector(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = { selectedPeriod = it }
                )
            }
            
            // Summary statistics for selected period
            item {
                PeriodSummaryCard(selectedPeriod, bluetoothManager)
            }
            
            // Metric selector
            item {
                MetricSelector(
                    selectedMetric = selectedMetric,
                    onMetricSelected = { selectedMetric = it }
                )
            }
            
            // Detailed metric analysis
            item {
                MetricAnalysisCard(selectedMetric, selectedPeriod, bluetoothManager)
            }
            
            // Trends and insights
            item {
                TrendsCard(selectedPeriod, bluetoothManager)
            }
            
            // Goals and achievements
            item {
                GoalsCard(selectedPeriod, bluetoothManager)
            }
            
            // Export options
            item {
                ExportOptionsCard()
            }
        }
    }
}

@Composable
private fun DisconnectedHistoryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.HistoryToggleOff,
                contentDescription = "No History",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Histórico Indisponível",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "Conecte o anel J2301B para visualizar histórico e análises de saúde",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodSelector(
    selectedPeriod: TimePeriod,
    onPeriodSelected: (TimePeriod) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Período",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(TimePeriod.values()) { period ->
                    FilterChip(
                        onClick = { onPeriodSelected(period) },
                        label = { Text(period.displayName) },
                        selected = selectedPeriod == period,
                        leadingIcon = {
                            Icon(
                                imageVector = period.icon,
                                contentDescription = period.displayName,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodSummaryCard(period: TimePeriod, bluetoothManager: RealBluetoothManager) {
    val heartRate by bluetoothManager.heartRate.collectAsState()
    val spO2 by bluetoothManager.spO2.collectAsState()
    val steps by bluetoothManager.steps.collectAsState()
    val calories by bluetoothManager.calories.collectAsState()
    
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
                    text = "Resumo - ${period.displayName}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "Analytics",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryMetric(
                    icon = Icons.Default.Favorite,
                    label = "FC Média",
                    value = "${heartRate ?: "--"} bpm",
                    color = Color(0xFFE91E63)
                )
                SummaryMetric(
                    icon = Icons.Default.Air,
                    label = "SpO2 Média",
                    value = "${spO2 ?: "--"}%",
                    color = Color(0xFF2196F3)
                )
                SummaryMetric(
                    icon = Icons.Default.DirectionsWalk,
                    label = "Passos",
                    value = steps.toString(),
                    color = Color(0xFF4CAF50)
                )
                SummaryMetric(
                    icon = Icons.Default.LocalFireDepartment,
                    label = "Calorias",
                    value = "${calories} kcal",
                    color = Color(0xFFFF5722)
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
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
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
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
private fun MetricSelector(
    selectedMetric: HealthMetricType,
    onMetricSelected: (HealthMetricType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Métrica Detalhada",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(HealthMetricType.values()) { metric ->
                    FilterChip(
                        onClick = { onMetricSelected(metric) },
                        label = { Text(metric.displayName) },
                        selected = selectedMetric == metric,
                        leadingIcon = {
                            Icon(
                                imageVector = metric.icon,
                                contentDescription = metric.displayName,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricAnalysisCard(
    metric: HealthMetricType,
    period: TimePeriod,
    bluetoothManager: RealBluetoothManager
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
                Text(
                    text = "${metric.displayName} - Análise",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(metric.color.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = metric.icon,
                        contentDescription = metric.displayName,
                        tint = metric.color,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Mock analysis data
            val analysisData = getMetricAnalysis(metric, bluetoothManager)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AnalysisItem("Máximo", analysisData.max, metric.color)
                AnalysisItem("Média", analysisData.average, metric.color)
                AnalysisItem("Mínimo", analysisData.min, metric.color)
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Chart placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Chart",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Gráfico de ${metric.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Insights
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = metric.color.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Insight",
                            tint = metric.color,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Insight",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = metric.color
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = getMetricInsight(metric, analysisData),
                        style = MaterialTheme.typography.bodyMedium,
                        color = metric.color.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalysisItem(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TrendsCard(period: TimePeriod, bluetoothManager: RealBluetoothManager) {
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
                    text = "Tendências",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = "Trends",
                    tint = Color(0xFF4CAF50)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val trends = listOf(
                TrendItem("Frequência Cardíaca", "↗️ Melhoria de 3% esta semana", Color(0xFF4CAF50)),
                TrendItem("Atividade Física", "📈 +15% passos comparado à semana passada", Color(0xFF2196F3)),
                TrendItem("Qualidade do Sono", "💤 Sono profundo melhorou 12 min/noite", Color(0xFF9C27B0)),
                TrendItem("SpO2", "→ Mantendo níveis estáveis (95-98%)", Color(0xFFFF9800))
            )
            
            trends.forEach { trend ->
                TrendItemCard(trend)
                if (trend != trends.last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun TrendItemCard(trend: TrendItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(trend.color, CircleShape)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = trend.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = trend.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GoalsCard(period: TimePeriod, bluetoothManager: RealBluetoothManager) {
    val steps by bluetoothManager.steps.collectAsState()
    val calories by bluetoothManager.calories.collectAsState()
    
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
                    text = "Metas e Conquistas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Goals",
                    tint = Color(0xFFFFD700)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Daily goals
            GoalProgress(
                title = "Meta de Passos",
                current = steps,
                target = 8000,
                unit = "passos",
                icon = Icons.Default.DirectionsWalk,
                color = Color(0xFF4CAF50)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            GoalProgress(
                title = "Meta de Calorias",
                current = calories,
                target = 1800,
                unit = "kcal",
                icon = Icons.Default.LocalFireDepartment,
                color = Color(0xFFFF5722)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Achievements
            Text(
                text = "Conquistas Recentes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val achievements = listOf(
                "🏆 7 dias consecutivos de meta de passos",
                "💪 Melhor semana de atividade física",
                "❤️ FC em repouso melhorou 5 bpm"
            )
            
            achievements.forEach { achievement ->
                Text(
                    text = achievement,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun GoalProgress(
    title: String,
    current: Int,
    target: Int,
    unit: String,
    icon: ImageVector,
    color: Color
) {
    val progress = (current.toFloat() / target).coerceIn(0f, 1f)
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Text(
                text = "$current / $target $unit",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth(),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun ExportOptionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Exportar Dados",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { /* Export PDF */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PDF")
                }
                
                OutlinedButton(
                    onClick = { /* Export CSV */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CSV")
                }
                
                OutlinedButton(
                    onClick = { /* Share */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compartilhar")
                }
            }
        }
    }
}

// Data classes and enums
enum class TimePeriod(val displayName: String, val icon: ImageVector) {
    TODAY("Hoje", Icons.Default.Today),
    WEEK("7 Dias", Icons.Default.DateRange),
    MONTH("30 Dias", Icons.Default.CalendarMonth),
    YEAR("1 Ano", Icons.Default.CalendarToday)
}

enum class HealthMetricType(
    val displayName: String,
    val icon: ImageVector,
    val color: Color
) {
    HEART_RATE("Freq. Cardíaca", Icons.Default.Favorite, Color(0xFFE91E63)),
    SPO2("SpO2", Icons.Default.Air, Color(0xFF2196F3)),
    TEMPERATURE("Temperatura", Icons.Default.Thermostat, Color(0xFFFF9800)),
    GLUCOSE("Glicose", Icons.Default.Bloodtype, Color(0xFF795548)),
    STRESS("Estresse", Icons.Default.Psychology, Color(0xFF9C27B0)),
    ACTIVITY("Atividade", Icons.Default.DirectionsWalk, Color(0xFF4CAF50))
}

data class MetricAnalysis(
    val max: String,
    val average: String,
    val min: String
)

data class TrendItem(
    val title: String,
    val description: String,
    val color: Color
)

// Helper functions
private fun getMetricAnalysis(metric: HealthMetricType, bluetoothManager: RealBluetoothManager): MetricAnalysis {
    return when (metric) {
        HealthMetricType.HEART_RATE -> {
            val current = bluetoothManager.heartRate.value ?: 0
            MetricAnalysis("${current + 15} bpm", "$current bpm", "${current - 10} bpm")
        }
        HealthMetricType.SPO2 -> {
            val current = bluetoothManager.spO2.value ?: 0
            MetricAnalysis("${current + 2}%", "$current%", "${current - 1}%")
        }
        HealthMetricType.TEMPERATURE -> {
            val current = bluetoothManager.temperature.value ?: 36.5f
            MetricAnalysis("%.1f°C".format(current + 0.5f), "%.1f°C".format(current), "%.1f°C".format(current - 0.3f))
        }
        HealthMetricType.GLUCOSE -> {
            val current = bluetoothManager.glucose.value ?: 95f
            MetricAnalysis("%.0f mg/dL".format(current + 15), "%.0f mg/dL".format(current), "%.0f mg/dL".format(current - 8))
        }
        HealthMetricType.STRESS -> {
            val current = bluetoothManager.stress.value ?: 45
            MetricAnalysis("${current + 25}", "$current", "${current - 15}")
        }
        HealthMetricType.ACTIVITY -> {
            val current = bluetoothManager.steps.value
            MetricAnalysis("${current + 2000}", "$current", "${current - 1000}")
        }
    }
}

private fun getMetricInsight(metric: HealthMetricType, analysis: MetricAnalysis): String {
    return when (metric) {
        HealthMetricType.HEART_RATE -> "Sua frequência cardíaca está dentro da faixa normal para sua idade. Continue mantendo atividade física regular."
        HealthMetricType.SPO2 -> "Excelente saturação de oxigênio! Seus pulmões estão funcionando muito bem."
        HealthMetricType.TEMPERATURE -> "Temperatura corporal estável. Mantenha-se hidratado e em ambientes com temperatura adequada."
        HealthMetricType.GLUCOSE -> "Níveis de glicose estimados estão controlados. Mantenha dieta equilibrada e exercícios regulares."
        HealthMetricType.STRESS -> "Níveis de estresse moderados. Pratique técnicas de relaxamento e meditação."
        HealthMetricType.ACTIVITY -> "Boa atividade física! Tente manter ou aumentar gradualmente seus passos diários."
    }
}