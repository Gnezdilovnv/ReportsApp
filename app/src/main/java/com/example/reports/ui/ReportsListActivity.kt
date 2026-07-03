package com.example.reports.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.reports.R
import com.example.reports.data.AppDatabase
import com.example.reports.utils.Logger
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class ReportsListActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private val db by lazy { AppDatabase.getDatabase(this) }
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports_list)
        
        Logger.writeLog("ReportsListActivity started")
        
        listView = findViewById(R.id.listViewReports)
        loadReports()
    }

    private fun loadReports() {
        scope.launch {
            try {
                val reports = withContext(Dispatchers.IO) {
                    db.reportDao().getAll()
                }
                
                if (reports.isEmpty()) {
                    Toast.makeText(this@ReportsListActivity, "Нет отчетов", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val items = reports.map { report ->
                    "${report.title} (${format.format(Date(report.createdAt))})"
                }
                
                val adapter = ArrayAdapter(this@ReportsListActivity, android.R.layout.simple_list_item_1, items)
                listView.adapter = adapter
                
                listView.setOnItemClickListener { _, _, position, _ ->
                    val report = reports[position]
                    val data = report.data.map { "${it.key}: ${it.value}" }.joinToString("\n")
                    Toast.makeText(
                        this@ReportsListActivity,
                        "Отчет: ${report.title}\n\nДанные:\n$data",
                        Toast.LENGTH_LONG
                    ).show()
                }
                
                Logger.writeLog("Loaded ${reports.size} reports")
            } catch (e: Exception) {
                Logger.writeError("Load reports error", e)
                Toast.makeText(this@ReportsListActivity, "Ошибка загрузки: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
