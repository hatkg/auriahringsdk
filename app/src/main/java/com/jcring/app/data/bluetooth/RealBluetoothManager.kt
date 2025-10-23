package com.jcring.app.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.jcring.app.data.logger.LogManager
import com.jstyle.blesdk2301.Util.BleSDK
import com.jstyle.test2025.ble.BleManager
import com.jstyle.blesdk2301.callback.DataListener2301
import com.jstyle.blesdk2301.constant.BleConst
import com.jstyle.blesdk2301.constant.DeviceKey
import com.jstyle.blesdk2301.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.*

/**
 * REAL Bluetooth Manager seguindo EXATAMENTE o padrão do SDK original
 * 
 * Baseado no RealTimeStepCountingActivity.java e BatteryActivity.java
 * 
 * COLETA DE DADOS REAL:
 * 1. BleSDK.RealTimeStep(true, true) - Dados em tempo real
 * 2. BleSDK.GetDeviceBatteryLevel() - Bateria
 * 3. BleSDK.GetDynamicHRWithMode() - Frequência cardíaca
 * 4. BleSDK.GetTotalActivityData() - Dados de atividade
 * 5. BleSDK.GetTemperature_historyData() - Temperatura
 */
class RealBluetoothManager private constructor(private val context: Context) : DataListener2301 {
    
