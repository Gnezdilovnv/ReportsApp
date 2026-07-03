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
            val reports = withContext(Dispatchers.IO) {
                db.reportDao().getAll()
            }
            
            if (reports.isEmpty()) {
                Toast.makeText(this@ReportsListActivity, "Нет отчетов", Toast.LENGTH_SHORT).show()
            } else {
                val items = reports.map { report ->
                    val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(report.createdAt))
                    "${report.title} ($date)"
                }
                val adapter = ArrayAdapter(this@ReportsListActivity, android.R.layout.simple_list_item_1, items)
                listView.adapter = adapter
                
                listView.setOnItemClickListener { _, _, position, _ ->
                    val report = reports[position]
                    Toast.makeText(
                        this@ReportsListActivity,
                        "Данные: ${report.data}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                Logger.writeLog("Loaded ${reports.size} reports")
            }
        }
    }
}
