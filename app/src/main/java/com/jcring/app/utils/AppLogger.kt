package com.jcring.app.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object AppLogger {
    private const val LOG_FILE_NAME = "jcring_app.log"
    private const val MAX_LOG_SIZE = 5 * 1024 * 1024 // 5MB
    private const val TAG = "AppLogger"
    
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    fun initialize(context: Context) {
        try {
            val logDir = File(context.getExternalFilesDir(null), "logs")
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            logFile = File(logDir, LOG_FILE_NAME)
            
            // Rotate log if too large
            if (logFile?.exists() == true && logFile?.length()!! > MAX_LOG_SIZE) {
                val backupFile = File(logDir, "jcring_app_backup.log")
                logFile?.renameTo(backupFile)
                logFile = File(logDir, LOG_FILE_NAME)
            }
            
            log("INFO", "AppLogger", "Logger initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize logger", e)
        }
    }
    
    fun log(level: String, tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val logMessage = "[$timestamp] [$level] [$tag] $message"
        
        // Log to Logcat
        when (level) {
            "DEBUG" -> Log.d(tag, message)
            "INFO" -> Log.i(tag, message)
            "WARN" -> Log.w(tag, message)
            "ERROR" -> Log.e(tag, message)
            else -> Log.v(tag, message)
        }
        
        // Log to file
        try {
            logFile?.let { file ->
                FileWriter(file, true).use { writer ->
                    writer.appendLine(logMessage)
                    writer.flush()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to log file", e)
        }
    }
    
    fun debug(tag: String, message: String) = log("DEBUG", tag, message)
    fun info(tag: String, message: String) = log("INFO", tag, message)
    fun warn(tag: String, message: String) = log("WARN", tag, message)
    fun error(tag: String, message: String) = log("ERROR", tag, message)
    
    fun getLogFile(): File? = logFile
    
    fun clearLogs() {
        try {
            logFile?.delete()
            logFile?.createNewFile()
            log("INFO", "AppLogger", "Logs cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear logs", e)
        }
    }
    
    fun getLogContent(): String {
        return try {
            logFile?.readText() ?: "No log file found"
        } catch (e: Exception) {
            "Error reading log file: ${e.message}"
        }
    }
    
    fun exportLogs(context: Context, callback: (Boolean, String?) -> Unit) {
        try {
            val logContent = getLogContent()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val exportFile = File(context.getExternalFilesDir(null), "jcring_logs_$timestamp.txt")
            
            exportFile.writeText(logContent)
            
            log("INFO", "AppLogger", "Logs exported to: ${exportFile.absolutePath}")
            callback(true, exportFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export logs", e)
            callback(false, e.message)
        }
    }
}