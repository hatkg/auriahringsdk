package com.jcring.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object LogExporter {
    
    fun shareLogFile(context: Context, onResult: (Boolean, String?) -> Unit) {
        AppLogger.exportLogs(context) { success, filePath ->
            if (success && filePath != null) {
                try {
                    val file = File(filePath)
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "JCRing App Logs")
                        putExtra(Intent.EXTRA_TEXT, "JCRing App debug logs attached")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    
                    val chooserIntent = Intent.createChooser(intent, "Share Logs")
                    chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooserIntent)
                    
                    onResult(true, "Logs shared successfully")
                } catch (e: Exception) {
                    onResult(false, "Error sharing logs: ${e.message}")
                }
            } else {
                onResult(false, "Failed to export logs: $filePath")
            }
        }
    }
    
    fun getLogContent(): String {
        return AppLogger.getLogContent()
    }
    
    fun addDebugInfo(context: Context) {
        AppLogger.info("LogExporter", "=== DEVICE DEBUG INFO ===")
        AppLogger.info("LogExporter", "App Version: ${getAppVersion(context)}")
        AppLogger.info("LogExporter", "Android Version: ${android.os.Build.VERSION.RELEASE}")
        AppLogger.info("LogExporter", "Device Model: ${android.os.Build.MODEL}")
        AppLogger.info("LogExporter", "Device Manufacturer: ${android.os.Build.MANUFACTURER}")
        AppLogger.info("LogExporter", "Timestamp: ${java.util.Date()}")
        AppLogger.info("LogExporter", "=== END DEBUG INFO ===")
    }
    
    private fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.versionCode})"
        } catch (e: Exception) {
            "Unknown"
        }
    }
}