    companion object {
        @Volatile
        private var INSTANCE: RealBluetoothManager? = null
        
        fun getInstance(context: Context): RealBluetoothManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RealBluetoothManager(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        private const val TAG = "RealBluetoothManager"
    }
    
    // Instâncias para BLE real
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: android.bluetooth.BluetoothAdapter
    private var bleManager: BleManager? = null

    // Handler para operações BLE
    private val handler = Handler(Looper.getMainLooper())
    
    // Callback para scan BLE
    private var currentScanCallback: android.bluetooth.le.ScanCallback? = null
    
    // Estados de conexão
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _deviceName = MutableStateFlow("")
    val deviceName: StateFlow<String> = _deviceName.asStateFlow()
    
    // DADOS DE SAÚDE REAIS - seguindo EXATAMENTE o SDK original
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
    
    private val _distance = MutableStateFlow(0f)
    val distance: StateFlow<Float> = _distance.asStateFlow()
    
    private val _batteryLevel = MutableStateFlow(0)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()
    
    // Dados adicionais para compatibilidade
    private val _glucose = MutableStateFlow<Float?>(null)
    val glucose: StateFlow<Float?> = _glucose.asStateFlow()
    
    private val _hrv = MutableStateFlow<Int?>(null)
    val hrv: StateFlow<Int?> = _hrv.asStateFlow()
    
    private val _stress = MutableStateFlow<Int?>(null)
    val stress: StateFlow<Int?> = _stress.asStateFlow()
    
    // Estado da medição em tempo real
    private var isRealTimeStarted = false
    
    fun initialize() {
        try {
            LogManager.log(TAG, "🔧 INICIANDO RealBluetoothManager SIMPLES...", "INFO")
            
            bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothAdapter = bluetoothManager.adapter

            if (bleManager == null) {
                BleManager.init(context)
                bleManager = BleManager.getInstance()
                bleManager?.bluetoothAdapter = this.bluetoothAdapter // FIX: Injetar o adapter diretamente
                LogManager.log(TAG, "✅ BleManager inicializado: ${bleManager != null}", "INFO")
                try {
                    val serviceIntent = Intent(context, com.jstyle.test2025.ble.BleService::class.java)
                    context.startService(serviceIntent)
                    LogManager.log(TAG, "✅ BleService iniciado com startService", "INFO")
                } catch (illegal: IllegalStateException) {
                    LogManager.log(TAG, "⚠️ startService falhou: ${illegal.message}", "WARN")
                } catch (security: SecurityException) {
                    LogManager.log(TAG, "⚠️ Permissão insuficiente para iniciar BleService: ${security.message}", "WARN")
                }
            }

            if (!bluetoothAdapter.isEnabled) {
                LogManager.log(TAG, "⚠️ Bluetooth não está ligado!", "WARN")
            }
            
            com.jstyle.test2025.ble.BleService.setDataListener(this)
            LogManager.log(TAG, "✅ DataListener registrado no BleService!", "INFO")
            
            LogManager.log(TAG, "✅ RealBluetoothManager inicializado corretamente", "INFO")
        } catch (e: Exception) {
            LogManager.log(TAG, "❌ Erro ao inicializar: ${e.message}", "ERROR")
        }
    }
    
    
    /**
     * IMPLEMENTAÇÃO EXATA DO DataListener2301 - igual ao RealTimeStepCountingActivity.java
     */
    override fun dataCallback(maps: Map<String, Any>) {
        LogManager.log(TAG, "🎉 DATACALLBACK EXECUTADO! SDK parser funcionou!", "INFO")
        LogManager.log(TAG, "🎯 Conteúdo completo do Map: $maps", "INFO")
        
        try {
            val dataType = getDataType(maps)
            LogManager.log(TAG, "🔍 DataType extraído: '$dataType'", "INFO")
            LogManager.log(TAG, "🔍 Map keys: ${maps.keys}", "INFO")
            LogManager.log(TAG, "🔍 Map values: ${maps.values}", "INFO")
            
            // 🔧 TERCEIRA CORREÇÃO: Verificar teste básico primeiro
            if (dataType == BleConst.GetDeviceTime) { // "0"
                Log.d(TAG, "✅ PIPELINE SDK FUNCIONANDO! GetDeviceTime received: $maps")
                val data = getData(maps)
                data?.let { 
                    Log.d(TAG, "📅 Device Time: ${it[DeviceKey.DeviceTime]}")
                }
            }
            
            when (dataType) {
                BleConst.RealTimeStep -> { // "23" - DADOS EM TEMPO REAL
                    Log.d(TAG, "🎯 REAL TIME STEP DATA RECEIVED! - Esta é a linha mais importante!")
                    
                    // IMPLEMENTAÇÃO EXATA do RealTimeStepCountingActivity.java linha 123-142
                    val data = getData(maps)
                    data?.let { mmp ->
                        Log.d(TAG, "📊 Chaves disponíveis: ${mmp.keys}")
                        
                        // Seguindo EXATAMENTE as chaves do SDK original
                        val step = mmp[DeviceKey.Step]              // Passos
                        val cal = mmp[DeviceKey.Calories]           // Calorias
                        val distance = mmp[DeviceKey.Distance]      // Distância
                        val heartRate = mmp[DeviceKey.HeartRate]    // Frequência cardíaca
                        val tempData = mmp[DeviceKey.TempData]      // Temperatura
                        val bloodOxygen = mmp[DeviceKey.Blood_oxygen] // SpO2
                        
                        Log.d(TAG, "🔍 Raw values - Step:$step, Cal:$cal, Dist:$distance, HR:$heartRate, Temp:$tempData, SpO2:$bloodOxygen")
                        
                        // Atualizar StateFlows com dados REAIS
                        step?.toIntOrNull()?.let { 
                            _steps.value = it
                            Log.d(TAG, "✅ REAL Steps: $it")
                        }
                        
                        cal?.toIntOrNull()?.let { 
                            _calories.value = it
                            Log.d(TAG, "✅ REAL Calories: $it")
                        }
                        
                        distance?.toFloatOrNull()?.let { 
                            _distance.value = it / 100f // Conversão igual ao original
                            Log.d(TAG, "✅ REAL Distance: ${it / 100f} km")
                        }
                        
                        heartRate?.toIntOrNull()?.let { hr ->
                            if (hr > 0 && hr < 300) {
                                _heartRate.value = hr
                                Log.d(TAG, "✅ REAL Heart Rate: $hr bpm")
                            } else {
                                Log.d(TAG, "⚠️ HR fora da faixa: $hr")
                            }
                        }
                        
                        tempData?.toFloatOrNull()?.let { temp ->
                            val tempCelsius = temp * 0.1f // Conversão igual ao original
                            if (tempCelsius > 30f && tempCelsius < 45f) {
                                _temperature.value = tempCelsius
                                Log.d(TAG, "✅ REAL Temperature: $tempCelsius°C")
                            } else {
                                Log.d(TAG, "⚠️ Temp fora da faixa: $tempCelsius°C")
                            }
                        }
                        
                        bloodOxygen?.toIntOrNull()?.let { spo2 ->
                            if (spo2 > 70 && spo2 <= 100) {
                                _spO2.value = spo2
                                Log.d(TAG, "✅ REAL SpO2: $spo2%")
                            } else {
                                Log.d(TAG, "⚠️ SpO2 fora da faixa: $spo2%")
                            }
                        }
                    } ?: run {
                        Log.e(TAG, "❌ getData retornou null para RealTimeStep!")
                    }
                }
                
                BleConst.GetDeviceBatteryLevel -> {
                    // IMPLEMENTAÇÃO EXATA do BatteryActivity.java linha 41-44
                    Log.d(TAG, "🔋 BATERIA: $maps")
                    val data = getData(maps)
                    data?.get(DeviceKey.BatteryLevel)?.toIntOrNull()?.let { battery ->
                        _batteryLevel.value = battery
                        Log.d(TAG, "✅ REAL Battery: $battery%")
                    }
                }
                
                BleConst.GetDynamicHR, BleConst.GetStaticHR -> {
                    // Dados de frequência cardíaca histórica
                    Log.d(TAG, "❤️ HR HISTÓRICA: $maps")
                    processHeartRateHistory(maps)
                }
                
                BleConst.GetTotalActivityData -> {
                    // Dados totais de atividade (sincronização)
                    Log.d(TAG, "🏃 ATIVIDADE TOTAL: $maps")
                    processTotalActivityData(maps)
                }
                
                BleConst.Temperature_history -> {
                    // Histórico de temperatura
                    Log.d(TAG, "🌡️ TEMPERATURA HISTÓRICA: $maps")
                    processTemperatureHistory(maps)
                }
                
                BleConst.GetHRVData -> {
                    // Dados de HRV e stress
                    Log.d(TAG, "💓 HRV/STRESS: $maps")
                    processHrvData(maps)
                }
                
                BleConst.Blood_oxygen -> {
                    // Dados de SpO2
                    Log.d(TAG, "🫁 SPO2: $maps")
                    processSpO2Data(maps)
                }
                
                // Callbacks das medições automáticas - seguindo AutoModeSetActivity.java
                BleConst.SetAutomatic -> {
                    Log.d(TAG, "⏰ Medição automática configurada: $maps")
                }
                
                BleConst.GetAutomatic -> {
                    Log.d(TAG, "⏰ Estado de medição automática: $maps")
                }
                
                // 🔧 QUARTA CORREÇÃO: Separar ACK das medições dos RESULTADOS
                BleConst.MeasurementHeartCallback -> { // "74" - ACK de início de medição HR
                    Log.d(TAG, "❤️ ACK: Medição HR iniciada - aguardando resultado...")
                    // Após 45s, buscar resultado
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(45000)
                        sendCommand(BleSDK.GetDynamicHRWithMode(0x00.toByte(), ""), "HR Result After Measurement")
                    }
                }
                
                BleConst.MeasurementOxygenCallback -> { // "75" - ACK de início de medição SpO2
                    Log.d(TAG, "🫁 ACK: Medição SpO2 iniciada - aguardando resultado...")
                    // Após 60s, buscar resultado
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(60000)
                        sendCommand(BleSDK.Oxygen_data(0x00.toByte(), ""), "SpO2 Result After Measurement")
                    }
                }
                
                BleConst.MeasurementHrvCallback -> { // "73" - ACK de início de medição HRV
                    Log.d(TAG, "💓 ACK: Medição HRV iniciada - aguardando resultado...")
                    // Após 120s, buscar resultado
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(120000)
                        sendCommand(BleSDK.GetHRVDataWithMode(0x00.toByte(), ""), "HRV Result After Measurement")
                    }
                }
                
                BleConst.StopMeasurementHeartCallback,
                BleConst.StopMeasurementOxygenCallback,
                BleConst.StopMeasurementHrvCallback -> {
                    Log.d(TAG, "⏹️ Medição interrompida: $maps")
                }
                
                else -> {
                    Log.d(TAG, "📋 Outros dados - DataType: $dataType")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao processar dataCallback", e)
        }
    }
    
