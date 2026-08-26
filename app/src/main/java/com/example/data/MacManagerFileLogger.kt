package com.example.data

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MacManagerFileLogger {
    private const val LOG_FILE_NAME = "mac_manager_logs.txt"

    private fun getLogFile(context: Context): File {
        return File(context.filesDir, LOG_FILE_NAME)
    }

    fun log(
        context: Context,
        action: String,
        isSuccess: Boolean,
        interfaceUsed: String,
        macAddress: String,
        message: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val dateStr = sdf.format(Date(timestamp))
            val statusStr = if (isSuccess) "SUCCESS" else "FAILED"
            val logLine = "[$dateStr] [$statusStr] [$action] Interface: $interfaceUsed | MAC: $macAddress | Message: $message\n"
            
            val file = getLogFile(context)
            file.appendText(logLine)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun readLogs(context: Context): String {
        return try {
            val file = getLogFile(context)
            if (file.exists()) file.readText() else "No logs recorded yet."
        } catch (e: Exception) {
            "Error reading log file: ${e.localizedMessage}"
        }
    }

    fun clearLogs(context: Context): Boolean {
        return try {
            val file = getLogFile(context)
            if (file.exists()) file.delete() else true
        } catch (e: Exception) {
            false
        }
    }

    fun getLogFilePath(context: Context): String {
        return getLogFile(context).absolutePath
    }
}
