package com.example.reports.ui

import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.reports.R
import com.example.reports.utils.Logger

class LogsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logs)
        
        val tvLogs = findViewById<TextView>(R.id.tvLogs)
        val btnRefresh = findViewById<Button>(R.id.btnRefresh)
        val btnClear = findViewById<Button>(R.id.btnClear)
        val scrollView = findViewById<ScrollView>(R.id.scrollView)
        
        loadLogs(tvLogs, scrollView)
        
        btnRefresh.setOnClickListener {
            loadLogs(tvLogs, scrollView)
            Toast.makeText(this, "Логи обновлены", Toast.LENGTH_SHORT).show()
        }
        
        btnClear.setOnClickListener {
            val logFile = Logger.getLogFile()
            if (logFile != null && logFile.exists()) {
                logFile.delete()
                logFile.createNewFile()
                tvLogs.text = "Логи очищены"
                Toast.makeText(this, "Логи очищены", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun loadLogs(tvLogs: TextView, scrollView: ScrollView) {
        val content = Logger.getLogContent()
        tvLogs.text = if (content.isEmpty()) "Логов нет" else content
        scrollView.post {
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }
}
