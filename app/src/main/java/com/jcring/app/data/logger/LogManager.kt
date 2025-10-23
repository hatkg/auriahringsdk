package com.jcring.app.data.logger

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Sistema de logs exportáveis para debug do BLE
 */
object LogManager {
    private val logs = ConcurrentLinkedQueue<String>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    fun log(tag: String, message: String, level: String = "DEBUG") {
        val timestamp = dateFormat.format(Date())
        val logEntry = "[$timestamp] [$level] [$tag] $message"
        logs.offer(logEntry)
        
        // Manter apenas os últimos 1000 logs para não consumir muita memória
        while (logs.size > 1000) {
            logs.poll()
        }
        
        // Também imprimir no logcat normal
        when (level) {
            "ERROR" -> android.util.Log.e(tag, message)
            "WARN" -> android.util.Log.w(tag, message)
            "INFO" -> android.util.Log.i(tag, message)
            else -> android.util.Log.d(tag, message)
        }
    }
    
    fun exportLogs(context: Context): String? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "JCRingApp_Debug_$timestamp.txt"
            
            // Salvar no diretório Downloads para fácil acesso
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            
            FileWriter(file).use { writer ->
                writer.write("=== JCRingApp Debug Logs ===\n")
                writer.write("Exported at: ${dateFormat.format(Date())}\n")
                writer.write("Total logs: ${logs.size}\n")
                writer.write("=====================================\n\n")
                
                logs.forEach { log ->
                    writer.write("$log\n")
                }
            }
            
            file.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("LogManager", "Erro ao exportar logs", e)
            null
        }
    }
    
    fun clearLogs() {
        logs.clear()
    }
    
    fun getLogCount(): Int = logs.size
}