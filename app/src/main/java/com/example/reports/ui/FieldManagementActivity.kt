package com.example.reports.ui

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reports.R
import com.example.reports.data.AppDatabase
import com.example.reports.data.Field
import com.example.reports.data.FieldType
import com.example.reports.ui.adapters.FieldsAdapter
import com.example.reports.utils.Logger
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.*

class FieldManagementActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FieldsAdapter
    private val db by lazy { AppDatabase.getDatabase(this) }
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.writeLog("FieldManagementActivity onCreate started")
        
        try {
            setContentView(R.layout.activity_field_management)
            Logger.writeLog("FieldManagementActivity layout set")

            recyclerView = findViewById(R.id.recyclerViewFields)
            recyclerView.layoutManager = LinearLayoutManager(this)

            adapter = FieldsAdapter(
                onEdit = { field ->
                    Logger.writeLog("Edit field: ${field.name}")
                    showEditFieldDialog(field)
                },
                onDelete = { field ->
                    Logger.writeLog("Delete field: ${field.name}")
                    deleteField(field)
                }
            )
            recyclerView.adapter = adapter

            findViewById<FloatingActionButton>(R.id.fabAddField).setOnClickListener {
                Logger.writeLog("Add field button clicked")
                showAddFieldDialog()
            }

            loadFields()
            Logger.writeLog("FieldManagementActivity initialized successfully")
            
        } catch (e: Exception) {
            Logger.writeError("FieldManagementActivity onCreate error", e)
            Toast.makeText(this, "Ошибка загрузки полей", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadFields() {
        try {
            scope.launch {
                val fields = withContext(Dispatchers.IO) {
                    db.fieldDao().getAll()
                }
                Logger.writeLog("Loaded ${fields.size} fields")
                adapter.submitList(fields)
            }
        } catch (e: Exception) {
            Logger.writeError("loadFields error", e)
        }
    }

    private fun showAddFieldDialog() {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_field, null)
            val etName = dialogView.findViewById<EditText>(R.id.etFieldName)
            val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerFieldType)
            val etCategories = dialogView.findViewById<EditText>(R.id.etCategoryIds)

            val typeNames = FieldType.values().map { it.name }
            val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, typeNames)
            typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerType.adapter = typeAdapter

            MaterialAlertDialogBuilder(this)
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
                                    db.fieldDao().insert(
                                        Field(
                                            name = name,
                                            type = type,
                                            categoryIds = categoryIds
                                        )
                                    )
                                }
                                Logger.writeLog("Field created: $name")
                                loadFields()
                                Toast.makeText(this@FieldManagementActivity, "Поле создано", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Logger.writeError("Failed to create field", e)
                                Toast.makeText(this@FieldManagementActivity, "Ошибка создания", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        } catch (e: Exception) {
            Logger.writeError("showAddFieldDialog error", e)
        }
    }

    private fun showEditFieldDialog(field: Field) {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_field, null)
            val etName = dialogView.findViewById<EditText>(R.id.etFieldName)
            val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerFieldType)
            val etCategories = dialogView.findViewById<EditText>(R.id.etCategoryIds)

            etName.setText(field.name)
            spinnerType.setSelection(field.type.ordinal)
            etCategories.setText(field.categoryIds.joinToString(","))

            MaterialAlertDialogBuilder(this)
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
                                    db.fieldDao().update(
                                        field.copy(
                                            name = name,
                                            type = type,
                                            categoryIds = categoryIds
                                        )
                                    )
                                }
                                Logger.writeLog("Field updated: ${field.name} -> $name")
                                loadFields()
                                Toast.makeText(this@FieldManagementActivity, "Поле обновлено", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Logger.writeError("Failed to update field", e)
                                Toast.makeText(this@FieldManagementActivity, "Ошибка обновления", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        } catch (e: Exception) {
            Logger.writeError("showEditFieldDialog error", e)
        }
    }

    private fun deleteField(field: Field) {
        try {
            MaterialAlertDialogBuilder(this)
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
                            Logger.writeError("Failed to delete field", e)
                            Toast.makeText(this@FieldManagementActivity, "Ошибка удаления", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        } catch (e: Exception) {
            Logger.writeError("deleteField error", e)
        }
    }
}
