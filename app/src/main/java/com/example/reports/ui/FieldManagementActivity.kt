package com.example.reports.ui

import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.reports.R
import com.example.reports.data.AppDatabase
import com.example.reports.data.Field
import com.example.reports.data.FieldType
import com.example.reports.utils.Logger
import kotlinx.coroutines.*

class FieldManagementActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private val db by lazy { AppDatabase.getDatabase(this) }
    private val scope = CoroutineScope(Dispatchers.Main)
    private var fields = listOf<Field>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_field_management)
        
        Logger.writeLog("FieldManagementActivity started")
        
        listView = findViewById(R.id.listViewFields)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listView.adapter = adapter
        
        // Клик для редактирования
        listView.setOnItemClickListener { _, _, position, _ ->
            val field = fields[position]
            showEditDialog(field)
        }
        
        // Долгий клик для удаления
        listView.setOnItemLongClickListener { _, _, position, _ ->
            val field = fields[position]
            showDeleteDialog(field)
            true
        }
        
        findViewById<Button>(R.id.btnAddField).setOnClickListener {
            showAddDialog()
        }
        
        loadFields()
    }

    private fun loadFields() {
        scope.launch {
            fields = withContext(Dispatchers.IO) {
                db.fieldDao().getAll()
            }
            val items = fields.map { "${it.name} (${it.type.name}) - ${it.categoryIds}" }
            adapter.clear()
            adapter.addAll(items)
            adapter.notifyDataSetChanged()
            Logger.writeLog("Loaded ${fields.size} fields")
        }
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_field, null)
        val etName = dialogView.findViewById<EditText>(R.id.etFieldName)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerFieldType)
        val etCategories = dialogView.findViewById<EditText>(R.id.etCategoryIds)
        
        val typeNames = FieldType.values().map { it.name }
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, typeNames)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = typeAdapter
        
        AlertDialog.Builder(this)
            .setTitle("Новое поле")
            .setView(dialogView)
            .setPositiveButton("Создать") { _, _ ->
                val name = etName.text.toString().trim()
                val type = FieldType.values()[spinnerType.selectedItemPosition]
                val categoryIds = etCategories.text.toString()
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                
                if (name.isNotEmpty()) {
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                db.fieldDao().insert(Field(
                                    name = name,
                                    type = type,
                                    categoryIds = categoryIds
                                ))
                            }
                            Logger.writeLog("Field created: $name")
                            loadFields()
                            Toast.makeText(this@FieldManagementActivity, "Поле создано", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Logger.writeError("Create field error", e)
                            Toast.makeText(this@FieldManagementActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showEditDialog(field: Field) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_field, null)
        val etName = dialogView.findViewById<EditText>(R.id.etFieldName)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerFieldType)
        val etCategories = dialogView.findViewById<EditText>(R.id.etCategoryIds)
        
        etName.setText(field.name)
        spinnerType.setSelection(field.type.ordinal)
        etCategories.setText(field.categoryIds.joinToString(","))
        
        AlertDialog.Builder(this)
            .setTitle("Редактировать поле")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val name = etName.text.toString().trim()
                val type = FieldType.values()[spinnerType.selectedItemPosition]
                val categoryIds = etCategories.text.toString()
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                
                if (name.isNotEmpty()) {
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                db.fieldDao().update(field.copy(
                                    name = name,
                                    type = type,
                                    categoryIds = categoryIds
                                ))
                            }
                            Logger.writeLog("Field updated: ${field.name} -> $name")
                            loadFields()
                            Toast.makeText(this@FieldManagementActivity, "Поле обновлено", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Logger.writeError("Update field error", e)
                            Toast.makeText(this@FieldManagementActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showDeleteDialog(field: Field) {
        AlertDialog.Builder(this)
            .setTitle("Удалить поле?")
            .setPositiveButton("Удалить") { _, _ ->
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            db.fieldDao().delete(field)
                        }
                        Logger.writeLog("Field deleted: ${field.name}")
                        loadFields()
                        Toast.makeText(this@FieldManagementActivity, "Поле удалено", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Logger.writeError("Delete field error", e)
                        Toast.makeText(this@FieldManagementActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
