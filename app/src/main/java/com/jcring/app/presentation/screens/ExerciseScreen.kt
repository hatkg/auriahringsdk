package com.jcring.app.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExerciseScreen(bluetoothManager: RealBluetoothManager) {
    val isConnected by bluetoothManager.isConnected.collectAsState()
    var selectedCategory by remember { mutableStateOf(ExerciseCategory.POPULAR) }
    var activeExercise by remember { mutableStateOf<ExerciseMode?>(null) }
    var exerciseTimer by remember { mutableStateOf(0) }
    var isExerciseActive by remember { mutableStateOf(false) }
    
    // Timer effect for active exercise
    LaunchedEffect(isExerciseActive) {
        while (isExerciseActive) {
            delay(1000)
            exerciseTimer++
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
                text = "Modos de Exercício",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        if (!isConnected) {
            item {
                DisconnectedExerciseCard()
            }
        } else {
            // Active exercise card
            if (activeExercise != null) {
                item {
                    ActiveExerciseCard(
                        exercise = activeExercise!!,
                        timer = exerciseTimer,
                        isActive = isExerciseActive,
                        onStart = { isExerciseActive = true },
                        onPause = { isExerciseActive = false },
                        onStop = {
                            isExerciseActive = false
                            activeExercise = null
                            exerciseTimer = 0
                        }
                    )
                }
            }
            
            // Category selector
            item {
                ExerciseCategorySelector(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )
            }
            
            // Exercise modes grid
            item {
                ExerciseModesGrid(
                    category = selectedCategory,
                    onExerciseSelected = { exercise ->
                        activeExercise = exercise
                        exerciseTimer = 0
                        isExerciseActive = false
                    }
                )
            }
            
            // Recent exercise history
            item {
                RecentExerciseHistory()
            }
        }
    }
}

@Composable
private fun DisconnectedExerciseCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = "Exercise",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Conecte o Anel J2301B",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "Para acessar os modos de exercício e monitoramento em tempo real",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun ActiveExerciseCard(
    exercise: ExerciseMode,
    timer: Int,
    isActive: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = exercise.color.copy(alpha = 0.1f))
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
                            .background(exercise.color, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = exercise.icon,
                            contentDescription = exercise.name,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isActive) "Exercitando" else "Pausado",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isActive) Color(0xFF4CAF50) else Color(0xFFFF9800)
                        )
                    }
                }
                
                // Timer display
                Text(
                    text = formatTime(timer),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = exercise.color
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Exercise metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ExerciseMetric("❤️", "BPM", "75") // Could be real-time from bluetoothManager
                ExerciseMetric("🔥", "Cal", "${timer * 5}") // Rough estimate
                ExerciseMetric("👟", "Passos", "${timer * 2}") // Rough estimate
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isActive) {
                    Button(
                        onClick = onPause,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pausar")
                    }
                } else {
                    Button(
                        onClick = onStart,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Iniciar")
                    }
                }
                
                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Parar")
                }
            }
        }
    }
}

