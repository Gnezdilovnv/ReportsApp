package com.example.reports.ui

import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private var allCategories = listOf<com.example.reports.data.Category>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_field_management)
        
        Logger.writeLog("FieldManagementActivity started")
        
        listView = findViewById(R.id.listViewFields)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listView.adapter = adapter
        
        listView.setOnItemClickListener { _, _, position, _ ->
            val field = fields[position]
            showEditDialog(field)
        }
        
        listView.setOnItemLongClickListener { _, _, position, _ ->
            val field = fields[position]
            showDeleteDialog(field)
            true
        }
        
        findViewById<Button>(R.id.btnAddField).setOnClickListener {
            showAddDialog()
        }
        
        loadCategories()
        loadFields()
    }

    private fun loadCategories() {
        scope.launch {
            allCategories = withContext(Dispatchers.IO) {
                db.categoryDao().getAll()
            }
            Logger.writeLog("Loaded ${allCategories.size} categories")
        }
    }

    private fun loadFields() {
        scope.launch {
            fields = withContext(Dispatchers.IO) {
                db.fieldDao().getAll()
            }
            val items = fields.map { 
                val categoryNames = it.categoryIds.map { id ->
                    allCategories.find { cat -> cat.id == id }?.name ?: id
                }.joinToString(", ")
                "${it.name} (${it.type.name}) → ${if (categoryNames.isEmpty()) "нет категорий" else categoryNames}"
            }
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
        val btnSelectCategories = dialogView.findViewById<Button>(R.id.btnSelectCategories)
        
        val typeNames = FieldType.values().map { it.name }
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, typeNames)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = typeAdapter
        
        var selectedCategoryIds = mutableListOf<String>()
        
        btnSelectCategories.setOnClickListener {
            showCategorySelector { selectedIds ->
                selectedCategoryIds = selectedIds.toMutableList()
                val names = selectedIds.map { id ->
                    allCategories.find { it.id == id }?.name ?: id
                }.joinToString(", ")
                btnSelectCategories.text = if (names.isEmpty()) "Выбрать категории" else "Выбрано: $names"
            }
        }
        
        AlertDialog.Builder(this)
            .setTitle("Новое поле")
            .setView(dialogView)
            .setPositiveButton("Создать") { _, _ ->
                val name = etName.text.toString().trim()
                val type = FieldType.values()[spinnerType.selectedItemPosition]
                
                if (name.isNotEmpty()) {
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                db.fieldDao().insert(Field(
                                    name = name,
                                    type = type,
                                    categoryIds = selectedCategoryIds
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

    private fun showCategorySelector(onSelected: (List<String>) -> Unit) {
        if (allCategories.isEmpty()) {
            Toast.makeText(this, "Сначала создайте категории", Toast.LENGTH_SHORT).show()
            return
        }
        
        val checkedItems = BooleanArray(allCategories.size) { false }
        val categoryNames = allCategories.map { it.name }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Выберите категории")
            .setMultiChoiceItems(categoryNames, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("OK") { _, _ ->
                val selected = checkedItems.mapIndexed { index, isChecked ->
                    if (isChecked) allCategories[index].id else null
                }.filterNotNull()
                onSelected(selected)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showEditDialog(field: Field) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_field, null)
        val etName = dialogView.findViewById<EditText>(R.id.etFieldName)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerFieldType)
        val btnSelectCategories = dialogView.findViewById<Button>(R.id.btnSelectCategories)
        
        etName.setText(field.name)
        spinnerType.setSelection(field.type.ordinal)
        
        var selectedCategoryIds = field.categoryIds.toMutableList()
        
        val names = selectedCategoryIds.map { id ->
            allCategories.find { it.id == id }?.name ?: id
        }.joinToString(", ")
        btnSelectCategories.text = if (names.isEmpty()) "Выбрать категории" else "Выбрано: $names"
        
        btnSelectCategories.setOnClickListener {
            showCategorySelector { selectedIds ->
                selectedCategoryIds = selectedIds.toMutableList()
                val newNames = selectedIds.map { id ->
                    allCategories.find { it.id == id }?.name ?: id
                }.joinToString(", ")
                btnSelectCategories.text = if (newNames.isEmpty()) "Выбрать категории" else "Выбрано: $newNames"
            }
        }
        
        AlertDialog.Builder(this)
            .setTitle("Редактировать поле")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val name = etName.text.toString().trim()
                val type = FieldType.values()[spinnerType.selectedItemPosition]
                
                if (name.isNotEmpty()) {
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                db.fieldDao().update(field.copy(
                                    name = name,
                                    type = type,
                                    categoryIds = selectedCategoryIds
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
