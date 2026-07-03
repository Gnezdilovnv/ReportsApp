package com.example.reports.ui

import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.reports.R
import com.example.reports.data.AppDatabase
import com.example.reports.data.Report
import com.example.reports.utils.Logger
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class CreateReportActivity : AppCompatActivity() {
    private lateinit var spinnerCategory: Spinner
    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private val db by lazy { AppDatabase.getDatabase(this) }
    private val scope = CoroutineScope(Dispatchers.Main)
    private var categories = listOf<com.example.reports.data.Category>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_report)
        
        Logger.writeLog("CreateReportActivity started")
        
        spinnerCategory = findViewById(R.id.spinnerCategory)
        etTitle = findViewById(R.id.etTitle)
        etDescription = findViewById(R.id.etDescription)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)
        
        loadCategories()
        
        btnSubmit.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val desc = etDescription.text.toString().trim()
            
            if (title.isEmpty()) {
                Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (categories.isEmpty()) {
                Toast.makeText(this, "Сначала создайте категорию", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val categoryName = spinnerCategory.selectedItem.toString()
            val category = categories.find { it.name == categoryName }
            
            if (category != null) {
                scope.launch {
                    try {
                        val report = Report(
                            categoryId = category.id,
                            title = title,
                            data = mapOf("description" to desc),
                            createdAt = System.currentTimeMillis()
                        )
                        withContext(Dispatchers.IO) {
                            db.reportDao().insert(report)
                        }
                        Logger.writeLog("Report saved: $title")
                        Toast.makeText(this@CreateReportActivity, "Отчет сохранен!", Toast.LENGTH_SHORT).show()
                        finish()
                    } catch (e: Exception) {
                        Logger.writeError("Save error", e)
                        Toast.makeText(this@CreateReportActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun loadCategories() {
        scope.launch {
            categories = withContext(Dispatchers.IO) {
                db.categoryDao().getAll()
            }
            
            val names = categories.map { it.name }
            val adapter = ArrayAdapter(
                this@CreateReportActivity,
                android.R.layout.simple_spinner_item,
                names
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategory.adapter = adapter
            
            if (categories.isEmpty()) {
                Toast.makeText(this@CreateReportActivity, "Нет категорий. Создайте их в настройках.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