@Composable
private fun ExerciseMetric(emoji: String, label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = emoji, fontSize = 20.sp)
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseCategorySelector(
    selectedCategory: ExerciseCategory,
    onCategorySelected: (ExerciseCategory) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Categorias",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExerciseCategory.values().forEach { category ->
                    FilterChip(
                        onClick = { onCategorySelected(category) },
                        label = { Text(category.displayName) },
                        selected = selectedCategory == category,
                        leadingIcon = {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = category.displayName,
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
private fun ExerciseModesGrid(
    category: ExerciseCategory,
    onExerciseSelected: (ExerciseMode) -> Unit
) {
    val exercises = getExercisesByCategory(category)
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "${category.displayName} (${exercises.size} exercícios)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(300.dp) // Fixed height for grid
            ) {
                items(exercises) { exercise ->
                    ExerciseModeCard(
                        exercise = exercise,
                        onClick = { onExerciseSelected(exercise) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseModeCard(
    exercise: ExerciseMode,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = exercise.color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(exercise.color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = exercise.icon,
                    contentDescription = exercise.name,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun RecentExerciseHistory() {
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
                    text = "Exercícios Recentes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                TextButton(onClick = { /* Navigate to full history */ }) {
                    Text("Ver Todos")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mock recent exercise data
            val recentExercises = listOf(
                RecentExercise("Caminhada", "25 min", "120 cal", "Hoje"),
                RecentExercise("Corrida", "18 min", "180 cal", "Ontem"),
                RecentExercise("Ciclismo", "35 min", "250 cal", "2 dias atrás")
            )
            
            recentExercises.forEach { exercise ->
                RecentExerciseItem(exercise)
                if (exercise != recentExercises.last()) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun RecentExerciseItem(exercise: RecentExercise) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = exercise.date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = exercise.duration,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = exercise.calories,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFF5722)
            )
        }
    }
}

// Data classes and enums
enum class ExerciseCategory(val displayName: String, val icon: ImageVector) {
    POPULAR("Populares", Icons.Default.TrendingUp),
    CARDIO("Cardio", Icons.Default.Favorite),
    STRENGTH("Força", Icons.Default.FitnessCenter),
    OUTDOOR("Outdoor", Icons.Default.Terrain),
    SPORTS("Esportes", Icons.Default.SportsBaseball),
    WELLNESS("Bem-estar", Icons.Default.Spa)
}

data class ExerciseMode(
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val category: ExerciseCategory
)

data class RecentExercise(
    val name: String,
    val duration: String,
    val calories: String,
    val date: String
)

// Helper functions
private fun getExercisesByCategory(category: ExerciseCategory): List<ExerciseMode> {
    val allExercises = listOf(
        // Popular
        ExerciseMode("Caminhada", Icons.Default.DirectionsWalk, Color(0xFF4CAF50), ExerciseCategory.POPULAR),
        ExerciseMode("Corrida", Icons.Default.DirectionsRun, Color(0xFFFF5722), ExerciseCategory.POPULAR),
        ExerciseMode("Ciclismo", Icons.Default.DirectionsBike, Color(0xFF2196F3), ExerciseCategory.POPULAR),
        ExerciseMode("Natação", Icons.Default.Pool, Color(0xFF00BCD4), ExerciseCategory.POPULAR),
        
        // Cardio
        ExerciseMode("Corrida", Icons.Default.DirectionsRun, Color(0xFFFF5722), ExerciseCategory.CARDIO),
        ExerciseMode("Ciclismo", Icons.Default.DirectionsBike, Color(0xFF2196F3), ExerciseCategory.CARDIO),
        ExerciseMode("Pular Corda", Icons.Default.FitnessCenter, Color(0xFFE91E63), ExerciseCategory.CARDIO),
        ExerciseMode("Dança", Icons.Default.MusicNote, Color(0xFF9C27B0), ExerciseCategory.CARDIO),
        ExerciseMode("Aeróbica", Icons.Default.FitnessCenter, Color(0xFFFF9800), ExerciseCategory.CARDIO),
        ExerciseMode("Spinning", Icons.Default.DirectionsBike, Color(0xFF673AB7), ExerciseCategory.CARDIO),
        
        // Strength
        ExerciseMode("Musculação", Icons.Default.FitnessCenter, Color(0xFF795548), ExerciseCategory.STRENGTH),
        ExerciseMode("Flexões", Icons.Default.FitnessCenter, Color(0xFF607D8B), ExerciseCategory.STRENGTH),
        ExerciseMode("Abdominais", Icons.Default.FitnessCenter, Color(0xFF3F51B5), ExerciseCategory.STRENGTH),
        ExerciseMode("Levantamento", Icons.Default.FitnessCenter, Color(0xFF9E9E9E), ExerciseCategory.STRENGTH),
        ExerciseMode("Crossfit", Icons.Default.FitnessCenter, Color(0xFFFF5722), ExerciseCategory.STRENGTH),
        
        // Outdoor
        ExerciseMode("Caminhada", Icons.Default.DirectionsWalk, Color(0xFF4CAF50), ExerciseCategory.OUTDOOR),
        ExerciseMode("Trilha", Icons.Default.Terrain, Color(0xFF8BC34A), ExerciseCategory.OUTDOOR),
        ExerciseMode("Escalada", Icons.Default.Terrain, Color(0xFF795548), ExerciseCategory.OUTDOOR),
        ExerciseMode("Surf", Icons.Default.Pool, Color(0xFF00BCD4), ExerciseCategory.OUTDOOR),
        ExerciseMode("Esqui", Icons.Default.AcUnit, Color(0xFF03DAC5), ExerciseCategory.OUTDOOR),
        ExerciseMode("Canoagem", Icons.Default.Pool, Color(0xFF2196F3), ExerciseCategory.OUTDOOR),
        
        // Sports
        ExerciseMode("Futebol", Icons.Default.SportsBaseball, Color(0xFF4CAF50), ExerciseCategory.SPORTS),
        ExerciseMode("Basquete", Icons.Default.SportsBasketball, Color(0xFFFF9800), ExerciseCategory.SPORTS),
        ExerciseMode("Tênis", Icons.Default.SportsTennis, Color(0xFFE91E63), ExerciseCategory.SPORTS),
        ExerciseMode("Vôlei", Icons.Default.SportsVolleyball, Color(0xFF2196F3), ExerciseCategory.SPORTS),
        ExerciseMode("Ping Pong", Icons.Default.SportsTennis, Color(0xFF9C27B0), ExerciseCategory.SPORTS),
        ExerciseMode("Badminton", Icons.Default.SportsTennis, Color(0xFF00BCD4), ExerciseCategory.SPORTS),
        ExerciseMode("Golfe", Icons.Default.SportsGolf, Color(0xFF4CAF50), ExerciseCategory.SPORTS),
        ExerciseMode("Beisebol", Icons.Default.SportsBaseball, Color(0xFFFF5722), ExerciseCategory.SPORTS),
        
        // Wellness
        ExerciseMode("Yoga", Icons.Default.Spa, Color(0xFF9C27B0), ExerciseCategory.WELLNESS),
        ExerciseMode("Pilates", Icons.Default.Spa, Color(0xFF673AB7), ExerciseCategory.WELLNESS),
        ExerciseMode("Meditação", Icons.Default.SelfImprovement, Color(0xFF3F51B5), ExerciseCategory.WELLNESS),
        ExerciseMode("Tai Chi", Icons.Default.Spa, Color(0xFF00695C), ExerciseCategory.WELLNESS),
        ExerciseMode("Alongamento", Icons.Default.SelfImprovement, Color(0xFF8BC34A), ExerciseCategory.WELLNESS)
    )
    
    return allExercises.filter { it.category == category }
}

private fun formatTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    
    return if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, secs)
    } else {
        "%02d:%02d".format(minutes, secs)
    }
}