package com.example.reports.ui

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reports.R
import com.example.reports.data.AppDatabase
import com.example.reports.data.Category
import com.example.reports.ui.adapters.CategoriesAdapter
import com.example.reports.utils.Logger
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.*

class CategoriesActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CategoriesAdapter
    private val db by lazy { AppDatabase.getDatabase(this) }
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.writeLog("CategoriesActivity onCreate started")
        
        try {
            setContentView(R.layout.activity_categories)
            Logger.writeLog("CategoriesActivity layout set")

            recyclerView = findViewById(R.id.recyclerViewCategories)
            recyclerView.layoutManager = LinearLayoutManager(this)

            adapter = CategoriesAdapter(
                onEdit = { category ->
                    Logger.writeLog("Edit category: ${category.name}")
                    showEditCategoryDialog(category)
                },
                onDelete = { category ->
                    Logger.writeLog("Delete category: ${category.name}")
                    deleteCategory(category)
                }
            )
            recyclerView.adapter = adapter

            findViewById<FloatingActionButton>(R.id.fabAddCategory).setOnClickListener {
                Logger.writeLog("Add category button clicked")
                showAddCategoryDialog()
            }

            loadCategories()
            Logger.writeLog("CategoriesActivity initialized successfully")
            
        } catch (e: Exception) {
            Logger.writeError("CategoriesActivity onCreate error", e)
            Toast.makeText(this, "Ошибка загрузки категорий", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadCategories() {
        try {
            scope.launch {
                val categories = withContext(Dispatchers.IO) {
                    db.categoryDao().getAll()
                }
                Logger.writeLog("Loaded ${categories.size} categories")
                adapter.submitList(categories)
            }
        } catch (e: Exception) {
            Logger.writeError("loadCategories error", e)
        }
    }

    private fun showAddCategoryDialog() {
        try {
            val input = EditText(this)
            input.hint = "Название категории"

            MaterialAlertDialogBuilder(this)
                .setTitle("Новая категория")
                .setView(input)
                .setPositiveButton("Создать") { _, _ ->
                    val name = input.text.toString().trim()
                    if (name.isNotEmpty()) {
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    db.categoryDao().insert(Category(name = name))
                                }
                                Logger.writeLog("Category created: $name")
                                loadCategories()
                                Toast.makeText(this@CategoriesActivity, "Категория создана", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Logger.writeError("Failed to create category", e)
                                Toast.makeText(this@CategoriesActivity, "Ошибка создания", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        } catch (e: Exception) {
            Logger.writeError("showAddCategoryDialog error", e)
        }
    }

    private fun showEditCategoryDialog(category: Category) {
        try {
            val input = EditText(this)
            input.setText(category.name)

            MaterialAlertDialogBuilder(this)
                .setTitle("Редактировать категорию")
                .setView(input)
                .setPositiveButton("Сохранить") { _, _ ->
                    val name = input.text.toString().trim()
                    if (name.isNotEmpty()) {
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    db.categoryDao().update(category.copy(name = name))
                                }
                                Logger.writeLog("Category updated: ${category.name} -> $name")
                                loadCategories()
                                Toast.makeText(this@CategoriesActivity, "Категория обновлена", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Logger.writeError("Failed to update category", e)
                                Toast.makeText(this@CategoriesActivity, "Ошибка обновления", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        } catch (e: Exception) {
            Logger.writeError("showEditCategoryDialog error", e)
        }
    }

    private fun deleteCategory(category: Category) {
        try {
            MaterialAlertDialogBuilder(this)
                .setTitle("Удалить категорию?")
                .setMessage("Все отчеты в этой категории будут удалены")
                .setPositiveButton("Удалить") { _, _ ->
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                db.categoryDao().delete(category)
                            }
                            Logger.writeLog("Category deleted: ${category.name}")
                            loadCategories()
                            Toast.makeText(this@CategoriesActivity, "Категория удалена", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Logger.writeError("Failed to delete category", e)
                            Toast.makeText(this@CategoriesActivity, "Ошибка удаления", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        } catch (e: Exception) {
            Logger.writeError("deleteCategory error", e)
        }
    }
}
