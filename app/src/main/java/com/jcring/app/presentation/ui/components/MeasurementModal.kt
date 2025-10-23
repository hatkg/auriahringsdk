package com.jcring.app.presentation.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import android.os.VibrationEffect
import android.os.Vibrator
import android.content.Context

enum class MeasurementType(
    val displayName: String,
    val icon: ImageVector,
    val color: Color,
    val unit: String,
    val description: String
) {
    HEART_RATE("Frequência Cardíaca", Icons.Default.Favorite, Color(0xFFE91E63), "bpm", "Mantenha o dedo no sensor"),
    SPO2("Saturação de Oxigênio", Icons.Default.Air, Color(0xFF2196F3), "%", "Posicione o dedo corretamente"),
    TEMPERATURE("Temperatura", Icons.Default.Thermostat, Color(0xFFFF9800), "°C", "Aguarde a estabilização"),
    HRV("Variabilidade Cardíaca", Icons.Default.MonitorHeart, Color(0xFF9C27B0), "ms", "Mantenha-se relaxado"),
    STRESS("Nível de Stress", Icons.Default.Psychology, Color(0xFFFF5722), "/100", "Respire normalmente")
}

enum class MeasurementState {
    PREPARING, MEASURING, COMPLETED, ERROR
}

@Composable
fun MeasurementModal(
    measurementType: MeasurementType,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onStartMeasurement: () -> Unit,
    onStopMeasurement: () -> Unit,
    currentValue: String? = null,
    progress: Float = 0f,
    state: MeasurementState = MeasurementState.PREPARING
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    
    // Animações
    val infiniteTransition = rememberInfiniteTransition(label = "measurement")
    val pulseAnimation by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    val rotationAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "rotation"
    )
    
    // Vibração tátil quando inicia medição
    LaunchedEffect(state) {
        when (state) {
            MeasurementState.MEASURING -> {
                if (vibrator.hasVibrator()) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            }
            MeasurementState.COMPLETED -> {
                if (vibrator.hasVibrator()) {
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 100, 100), -1))
                }
            }
            MeasurementState.ERROR -> {
                if (vibrator.hasVibrator()) {
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1))
                }
            }
            else -> {}
        }
    }
    
    if (isVisible) {
        Dialog(
            onDismissRequest = { 
                onStopMeasurement()
                onDismiss() 
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Título
                        Text(
                            text = measurementType.displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Descrição
                        Text(
                            text = measurementType.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Círculo de medição animado
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(200.dp)
                        ) {
                            // Círculo de progresso
                            Canvas(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                val strokeWidth = 12.dp.toPx()
                                
                                // Círculo de fundo
                                drawCircle(
                                    color = measurementType.color.copy(alpha = 0.2f),
                                    radius = size.minDimension / 2 - strokeWidth / 2,
                                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                )
                                
                                // ANIMAÇÃO CONTÍNUA até dados chegarem
                                if (state == MeasurementState.MEASURING) {
                                    // Usar rotationAnimation para círculo infinito
                                    drawArc(
                                        color = measurementType.color,
                                        startAngle = rotationAnimation - 90f,
                                        sweepAngle = 120f, // Arco de 120 graus que gira infinitamente
                                        useCenter = false,
                                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                    )
                                }
                                
                                // Círculo completo quando concluído
                                if (state == MeasurementState.COMPLETED) {
                                    drawCircle(
                                        color = measurementType.color,
                                        radius = size.minDimension / 2 - strokeWidth / 2,
                                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                    )
                                }
                            }
                            
                            // Ícone central com animação
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(
                                        measurementType.color.copy(
                                            alpha = if (state == MeasurementState.MEASURING) 0.2f else 0.1f
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = measurementType.icon,
                                    contentDescription = measurementType.displayName,
                                    tint = measurementType.color,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .let { 
                                            if (state == MeasurementState.MEASURING) 
                                                it.graphicsLayer(scaleX = pulseAnimation, scaleY = pulseAnimation)
                                            else it
                                        }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Valor atual
                        if (currentValue != null && state == MeasurementState.COMPLETED) {
                            Text(
                                text = "$currentValue ${measurementType.unit}",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = measurementType.color
                            )
                        } else {
                            Text(
                                text = when (state) {
                                    MeasurementState.PREPARING -> "Toque para iniciar"
                                    MeasurementState.MEASURING -> "Medindo..."
                                    MeasurementState.COMPLETED -> "Concluído!"
                                    MeasurementState.ERROR -> "Erro na medição"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Botões de ação
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Botão Cancelar
                            OutlinedButton(
                                onClick = { 
                                    onStopMeasurement()
                                    onDismiss() 
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancelar")
                            }
                            
                            // Botão principal
                            Button(
                                onClick = {
                                    when (state) {
                                        MeasurementState.PREPARING -> onStartMeasurement()
                                        MeasurementState.MEASURING -> onStopMeasurement()
                                        MeasurementState.COMPLETED -> onDismiss()
                                        MeasurementState.ERROR -> onStartMeasurement()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = measurementType.color
                                )
                            ) {
                                Text(
                                    when (state) {
                                        MeasurementState.PREPARING -> "Iniciar"
                                        MeasurementState.MEASURING -> "Parar"
                                        MeasurementState.COMPLETED -> "OK"
                                        MeasurementState.ERROR -> "Tentar Novamente"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Preview para desenvolvimento
@Composable
fun MeasurementModalPreview() {
    MaterialTheme {
        MeasurementModal(
            measurementType = MeasurementType.HEART_RATE,
            isVisible = true,
            onDismiss = {},
            onStartMeasurement = {},
            onStopMeasurement = {},
            currentValue = "72",
            progress = 0.6f,
            state = MeasurementState.MEASURING
        )
    }
}