package com.jcring.app.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.jcring.app.data.logger.LogManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import kotlin.collections.LinkedHashSet

/**
 * Manager BLE nativo seguindo exatamente o padrão do BleService.java original
 * Implementa a sequência correta: discoverServices → requestMtu → enableNotifications → startMeasurement
 */
@SuppressLint("MissingPermission")
class NativeBleManager private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: NativeBleManager? = null
        
        fun getInstance(context: Context): NativeBleManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NativeBleManager(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        // UUIDs corretos do J2301B (extraídos do BleService.java original)
        private val SERVICE_UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
        private val WRITE_CHARACTERISTIC_UUID = UUID.fromString("0000fff6-0000-1000-8000-00805f9b34fb")
        private val NOTIFY_CHARACTERISTIC_UUID = UUID.fromString("0000fff7-0000-1000-8000-00805f9b34fb")
        private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        
        private const val TAG = "NativeBleManager"
    }
    
    private var bluetoothGatt: BluetoothGatt? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val handler = Handler(Looper.getMainLooper())
    
    // Estados da conexão BLE
    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()
    
    private val _deviceName = MutableStateFlow("")
    val deviceName: StateFlow<String> = _deviceName.asStateFlow()
    
    // Store device address for retry
    private var currentDeviceAddress: String? = null
    
    // Dados recebidos do anel
    private val _rawData = MutableStateFlow<ByteArray?>(null)
    val rawData: StateFlow<ByteArray?> = _rawData.asStateFlow()
    
    // Fila de comandos para envio sequencial
    private val commandQueue = LinkedList<ByteArray>()
    private var isWriting = false
    
    // Estados de inicialização BLE
    private var servicesDiscovered = false
    private var mtuSet = false
    private var notificationsEnabled = false
    
    // Connection timeout and retry
    private var connectionCallback: ((Boolean, String?) -> Unit)? = null
    private val connectionTimeoutMs = 30000L // 30 seconds
    private var connectionTimeoutRunnable: Runnable? = null
    private var watchdogRunnable: Runnable? = null
    private var retryCount = 0
    private val maxRetries = 3
    
    enum class BleConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, SERVICES_DISCOVERED, MTU_SET, NOTIFICATIONS_ENABLED, READY
    }
    
    fun initialize() {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        Log.d(TAG, "🔧 Native BLE Manager initialized")
    }
    
    fun connect(deviceAddress: String, callback: (Boolean, String?) -> Unit) {
        LogManager.log(TAG, "🔗 Conectando ao J2301B: $deviceAddress (Retry: $retryCount/$maxRetries)", "INFO")
        
        connectionCallback = callback
        
        bluetoothAdapter?.let { adapter ->
            try {
                // Cancel any existing timeout
                connectionTimeoutRunnable?.let { handler.removeCallbacks(it) }
                
                val device = adapter.getRemoteDevice(deviceAddress)
                currentDeviceAddress = deviceAddress
                _connectionState.value = BleConnectionState.CONNECTING
                _deviceName.value = device.name ?: "J2301B Ring"
                
                // Reset estados
                servicesDiscovered = false
                mtuSet = false
                notificationsEnabled = false
                
                // Disconnect any existing connection first
                bluetoothGatt?.let {
                    it.disconnect()
                    it.close()
                    refreshGattCache(it)
                }
                bluetoothGatt = null
                
                // Wait a bit before connecting
                handler.postDelayed({
                    try {
                        // Conectar seguindo o padrão do BleService original
                        bluetoothGatt = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            device.connectGatt(context, false, gattCallback, 2) // TRANSPORT_LE = 2
                        } else {
                            device.connectGatt(context, false, gattCallback)
                        }
                        
                        // Set connection timeout
                        connectionTimeoutRunnable = Runnable {
                            Log.e(TAG, "⏰ Connection timeout after ${connectionTimeoutMs}ms")
                            handleConnectionFailure("Connection timeout")
                        }
                        handler.postDelayed(connectionTimeoutRunnable!!, connectionTimeoutMs)
                        
                        // Start watchdog (if connection gets stuck without callback)
                        startWatchdog()
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error in delayed connect", e)
                        handleConnectionFailure(e.message ?: "Unknown error")
                    }
                }, 500) // 500ms delay before connecting
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error connecting to device", e)
                handleConnectionFailure(e.message ?: "Unknown error")
            }
        } ?: run {
            handleConnectionFailure("Bluetooth adapter not available")
        }
    }
    
    private fun handleConnectionFailure(error: String) {
        // Cancel all timers
        connectionTimeoutRunnable?.let { handler.removeCallbacks(it) }
        watchdogRunnable?.let { handler.removeCallbacks(it) }
        connectionTimeoutRunnable = null
        watchdogRunnable = null
        
        if (retryCount < maxRetries) {
            retryCount++
            Log.w(TAG, "🔄 Connection failed, retrying in 2s... ($retryCount/$maxRetries): $error")
            
            // Clean up current connection
            bluetoothGatt?.let {
                it.disconnect()
                it.close()
                refreshGattCache(it)
            }
            bluetoothGatt = null
            _connectionState.value = BleConnectionState.DISCONNECTED
            
            // Retry after delay
            handler.postDelayed({
                currentDeviceAddress?.let { address ->
                    connectionCallback?.let { callback ->
                        connect(address, callback)
                    }
                } ?: run {
                    Log.e(TAG, "Cannot retry - device address not available")
                    connectionCallback?.invoke(false, "Connection failed: $error")
                    connectionCallback = null
                }
            }, 2000)
        } else {
            Log.e(TAG, "❌ Connection failed after $maxRetries retries: $error")
            retryCount = 0
            _connectionState.value = BleConnectionState.DISCONNECTED
            connectionCallback?.invoke(false, "Connection failed after $maxRetries retries: $error")
            connectionCallback = null
        }
    }
    
    fun disconnect() {
        Log.d(TAG, "🔌 Disconnecting from J2301B")
        
        // Cancel all pending timers
        connectionTimeoutRunnable?.let { handler.removeCallbacks(it) }
        watchdogRunnable?.let { handler.removeCallbacks(it) }
        connectionTimeoutRunnable = null
        watchdogRunnable = null
        
        bluetoothGatt?.let { gatt ->
            try {
                gatt.disconnect()
                gatt.close()
                refreshGattCache(gatt)
            } catch (e: Exception) {
                Log.w(TAG, "Error during disconnect", e)
            }
        }
        bluetoothGatt = null
        _connectionState.value = BleConnectionState.DISCONNECTED
        _deviceName.value = ""
        currentDeviceAddress = null
        
        // Reset estados
        servicesDiscovered = false
        mtuSet = false
        notificationsEnabled = false
        commandQueue.clear()
        isWriting = false
        retryCount = 0
        
        // Reset callback
        connectionCallback = null
        
        Log.d(TAG, "✅ Disconnect completed and state reset")
    }
    
    fun writeCommand(command: ByteArray) {
        if (_connectionState.value != BleConnectionState.READY) {
            Log.w(TAG, "⚠️ Cannot write - BLE not ready. Current state: ${_connectionState.value}")
            return
        }
        
        Log.d(TAG, "📤 Queueing command: ${command.contentToString()}")
        commandQueue.offer(command)
        processCommandQueue()
    }
    
    private fun processCommandQueue() {
        if (isWriting || commandQueue.isEmpty()) return
        
        val command = commandQueue.poll() ?: return
        isWriting = true
        
        bluetoothGatt?.let { gatt ->
            val service = gatt.getService(SERVICE_UUID)
            val characteristic = service?.getCharacteristic(WRITE_CHARACTERISTIC_UUID)
            
            if (characteristic != null) {
                characteristic.value = command
                val success = gatt.writeCharacteristic(characteristic)
                Log.d(TAG, "📝 Writing command: ${command.contentToString()}, success: $success")
                
                if (!success) {
                    isWriting = false
                    // Retry after delay
                    handler.postDelayed({ processCommandQueue() }, 100)
                }
            } else {
                Log.e(TAG, "❌ Write characteristic not found")
                isWriting = false
            }
        }
    }
    
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            
            Log.d(TAG, "🔄 Connection state changed - Status: $status, NewState: $newState")
            
            // CRITICAL: Check for GATT errors FIRST (status != GATT_SUCCESS)
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "❌ GATT Error - Status: $status (133=GATT_ERROR, 8=TIMEOUT, 62=INTERNAL_ERROR)")
                
                // Cleanup current connection immediately
                gatt?.let {
                    try {
                        it.disconnect()
                        it.close()
                        refreshGattCache(it)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error during GATT cleanup", e)
                    }
                }
                bluetoothGatt = null
                
                // Handle the failure with retry
                handleConnectionFailure("GATT Error status: $status")
                return
            }
            
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    LogManager.log(TAG, "✅ Conectado ao J2301B - GATT_SUCCESS", "INFO")
                    _connectionState.value = BleConnectionState.CONNECTED
                    
                    // Cancel connection timeout
                    connectionTimeoutRunnable?.let { handler.removeCallbacks(it) }
                    connectionTimeoutRunnable = null
                    
                    // Reset retry count on successful connection
                    retryCount = 0
                    
                    // CRITICAL: Request high priority connection FIRST
                    if (android.os.Build.VERSION.SDK_INT >= 21) {
                        val prioritySuccess = gatt?.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH) ?: false
                        Log.d(TAG, "⚡ HIGH Priority requested: $prioritySuccess")
                    }
                    
                    // Then request MTU (pequeno delay para prioridade ser aplicada)
                    handler.postDelayed({
                        requestMtu(gatt)
                    }, 100)
                }
                
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "❌ Disconnected from J2301B")
                    _connectionState.value = BleConnectionState.DISCONNECTED
                    
                    // Cleanup
                    gatt?.let { 
                        try {
                            it.close()
                            refreshGattCache(it)
                        } catch (e: Exception) {
                            Log.w(TAG, "Error during disconnect cleanup", e)
                        }
                    }
                    bluetoothGatt = null
                    servicesDiscovered = false
                    mtuSet = false
                    notificationsEnabled = false
                    commandQueue.clear()
                    isWriting = false
                }
            }
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "🔍 Services discovered successfully")
                servicesDiscovered = true
                _connectionState.value = BleConnectionState.SERVICES_DISCOVERED
                
                // Validate that we have the J2301B service and characteristics
                val service = gatt?.getService(SERVICE_UUID)
                val writeChar = service?.getCharacteristic(WRITE_CHARACTERISTIC_UUID)
                val notifyChar = service?.getCharacteristic(NOTIFY_CHARACTERISTIC_UUID)
                
                if (service != null && writeChar != null && notifyChar != null) {
                    Log.d(TAG, "✅ J2301B Service and characteristics found")
                    
                    // Enable notifications IMMEDIATELY after services discovered
                    handler.postDelayed({
                        enableNotifications(gatt)
                    }, 100)
                } else {
                    Log.e(TAG, "❌ J2301B Service or characteristics not found!")
                    Log.e(TAG, "Service: $service, Write: $writeChar, Notify: $notifyChar")
                    handleConnectionFailure("Required BLE services not found")
                }
                
            } else {
                Log.e(TAG, "❌ Service discovery failed: $status")
                handleConnectionFailure("Service discovery failed: $status")
            }
        }
        
        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "📏 MTU changed to: $mtu")
                mtuSet = true
                _connectionState.value = BleConnectionState.MTU_SET
            } else {
                Log.w(TAG, "⚠️ MTU change failed: $status - continuing anyway")
            }
            
            // Discover services AFTER MTU is set (or failed)
            handler.postDelayed({
                val discoverSuccess = gatt?.discoverServices() ?: false
                Log.d(TAG, "🔍 Discovering services: $discoverSuccess")
                if (!discoverSuccess) {
                    handleConnectionFailure("Failed to start service discovery")
                }
            }, 100)
        }
        
        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            
            if (status == BluetoothGatt.GATT_SUCCESS) {
                LogManager.log(TAG, "🔔 NOTIFICAÇÕES HABILITADAS COM SUCESSO!", "INFO")
                notificationsEnabled = true
                _connectionState.value = BleConnectionState.NOTIFICATIONS_ENABLED
                
                // Próximo passo: Request HIGH priority connection
                handler.postDelayed({
                    finalizeConnection(gatt)
                }, 100)
                
            } else {
                LogManager.log(TAG, "❌ FALHA ao habilitar notificações: $status", "ERROR")
            }
        }
        
        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "✅ Command written successfully")
            } else {
                Log.e(TAG, "❌ Command write failed: $status")
            }
            
            // Processar próximo comando na fila
            isWriting = false
            handler.postDelayed({ processCommandQueue() }, 30) // 30ms delay como no original
        }
        
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicChanged(gatt, characteristic)
            
            LogManager.log(TAG, "🔔 onCharacteristicChanged TRIGGERED!", "INFO")
            LogManager.log(TAG, "🔍 Characteristic UUID: ${characteristic?.uuid}", "INFO")
            LogManager.log(TAG, "🔍 Expected UUID: $NOTIFY_CHARACTERISTIC_UUID", "INFO")
            
            characteristic?.value?.let { data ->
                LogManager.log(TAG, "📨 DATA RECEIVED! Size: ${data.size} bytes", "INFO")
                LogManager.log(TAG, "📨 Raw bytes: ${data.contentToString()}", "INFO")
                LogManager.log(TAG, "📨 Hex: ${data.joinToString(" ") { "%02x".format(it) }}", "INFO")
                
                _rawData.value = data
                
                // CRITICAL: Send ACTION_DATA_AVAILABLE broadcast like original BleService
                val intent = Intent("com.jstylelife.ble.service.ACTION_DATA_AVAILABLE")
                intent.putExtra("value", data)
                context.sendBroadcast(intent)
                LogManager.log(TAG, "📡 Sent ACTION_DATA_AVAILABLE broadcast", "INFO")
                
            } ?: run {
                LogManager.log(TAG, "❌ Characteristic value is NULL!", "ERROR")
            }
        }
    }
    
    private fun requestMtu(gatt: BluetoothGatt?) {
        Log.d(TAG, "📏 Requesting MTU 247")
        val success = gatt?.requestMtu(247) ?: false
        if (!success) {
            Log.e(TAG, "❌ Failed to request MTU")
        }
    }
    
    private fun enableNotifications(gatt: BluetoothGatt?) {
        Log.d(TAG, "🔔 DETAILED: Enabling notifications on J2301B")
        
        val service = gatt?.getService(SERVICE_UUID)
        Log.d(TAG, "🔍 Service found: ${service != null}")
        Log.d(TAG, "🔍 Service UUID: ${service?.uuid}")
        
        val notifyCharacteristic = service?.getCharacteristic(NOTIFY_CHARACTERISTIC_UUID)
        Log.d(TAG, "🔍 Notify characteristic found: ${notifyCharacteristic != null}")
        Log.d(TAG, "🔍 Notify char UUID: ${notifyCharacteristic?.uuid}")
        Log.d(TAG, "🔍 Char properties: ${notifyCharacteristic?.properties}")
        
        if (notifyCharacteristic != null) {
            // Verificar se suporta notificações
            val supportsNotify = (notifyCharacteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
            val supportsIndicate = (notifyCharacteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
            Log.d(TAG, "🔍 Supports NOTIFY: $supportsNotify, INDICATE: $supportsIndicate")
            
            // 1. Habilitar notificação local
            val success = gatt.setCharacteristicNotification(notifyCharacteristic, true)
            Log.d(TAG, "📱 Local notification enabled: $success")
            
            if (success) {
                // 2. Escrever CCCD (0x2902) - CRÍTICO!
                val cccdDescriptor = notifyCharacteristic.getDescriptor(CCCD_UUID)
                Log.d(TAG, "🔍 CCCD descriptor found: ${cccdDescriptor != null}")
                
                if (cccdDescriptor != null) {
                    // Usar NOTIFY ou INDICATE baseado no que a característica suporta
                    cccdDescriptor.value = if (supportsNotify) {
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    } else {
                        BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                    }
                    
                    val writeSuccess = gatt.writeDescriptor(cccdDescriptor)
                    Log.d(TAG, "📝 CCCD write initiated: $writeSuccess (${if (supportsNotify) "NOTIFY" else "INDICATE"})")
                } else {
                    Log.e(TAG, "❌ CCCD descriptor not found!")
                    Log.e(TAG, "Available descriptors:")
                    notifyCharacteristic.descriptors?.forEach { desc ->
                        Log.e(TAG, "  - ${desc.uuid}")
                    }
                }
            } else {
                Log.e(TAG, "❌ Failed to set local notification!")
            }
        } else {
            Log.e(TAG, "❌ Notify characteristic not found!")
            Log.e(TAG, "Available characteristics in service:")
            service?.characteristics?.forEach { char ->
                Log.e(TAG, "  - ${char.uuid} (props: ${char.properties})")
            }
        }
    }
    
    private fun finalizeConnection(gatt: BluetoothGatt?) {
        Log.d(TAG, "🏁 Finalizing BLE connection")
        
        // Cancel all timers
        connectionTimeoutRunnable?.let { handler.removeCallbacks(it) }
        watchdogRunnable?.let { handler.removeCallbacks(it) }
        connectionTimeoutRunnable = null
        watchdogRunnable = null
        
        // Marcar como PRONTO
        _connectionState.value = BleConnectionState.READY
        Log.d(TAG, "🎉 BLE connection fully ready for commands!")
        
        // Test basic communication with SDK ping
        handler.postDelayed({
            testCommunication()
        }, 500)
        
        // Notify success callback
        connectionCallback?.invoke(true, null)
        connectionCallback = null
    }
    
    private fun startWatchdog() {
        // If connection gets stuck without onConnectionStateChange, retry in 10s
        watchdogRunnable = Runnable {
            if (bluetoothGatt != null && _connectionState.value == BleConnectionState.CONNECTING) {
                Log.e(TAG, "🐶 Watchdog triggered - connection stuck, forcing retry")
                bluetoothGatt?.let {
                    try {
                        it.disconnect()
                        it.close()
                        refreshGattCache(it)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error during watchdog cleanup", e)
                    }
                }
                bluetoothGatt = null
                handleConnectionFailure("Connection watchdog timeout")
            }
        }
        handler.postDelayed(watchdogRunnable!!, 10000) // 10 second watchdog
    }
    
    private fun testCommunication() {
        Log.d(TAG, "🇺🇦 Testing basic communication - sending GetDeviceTime")
        
        // Basic ping test using SDK GetDeviceTime command
        try {
            val testCommand = byteArrayOf(0xAA.toByte(), 0x55.toByte(), 0x00.toByte(), 0x01.toByte(), 0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x57.toByte())
            writeCommand(testCommand)
            Log.d(TAG, "📤 Test command sent - awaiting response...")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending test command", e)
        }
    }
    
    // Refresh GATT cache (copiado do BleService original)
    private fun refreshGattCache(gatt: BluetoothGatt): Boolean {
        Log.d(TAG, "🔄 Refreshing GATT cache")
        try {
            val method = gatt.javaClass.getMethod("refresh")
            if (method != null) {
                val result = method.invoke(gatt) as Boolean
                Log.d(TAG, "Cache refresh result: $result")
                return result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh GATT cache", e)
        }
        return false
    }
    
    fun isReady(): Boolean {
        return _connectionState.value == BleConnectionState.READY
    }
}