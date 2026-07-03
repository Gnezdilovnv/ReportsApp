package com.example.reports.utils

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object Logger {
    private const val LOG_FILE_NAME = "reports_app_log.txt"
    private var logFile: File? = null
    private var isInitialized = false

    fun init(context: Context) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir != null && (downloadsDir.exists() || downloadsDir.mkdirs())) {
                logFile = File(downloadsDir, LOG_FILE_NAME)
                if (logFile!!.exists()) {
                    logFile!!.delete()
                }
                logFile!!.createNewFile()
                isInitialized = true
                writeLog("=== REPORTS APP STARTED ===")
                writeLog("App initialized successfully")
            }
        } catch (e: Exception) {
            android.util.Log.e("Logger", "Init error: ${e.message}")
        }
    }

    fun writeLog(message: String) {
        if (!isInitialized || logFile == null) return
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            val logMessage = "[$timestamp] $message\n"
            FileWriter(logFile, true).use { writer ->
                writer.append(logMessage)
                writer.flush()
            }
            android.util.Log.d("ReportsApp", message)
        } catch (e: Exception) {
            android.util.Log.e("Logger", "Write error: ${e.message}")
        }
    }

    fun writeError(message: String, throwable: Throwable? = null) {
        val errorMsg = if (throwable != null) {
            "$message: ${throwable.message}\n${throwable.stackTraceToString()}"
        } else {
            "ERROR: $message"
        }
        writeLog(errorMsg)
        android.util.Log.e("ReportsApp", errorMsg)
    }
}
