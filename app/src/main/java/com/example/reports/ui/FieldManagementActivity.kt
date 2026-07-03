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
import com.example.reports.data.FieldCategoryRelation
import com.example.reports.data.FieldType
import com.example.reports.ui.adapters.FieldsAdapter
import kotlinx.coroutines.*

class FieldManagementActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FieldsAdapter
    private val db by lazy { AppDatabase.getDatabase(this) }
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fields)
        
        recyclerView = findViewById(R.id.recyclerViewFields)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = FieldsAdapter(
            onEdit = { field -> showEditFieldDialog(field) },
            onDelete = { field -> showDeleteFieldDialog(field) }
        )
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.btnAddField).setOnClickListener {
            showAddFieldDialog()
        }

        loadFields()
    }

    private fun loadFields() {
        scope.launch {
            try {
                val fields = withContext(Dispatchers.IO) {
                    db.fieldDao().getAll()
                }
                adapter.submitList(fields)
            } catch (e: Exception) {
                Toast.makeText(this@FieldManagementActivity, "Ошибка загрузки", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCategorySelector(
        title: String = "Выберите категории",
        selectedIds: List<String> = emptyList(),
        onSelected: (List<String>) -> Unit
    ) {
        scope.launch {
            try {
                val categories = withContext(Dispatchers.IO) {
                    db.categoryDao().getAll()
                }
                
                if (categories.isEmpty()) {
                    Toast.makeText(this@FieldManagementActivity, "Сначала создайте категории", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                val checkedItems = BooleanArray(categories.size) { index ->
                    selectedIds.contains(categories[index].id)
                }
                val names = categories.map { it.name }.toTypedArray()
                
                AlertDialog.Builder(this@FieldManagementActivity)
                    .setTitle(title)
                    .setMultiChoiceItems(names, checkedItems) { _, which, isChecked ->
                        checkedItems[which] = isChecked
                    }
                    .setPositiveButton("OK") { _, _ ->
                        val selected = checkedItems.mapIndexed { index, checked ->
                            if (checked) categories[index].id else null
                        }.filterNotNull()
                        onSelected(selected)
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(this@FieldManagementActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddFieldDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_field, null)
        val etName = dialogView.findViewById<EditText>(R.id.etFieldName)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerFieldType)
        val btnSelectCategories = dialogView.findViewById<Button>(R.id.btnSelectCategories)
        val chkRequired = dialogView.findViewById<CheckBox>(R.id.chkRequired)
        
        val typeNames = FieldType.values().map { it.name }
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, typeNames)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = typeAdapter
        
        var selectedCategoryIds = mutableListOf<String>()
        
        btnSelectCategories.setOnClickListener {
            showCategorySelector("Выберите категории для поля", selectedCategoryIds) { selected ->
                selectedCategoryIds = selected.toMutableList()
                val names = selected.map { it.take(8) }.joinToString(", ")
                btnSelectCategories.text = if (names.isEmpty()) "Выбрать категории" else "Выбрано: $names"
            }
        }
        
        AlertDialog.Builder(this)
            .setTitle("Новое поле")
            .setView(dialogView)
            .setPositiveButton("Создать") { _, _ ->
                val name = etName.text.toString().trim()
                val type = FieldType.values()[spinnerType.selectedItemPosition]
                val isRequired = chkRequired.isChecked
                
                if (name.isEmpty()) {
                    Toast.makeText(this, "Введите название поля", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                if (selectedCategoryIds.isEmpty()) {
                    Toast.makeText(this, "Выберите категории", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                scope.launch {
                    try {
                        val field = Field(
                            name = name,
                            type = type,
                            isRequired = isRequired
                        )
                        
                        withContext(Dispatchers.IO) {
                            db.fieldDao().insert(field)
                            selectedCategoryIds.forEach { categoryId ->
                                db.fieldDao().insertRelation(
                                    FieldCategoryRelation(
                                        fieldId = field.id,
                                        categoryId = categoryId
                                    )
                                )
                            }
                        }
                        
                        loadFields()
                        Toast.makeText(this@FieldManagementActivity, "Поле создано", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@FieldManagementActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showEditFieldDialog(field: Field) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_field, null)
        val etName = dialogView.findViewById<EditText>(R.id.etFieldName)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerFieldType)
        val btnSelectCategories = dialogView.findViewById<Button>(R.id.btnSelectCategories)
        val chkRequired = dialogView.findViewById<CheckBox>(R.id.chkRequired)
        
        etName.setText(field.name)
        spinnerType.setSelection(field.type.ordinal)
        chkRequired.isChecked = field.isRequired
        
        var selectedCategoryIds = mutableListOf<String>()
        
        // Загружаем текущие категории для поля
        scope.launch {
            try {
                val relations = withContext(Dispatchers.IO) {
                    db.fieldDao().getByCategoryId(field.id)
                }
                selectedCategoryIds = relations.map { it.id }.toMutableList()
                btnSelectCategories.text = "Выбрано: ${relations.size} категорий"
            } catch (e: Exception) {
                // игнорируем
            }
        }
        
        btnSelectCategories.setOnClickListener {
            showCategorySelector("Выберите категории для поля", selectedCategoryIds) { selected ->
                selectedCategoryIds = selected.toMutableList()
                val names = selected.map { it.take(8) }.joinToString(", ")
                btnSelectCategories.text = if (names.isEmpty()) "Выбрать категории" else "Выбрано: $names"
            }
        }
        
        AlertDialog.Builder(this)
            .setTitle("Редактировать поле")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val name = etName.text.toString().trim()
                val type = FieldType.values()[spinnerType.selectedItemPosition]
                val isRequired = chkRequired.isChecked
                
                if (name.isEmpty()) {
                    Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            db.fieldDao().update(field.copy(
                                name = name,
                                type = type,
                                isRequired = isRequired
                            ))
                            db.fieldDao().deleteRelations(field.id)
                            selectedCategoryIds.forEach { categoryId ->
                                db.fieldDao().insertRelation(
                                    FieldCategoryRelation(
                                        fieldId = field.id,
                                        categoryId = categoryId
                                    )
                                )
                            }
                        }
                        loadFields()
                        Toast.makeText(this@FieldManagementActivity, "Поле обновлено", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@FieldManagementActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showDeleteFieldDialog(field: Field) {
        AlertDialog.Builder(this)
            .setTitle("Удалить поле?")
            .setPositiveButton("Удалить") { _, _ ->
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            db.fieldDao().deleteRelations(field.id)
                            db.fieldDao().delete(field)
                        }
                        loadFields()
                        Toast.makeText(this@FieldManagementActivity, "Поле удалено", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@FieldManagementActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