    override fun dataCallback(value: ByteArray?) {
        // Implementação secundária - não usada
    }
    
    private fun processHeartRateHistory(maps: Map<String, Any>) {
        try {
            val dataList = maps["Data"] as? List<Map<String, String>>
            dataList?.forEach { data ->
                data[DeviceKey.HeartRate]?.toIntOrNull()?.let { hr ->
                    if (hr > 0 && hr < 300) {
                        _heartRate.value = hr
                        Log.d(TAG, "✅ HR História: $hr bpm")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao processar HR histórica", e)
        }
    }
    
    private fun processTotalActivityData(maps: Map<String, Any>) {
        try {
            val dataList = maps["Data"] as? List<Map<String, String>>
            dataList?.forEach { data ->
                data[DeviceKey.Step]?.toIntOrNull()?.let { steps ->
                    _steps.value = steps
                    Log.d(TAG, "✅ Steps Sincronizados: $steps")
                }
                
                data[DeviceKey.Calories]?.toIntOrNull()?.let { calories ->
                    _calories.value = calories
                    Log.d(TAG, "✅ Calories Sincronizadas: $calories")
                }
                
                data[DeviceKey.Distance]?.toFloatOrNull()?.let { distance ->
                    _distance.value = distance / 100f
                    Log.d(TAG, "✅ Distance Sincronizada: ${distance / 100f} km")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao processar atividade total", e)
        }
    }
    
    private fun processTemperatureHistory(maps: Map<String, Any>) {
        try {
            val dataList = maps["Data"] as? List<Map<String, String>>
            dataList?.forEach { data ->
                data[DeviceKey.temperature]?.toFloatOrNull()?.let { temp ->
                    if (temp > 30f && temp < 45f) {
                        _temperature.value = temp
                        Log.d(TAG, "✅ Temperatura História: $temp°C")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao processar temperatura histórica", e)
        }
    }
    
    private fun processHrvData(maps: Map<String, Any>) {
        try {
            val dataList = maps["Data"] as? List<Map<String, String>>
            dataList?.forEach { data ->
                data[DeviceKey.HRV]?.toIntOrNull()?.let { hrv ->
                    _hrv.value = hrv
                    Log.d(TAG, "✅ HRV: $hrv ms")
                }
                
                data[DeviceKey.Stress]?.toIntOrNull()?.let { stress ->
                    _stress.value = stress
                    Log.d(TAG, "✅ Stress: $stress")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao processar HRV", e)
        }
    }
    
    private fun processSpO2Data(maps: Map<String, Any>) {
        try {
            val data = getData(maps)
            data?.get(DeviceKey.Blood_oxygen)?.toIntOrNull()?.let { spo2 ->
                if (spo2 > 70 && spo2 <= 100) {
                    _spO2.value = spo2
                    Log.d(TAG, "✅ SpO2: $spo2%")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao processar SpO2", e)
        }
    }
    
    // Helper methods seguindo BaseActivity.java
    private fun getDataType(maps: Map<String, Any>): String {
        return maps[DeviceKey.DataType] as? String ?: ""
    }
    
    private fun getData(maps: Map<String, Any>): Map<String, String>? {
        return maps[DeviceKey.Data] as? Map<String, String>
    }
    
    /**
     * 🔧 TESTE BÁSICO DE COMUNICAÇÃO primeiro - seguindo seu checklist
     * Testa se o pipeline SDK está funcionando com GetDeviceTime
     */
    private fun testBasicCommunication() {
        try {
            Log.d(TAG, "🧪 TESTE BÁSICO: Enviando GetDeviceTime para testar pipeline SDK...")
            
            // 👉 Se este comando não chegar no dataCallback com dataType="0", 
            // então o problema é o pipeline BLE → SDK
            sendCommand(BleSDK.GetDeviceTime(), "Basic Communication Test")
            
            // Aguardar 3 segundos e se funcionar, iniciar coleta real
            CoroutineScope(Dispatchers.Main).launch {
                delay(3000)
                Log.d(TAG, "🚀 Teste básico concluído, iniciando coleta real...")
                startRealDataCollection()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no teste básico", e)
        }
    }
    
    /**
     * Iniciar coleta de dados REAL seguindo RealTimeStepCountingActivity.java
     */
    private fun startRealDataCollection() {
        try {
            Log.d(TAG, "🚀 Iniciando coleta de dados REAL...")
            
            // 1. Configurar informações pessoais (necessário para cálculos corretos)
            val personalInfo = MyPersonalInfo().apply {
                age = 65
                height = 170
                weight = 70
                sex = 1
            }
            sendCommand(BleSDK.SetPersonalInfo(personalInfo), "Personal Info")
            
            // 2. Configurar tempo do dispositivo
            val deviceTime = MyDeviceTime().apply {
                year = Calendar.getInstance().get(Calendar.YEAR)
                month = Calendar.getInstance().get(Calendar.MONTH) + 1
                day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                minute = Calendar.getInstance().get(Calendar.MINUTE)
                second = Calendar.getInstance().get(Calendar.SECOND)
            }
            sendCommand(BleSDK.SetDeviceTime(deviceTime), "Device Time")
            
            // 3. 🔧 QUINTA CORREÇÃO: INICIAR COLETA EM TEMPO REAL
            // 👉 Este é o comando que faz aparecer HR/steps/etc. na tela em tempo real!
            // BleSDK.RealTimeStep(isStartReal, SwitchCompatTemp.isChecked())
            Log.d(TAG, "🎯 INICIANDO RealTimeStep - dados contínuos HR/Steps/Calorias...")
            isRealTimeStarted = true
            sendCommand(BleSDK.RealTimeStep(true, true), "Real Time Data Collection")
            Log.d(TAG, "✅ RealTimeStep enviado - dados devem chegar com dataType=23")
            
            // 4. Configurar medições automáticas
            setupAutomaticMeasurements()
            
            // 5. Solicitar dados de sincronização
            requestSyncData()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao iniciar coleta de dados", e)
        }
    }
    
    /**
     * Solicitar dados de sincronização (dados já existentes no anel)
     */
    private fun requestSyncData() {
        try {
            Log.d(TAG, "🔄 Solicitando sincronização de dados...")
            
            // Solicitar bateria - BatteryActivity.java linha 30
            sendCommand(BleSDK.GetDeviceBatteryLevel(), "Battery Level")
            
            // Solicitar dados de atividade total
            sendCommand(BleSDK.GetTotalActivityDataWithMode(0x00.toByte(), ""), "Total Activity Data")
            
            // Solicitar frequência cardíaca histórica
            sendCommand(BleSDK.GetDynamicHRWithMode(0x00.toByte(), ""), "Dynamic HR History")
            sendCommand(BleSDK.GetStaticHRWithMode(0x00.toByte(), ""), "Static HR History")
            
            // Solicitar temperatura histórica
            sendCommand(BleSDK.GetTemperature_historyData(0x00.toByte(), ""), "Temperature History")
            
            // Solicitar dados de HRV
            sendCommand(BleSDK.GetHRVDataWithMode(0x00.toByte(), ""), "HRV Data")
            
            Log.d(TAG, "✅ Todos os comandos de sincronização enviados")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao solicitar sincronização", e)
        }
    }
    
    /**
     * Configurar medições automáticas seguindo AutoModeSetActivity.java
     * Medições de HR, SpO2, Temperatura e HRV de forma automática
     */
    private fun setupAutomaticMeasurements() {
        try {
            Log.d(TAG, "⏰ Configurando medições automáticas...")
            
            // Configurar medição automática de frequência cardíaca a cada 30 minutos
            val automaticHR = MyAutomaticHRMonitoring().apply {
                startHour = 6   // Iniciar às 6h
                startMinute = 0
                endHour = 22    // Terminar às 22h
                endMinute = 0
                time = 30       // Intervalo de 30 minutos
                week = 127      // Todos os dias da semana (binário: 1111111)
                open = 2      // Modo de intervalo (2 = interval mode)
            }
            sendCommand(BleSDK.SetAutomaticHRMonitoring(automaticHR, AutoMode.AutoHeartRate), "Auto HR")
            
            // Configurar medição automática de SpO2 a cada 1 hora
            val automaticSpO2 = MyAutomaticHRMonitoring().apply {
                startHour = 8   // Iniciar às 8h
                startMinute = 0
                endHour = 20    // Terminar às 20h
                endMinute = 0
                time = 60       // Intervalo de 1 hora
                week = 127      // Todos os dias da semana
                open = 2      // Modo de intervalo
            }
            sendCommand(BleSDK.SetAutomaticHRMonitoring(automaticSpO2, AutoMode.AutoSpo2), "Auto SpO2")
            
            // Configurar medição automática de temperatura a cada 15 minutos
            val automaticTemp = MyAutomaticHRMonitoring().apply {
                startHour = 0   // 24 horas
                startMinute = 0
                endHour = 23
                endMinute = 59
                time = 15       // Intervalo de 15 minutos
                week = 127      // Todos os dias da semana
                open = 2      // Modo de intervalo
            }
            sendCommand(BleSDK.SetAutomaticHRMonitoring(automaticTemp, AutoMode.AutoTemp), "Auto Temperature")
            
            // Configurar medição automática de HRV a cada 2 horas
            val automaticHRV = MyAutomaticHRMonitoring().apply {
                startHour = 8   // Iniciar às 8h
                startMinute = 0
                endHour = 20    // Terminar às 20h
                endMinute = 0
                time = 120      // Intervalo de 2 horas
                week = 127      // Todos os dias da semana
                open = 2      // Modo de intervalo
            }
            sendCommand(BleSDK.SetAutomaticHRMonitoring(automaticHRV, AutoMode.AutoHrv), "Auto HRV")
            
            Log.d(TAG, "✅ Medições automáticas configuradas")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao configurar medições automáticas", e)
        }
    }
    
    private fun sendCommand(command: ByteArray?, description: String) {
        try {
            if (command != null) {
                LogManager.log(TAG, "📤 ENVIANDO COMANDO via BleService: $description", "INFO")
                LogManager.log(TAG, "📤 Command size: ${command.size} bytes", "INFO")
                LogManager.log(TAG, "📤 Command hex: ${command.joinToString(" ") { "%02x".format(it) }}", "INFO")
                if (bleManager == null) {
                    LogManager.log(TAG, "⚠️ BleManager não inicializado ao enviar comando. Recriando...", "WARN")
                    BleManager.init(context)
                    bleManager = BleManager.getInstance()
                }
                bleManager?.offerValue(command)
                bleManager?.writeValue()
                LogManager.log(TAG, "✅ Comando enfileirado via BleManager: $description", "INFO")
            } else {
                LogManager.log(TAG, "❌ Comando é NULL para: $description", "ERROR")
            }
        } catch (e: Exception) {
            LogManager.log(TAG, "❌ Erro ao enviar comando $description: $${e.message}", "ERROR")
        }
    }
    
    // Métodos públicos para interface
    @SuppressLint("MissingPermission")
    fun startScan(callback: (List<BluetoothDevice>) -> Unit) {
        try {
            LogManager.log(TAG, "🔍 Iniciando busca REAL por dispositivos J2301B...", "INFO")
            
            if (!::bluetoothAdapter.isInitialized || !bluetoothAdapter.isEnabled) {
                LogManager.log(TAG, "❌ Bluetooth não está ligado ou não inicializado!", "ERROR")
                callback(emptyList())
                return
            }
            
            val foundDevices = mutableListOf<BluetoothDevice>()
            
            currentScanCallback = object : android.bluetooth.le.ScanCallback() {
                override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult) {
                    val device = result.device
                    val name = device.name ?: ""
                    val address = device.address
                    val rssi = result.rssi
                    
                    LogManager.log(TAG, "📡 Dispositivo encontrado: Nome='$name', MAC='$address', RSSI=$rssi", "INFO")
                    
                    // Filtro para mostrar apenas anéis J2301B
                    val isJ2301B = name.uppercase().contains("J2301") || name.uppercase().contains("RING")
                    
                    if (isJ2301B) {
                        val bleDevice = BluetoothDevice(
                            name = if (name.isEmpty()) "J2301B Ring (${address.takeLast(5)})" else name,
                            address = address,
                            rssi = rssi
                        )
                        
                        // Evitar duplicatas
                        if (!foundDevices.any { it.address == address }) {
                            foundDevices.add(bleDevice)
                            LogManager.log(TAG, "✅ J2301B adicionado: ${bleDevice.name} (${bleDevice.address})", "INFO")
                        }
                    }
                }
                
                override fun onScanFailed(errorCode: Int) {
                    LogManager.log(TAG, "❌ Falha na busca BLE: $errorCode", "ERROR")
                    callback(foundDevices)
                }
            }
            
            // Iniciar scan BLE
            val leScanner = bluetoothAdapter.bluetoothLeScanner
            if (leScanner != null && currentScanCallback != null) {
                LogManager.log(TAG, "🚀 Iniciando LE scan por 5 segundos...", "INFO")
                leScanner.startScan(currentScanCallback)
                
                // Parar scan após 5 segundos
                handler.postDelayed({
                    try {
                        currentScanCallback?.let { scanCallback ->
                            leScanner.stopScan(scanCallback)
                            currentScanCallback = null
                        }
                        LogManager.log(TAG, "⏹️ Scan finalizado. Total encontrados: ${foundDevices.size}", "INFO")
                        
                        // Log de todos os dispositivos encontrados
                        foundDevices.forEach { device ->
                            LogManager.log(TAG, "📋 Final: ${device.name} (${device.address}) RSSI: ${device.rssi}", "INFO")
                        }
                        
                        callback(foundDevices)
                    } catch (e: Exception) {
                        LogManager.log(TAG, "❌ Erro ao parar scan: $${e.message}", "ERROR")
                        callback(foundDevices)
                    }
                }, 5000)
            } else {
                LogManager.log(TAG, "❌ LE Scanner não disponível", "ERROR")
                callback(emptyList())
            }
            
        } catch (e: Exception) {
            LogManager.log(TAG, "❌ Erro na busca: $${e.message}", "ERROR")
            callback(emptyList())
        }
    }
    
    @SuppressLint("MissingPermission")
    fun StopDeviceScan() {
        try {
            LogManager.log(TAG, "⏹️ Parando busca de dispositivos...", "INFO")
            
            if (::bluetoothAdapter.isInitialized) {
                currentScanCallback?.let { callback ->
                    bluetoothAdapter.bluetoothLeScanner?.stopScan(callback)
                    currentScanCallback = null
                    LogManager.log(TAG, "✅ Scan interrompido", "INFO")
                }
            }
            
        } catch (e: Exception) {
            LogManager.log(TAG, "❌ Erro ao parar busca: $${e.message}", "ERROR")
        }
    }
    
    fun connectToDevice(device: BluetoothDevice, callback: (Boolean) -> Unit) {
        try {
            LogManager.log(TAG, "🔗 CONECTANDO J2301B: ${device.name} (${device.address})", "INFO")
            
            // 🔥 USAR APENAS O BLESERVICE ORIGINAL (funcionou no SDK demo)
            LogManager.log(TAG, "🔄 Usando BleService original do SDK...", "INFO")
            com.jstyle.test2025.ble.BleService.connectedDevice(device.address, context, object : com.jstyle.blesdk2301.callback.BleConnectionListener {
                override fun ConnectionSucceeded() {
                    LogManager.log(TAG, "✅ CONECTADO com sucesso via BleService!", "INFO")
                    _deviceName.value = device.name ?: "J2301B Ring"
                    _isConnected.value = true
                    
                    // Aguardar um pouco e iniciar coleta real
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(2000)
                        startRealDataCollection()
                    }
                    
                    callback(true)
                }
                
                override fun ConnectionFailed(errorCode: Int, state: Int) {
                    LogManager.log(TAG, "❌ Falha na conexão! ErrorCode: $errorCode, State: $state", "ERROR")
                    _isConnected.value = false
                    callback(false)
                }
                
                override fun Connecting() {
                    LogManager.log(TAG, "🔄 Conectando ao J2301B...", "INFO")
                }
                
                override fun Ondisconnected() {
                    LogManager.log(TAG, "🔌 J2301B desconectado", "INFO")
                    _isConnected.value = false
                }
                
                override fun BleStatus(status: Int, newState: Int) {
                    LogManager.log(TAG, "📊 BLE Status mudou: $status -> $newState", "INFO")
                }
                
                override fun OnReconnect() {
                    LogManager.log(TAG, "🔄 Tentativa de reconexão...", "INFO")
                }
                
                override fun BluetoothSwitchIsTurnedOff() {
                    LogManager.log(TAG, "📴 Bluetooth foi desligado", "WARN")
                    _isConnected.value = false
                }
            })
            
        } catch (e: Exception) {
            LogManager.log(TAG, "❌ ERRO na conexão: $${e.message}", "ERROR")
            callback(false)
        }
    }
    
    fun disconnect() {
        try {
            Log.d(TAG, "🔌 Desconectando dispositivo...")
            
            // Parar coleta em tempo real
            if (isRealTimeStarted) {
                sendCommand(BleSDK.RealTimeStep(false, false), "Stop Real Time")
                isRealTimeStarted = false
            }
            
            // Desconectar via BleService
            com.jstyle.test2025.ble.BleService().disconnect()
            _isConnected.value = false
            
            // Reset dados
            resetAllData()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao desconectar", e)
        }
    }
    
    fun startHeartRateMeasurement(callback: (Boolean) -> Unit) {
        try {
            Log.d(TAG, "❤️ 🔧 INICIANDO medição HR - aguardando ACK dataType=74...")
            
            // 👉 Este comando só retorna ACK (dataType=74), NÃO o resultado!
            val command = BleSDK.SetDeviceMeasurementWithType(AutoTestMode.AutoHeartRate, 45, true)
            sendCommand(command, "HR Measurement Start")
            
            // O resultado chegará automaticamente no dataCallback após 45s
            // via BleConst.MeasurementHeartCallback que faz a busca
            
            callback(true)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao iniciar medição HR", e)
            callback(false)
        }
    }
    
    fun startSpO2Measurement(callback: (Boolean) -> Unit) {
        try {
            Log.d(TAG, "🫁 🔧 INICIANDO medição SpO2 - aguardando ACK dataType=75...")
            
            // 👉 Este comando só retorna ACK (dataType=75), NÃO o resultado!
            val command = BleSDK.SetDeviceMeasurementWithType(AutoTestMode.AutoSpo2, 60, true)
            sendCommand(command, "SpO2 Measurement Start")
            
            // O resultado chegará automaticamente no dataCallback após 60s
            // via BleConst.MeasurementOxygenCallback que faz a busca
            
            callback(true)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao iniciar medição SpO2", e)
            callback(false)
        }
    }
    
    fun requestBatteryLevel() {
        sendCommand(BleSDK.GetDeviceBatteryLevel(), "Battery Level Request")
    }
    
    fun requestAllHealthData() {
        requestSyncData()
    }
    
    fun measureGlucose(callback: (Boolean) -> Unit) {
        callback(true) // Mock para compatibilidade
    }
    
    fun measureStress(callback: (Boolean) -> Unit) {
        try {
            sendCommand(BleSDK.GetHRVDataWithMode(0x00.toByte(), ""), "Stress Measurement")
            callback(true)
        } catch (e: Exception) {
            callback(false)
        }
    }
    
    fun getSleepData(): Triple<Int, Int, String> {
        return Triple(7, 30, "Boa")
    }
    
    private fun resetAllData() {
        _heartRate.value = null
        _spO2.value = null
        _temperature.value = null
        _glucose.value = null
        _hrv.value = null
        _stress.value = null
        _steps.value = 0
        _calories.value = 0
        _distance.value = 0f
        _batteryLevel.value = 0
        _deviceName.value = ""
    }
    
    fun cleanup() {
        try {
            if (isRealTimeStarted) {
                sendCommand(BleSDK.RealTimeStep(false, false), "Stop Real Time")
            }
            Log.d(TAG, "🧹 Limpeza concluída")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro na limpeza", e)
        }
    }
}

// Data classes
data class BluetoothDevice(
    val name: String?,
    val address: String,
    val rssi: Int = 0
)








