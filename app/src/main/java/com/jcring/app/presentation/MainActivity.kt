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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.jcring.app.data.bluetooth.RealBluetoothManager
import com.jcring.app.presentation.screens.*
import com.jcring.app.presentation.theme.JCRingAppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private lateinit var bluetoothManager: RealBluetoothManager
    
    // Navigation states
    private val _currentScreen = mutableStateOf<Screen>(Screen.Home)
    private val currentScreen: State<Screen> = _currentScreen
    
    // Bluetooth setup
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
        
        // Initialize Bluetooth Manager
        bluetoothManager = RealBluetoothManager.getInstance(this)
        bluetoothManager.initialize()
        
        checkAndRequestPermissions()
        
        setContent {
            JCRingAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppContent()
                }
            }
        }
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MainAppContent() {
        val connectionState by bluetoothManager.isConnected.collectAsState()
        val deviceName by bluetoothManager.deviceName.collectAsState()
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            text = if (connectionState) "JC Ring - $deviceName" else "JC Ring Health",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        // Connection status indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Icon(
                                imageVector = if (connectionState) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                                contentDescription = "Connection Status",
                                tint = if (connectionState) Color(0xFF4CAF50) else Color(0xFFE57373),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (connectionState) "Conectado" else "Desconectado",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (connectionState) Color(0xFF4CAF50) else Color(0xFFE57373)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Início") },
                        label = { Text("Início") },
                        selected = currentScreen.value == Screen.Home,
                        onClick = { _currentScreen.value = Screen.Home }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Favorite, contentDescription = "Saúde") },
                        label = { Text("Saúde") },
                        selected = currentScreen.value == Screen.Health,
                        onClick = { _currentScreen.value = Screen.Health }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Analytics, contentDescription = "Histórico") },
                        label = { Text("Histórico") },
                        selected = currentScreen.value == Screen.History,
                        onClick = { _currentScreen.value = Screen.History }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.FitnessCenter, contentDescription = "Exercícios") },
                        label = { Text("Exercícios") },
                        selected = currentScreen.value == Screen.Exercise,
                        onClick = { _currentScreen.value = Screen.Exercise }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Config") },
                        label = { Text("Config") },
                        selected = currentScreen.value == Screen.Settings,
                        onClick = { _currentScreen.value = Screen.Settings }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentScreen.value) {
                    Screen.Home -> HomeScreen(bluetoothManager)
                    Screen.Health -> HealthScreen(bluetoothManager)
                    Screen.History -> HistoryScreen(bluetoothManager)
                    Screen.Exercise -> ExerciseScreen(bluetoothManager)
                    Screen.Settings -> SettingsScreen(bluetoothManager)
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

// Screen navigation enum
sealed class Screen {
    object Home : Screen()
    object Health : Screen()
    object History : Screen()
    object Exercise : Screen()
    object Settings : Screen()
}