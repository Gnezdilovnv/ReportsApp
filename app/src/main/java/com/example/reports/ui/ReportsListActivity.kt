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

class ReportsListActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private val db by lazy { AppDatabase.getDatabase(this) }
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.writeLog("ReportsListActivity onCreate started")
        
        try {
            setContentView(R.layout.activity_reports_list)
            Logger.writeLog("ReportsListActivity layout set")

            listView = findViewById(R.id.listViewReports)
            loadReports()
            Logger.writeLog("ReportsListActivity initialized successfully")
            
        } catch (e: Exception) {
            Logger.writeError("ReportsListActivity onCreate error", e)
            Toast.makeText(this, "Ошибка загрузки отчетов", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadReports() {
        try {
            scope.launch {
                val reports = withContext(Dispatchers.IO) {
                    db.reportDao().getAll()
                }
                
                Logger.writeLog("Loaded ${reports.size} reports")

                if (reports.isEmpty()) {
                    Toast.makeText(this@ReportsListActivity, "Нет отчетов", Toast.LENGTH_SHORT).show()
                } else {
                    val titles = reports.map { 
                        "${it.title} (${java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).format(it.createdAt)})" 
                    }
                    val adapter = ArrayAdapter(this@ReportsListActivity, android.R.layout.simple_list_item_1, titles)
                    listView.adapter = adapter
                    
                    listView.setOnItemClickListener { _, _, position, _ ->
                        val report = reports[position]
                        Logger.writeLog("Report clicked: ${report.title}")
                        Toast.makeText(
                            this@ReportsListActivity,
                            "Отчет: ${report.title}\nДанные: ${report.data}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        } catch (e: Exception) {
            Logger.writeError("loadReports error", e)
        }
    }
}
