package com.jcring.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Cores adaptadas para terceira idade - mais contrastantes e suaves
private val SeniorLightColorScheme = lightColorScheme(
    primary = Color(0xFF2E7D32),           // Verde mais escuro e contrastante
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F5E8),  // Verde muito claro
    onPrimaryContainer = Color(0xFF1B5E20),
    
    secondary = Color(0xFF1976D2),          // Azul mais contrastante
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3F2FD),
    onSecondaryContainer = Color(0xFF0D47A1),
    
    tertiary = Color(0xFFD32F2F),           // Vermelho para alertas
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFEBEE),
    onTertiaryContainer = Color(0xFFB71C1C),
    
    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFFB71C1C),
    
    background = Color(0xFFFAFAFA),         // Fundo muito claro
    onBackground = Color(0xFF212121),       // Texto muito escuro
    surface = Color.White,
    onSurface = Color(0xFF212121),
    
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF424242),
    
    outline = Color(0xFF757575),
    outlineVariant = Color(0xFFBDBDBD),
    
    scrim = Color(0x80000000)
)

private val SeniorDarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),           // Verde mais claro para modo escuro
    onPrimary = Color(0xFF1B5E20),
    primaryContainer = Color(0xFF2E7D32),
    onPrimaryContainer = Color(0xFFE8F5E8),
    
    secondary = Color(0xFF64B5F6),         // Azul mais claro
    onSecondary = Color(0xFF0D47A1),
    secondaryContainer = Color(0xFF1976D2),
    onSecondaryContainer = Color(0xFFE3F2FD),
    
    tertiary = Color(0xFFEF5350),          // Vermelho mais claro
    onTertiary = Color(0xFFB71C1C),
    tertiaryContainer = Color(0xFFD32F2F),
    onTertiaryContainer = Color(0xFFFFEBEE),
    
    error = Color(0xFFEF5350),
    onError = Color(0xFFB71C1C),
    errorContainer = Color(0xFFD32F2F),
    onErrorContainer = Color(0xFFFFEBEE),
    
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    
    surfaceVariant = Color(0xFF424242),
    onSurfaceVariant = Color(0xFFE0E0E0),
    
    outline = Color(0xFF9E9E9E),
    outlineVariant = Color(0xFF616161)
)

// Tipografia adaptada para terceira idade - fontes maiores e mais legíveis
val SeniorTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 64.sp,              // Extra grande para títulos principais
        lineHeight = 72.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 52.sp,              // Grande para subtítulos
        lineHeight = 60.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,              // Médio-grande para seções
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,              // Cabeçalhos de páginas
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,              // Títulos de seções
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,              // Subtítulos de cards
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,              // Títulos de dados
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,              // Labels importantes
        lineHeight = 28.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,              // Labels secundários
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,              // Texto principal - maior
        lineHeight = 28.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,              // Texto secundário - maior
        lineHeight = 26.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,              // Texto pequeno - ainda legível
        lineHeight = 22.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,              // Botões principais
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,              // Botões secundários
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,              // Labels pequenos
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp
    )
)

// Cores específicas para dados de saúde
object HealthColors {
    val heartRate = Color(0xFFE91E63)      // Rosa para frequência cardíaca
    val spo2 = Color(0xFF2196F3)          // Azul para SpO2
    val temperature = Color(0xFFFF9800)    // Laranja para temperatura
    val steps = Color(0xFF4CAF50)         // Verde para passos
    val sleep = Color(0xFF9C27B0)         // Roxo para sono
    val stress = Color(0xFFFF5722)        // Vermelho para estresse
    val glucose = Color(0xFF795548)       // Marrom para glicose
    val hrv = Color(0xFF607D8B)           // Cinza-azul para HRV
    val exercise = Color(0xFFFF6F00)      // Laranja escuro para exercício
    
    // Estados dos valores
    val excellent = Color(0xFF4CAF50)     // Verde para excelente
    val good = Color(0xFF8BC34A)          // Verde claro para bom
    val normal = Color(0xFFFFC107)        // Amarelo para normal
    val warning = Color(0xFFFF9800)       // Laranja para atenção
    val critical = Color(0xFFF44336)      // Vermelho para crítico
    
    // Transparências para fundos
    val backgroundAlpha = 0.1f
    val cardAlpha = 0.05f
}

@Composable
fun SeniorHealthTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        SeniorDarkColorScheme
    } else {
        SeniorLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SeniorTypography,
        content = content
    )
}

// Extensões para facilitar o uso das cores de saúde
@Composable
fun Color.Companion.healthMetric(metric: String): Color {
    return when (metric.lowercase()) {
        "heartrate", "frequencia", "bpm" -> HealthColors.heartRate
        "spo2", "oxigenacao", "oxygen" -> HealthColors.spo2
        "temperature", "temperatura" -> HealthColors.temperature
        "steps", "passos" -> HealthColors.steps
        "sleep", "sono" -> HealthColors.sleep
        "stress", "estresse" -> HealthColors.stress
        "glucose", "glicose" -> HealthColors.glucose
        "hrv" -> HealthColors.hrv
        "exercise", "exercicio" -> HealthColors.exercise
        else -> MaterialTheme.colorScheme.primary
    }
}

@Composable
fun Color.Companion.healthStatus(value: Float, ranges: HealthRange): Color {
    return when {
        value >= ranges.excellent -> HealthColors.excellent
        value >= ranges.good -> HealthColors.good
        value >= ranges.normal -> HealthColors.normal
        value >= ranges.warning -> HealthColors.warning
        else -> HealthColors.critical
    }
}

data class HealthRange(
    val excellent: Float,
    val good: Float,
    val normal: Float,
    val warning: Float
)

// Ranges padrão para diferentes métricas de saúde
object HealthRanges {
    val heartRate = HealthRange(
        excellent = 90f,
        good = 80f,
        normal = 60f,
        warning = 50f
    )
    
    val spo2 = HealthRange(
        excellent = 98f,
        good = 95f,
        normal = 90f,
        warning = 85f
    )
    
    val temperature = HealthRange(
        excellent = 37.0f,
        good = 36.5f,
        normal = 36.0f,
        warning = 35.5f
    )
    
    val stepsPercentage = HealthRange(
        excellent = 100f,
        good = 80f,
        normal = 60f,
        warning = 40f
    )
}