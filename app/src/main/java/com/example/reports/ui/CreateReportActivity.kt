package com.example.reports.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.reports.R
import com.example.reports.data.AppDatabase
import com.example.reports.data.Report
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class CreateReportActivity : AppCompatActivity() {
    private lateinit var spinnerCategory: Spinner
    private lateinit var container: LinearLayout
    private lateinit var etTitle: EditText
    private val db by lazy { AppDatabase.getDatabase(this) }
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_report)
        
        spinnerCategory = findViewById(R.id.spinnerCategory)
        container = findViewById(R.id.fieldsContainer)
        etTitle = findViewById(R.id.etTitle)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)
        
        loadCategories()
        
        btnSubmit.setOnClickListener {
            saveReport()
        }
    }

    private fun loadCategories() {
        scope.launch {
            try {
                val categories = withContext(Dispatchers.IO) {
                    db.categoryDao().getAll()
                }
                
                if (categories.isEmpty()) {
                    Toast.makeText(this@CreateReportActivity, "Создайте категорию", Toast.LENGTH_LONG).show()
                    return@launch
                }
                
                val adapter = ArrayAdapter(
                    this@CreateReportActivity,
                    android.R.layout.simple_spinner_item,
                    categories.map { it.name }
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerCategory.adapter = adapter
            } catch (e: Exception) {
                Toast.makeText(this@CreateReportActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveReport() {
        val title = etTitle.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show()
            return
        }
        
        val categoryName = spinnerCategory.selectedItem?.toString() ?: ""
        if (categoryName.isEmpty()) {
            Toast.makeText(this, "Выберите категорию", Toast.LENGTH_SHORT).show()
            return
        }
        
        scope.launch {
            try {
                val categories = withContext(Dispatchers.IO) {
                    db.categoryDao().getAll()
                }
                val category = categories.find { it.name == categoryName }
                
                if (category == null) {
                    Toast.makeText(this@CreateReportActivity, "Категория не найдена", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                val report = Report(
                    categoryId = category.id,
                    title = title,
                    data = emptyMap()
                )
                
                withContext(Dispatchers.IO) {
                    db.reportDao().insert(report)
                }
                
                Toast.makeText(this@CreateReportActivity, "Отчет сохранен!", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@CreateReportActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
