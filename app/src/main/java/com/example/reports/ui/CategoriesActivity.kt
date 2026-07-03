package com.example.reports.ui

import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.reports.R
import com.example.reports.data.AppDatabase
import com.example.reports.data.Category
import com.example.reports.utils.Logger
import kotlinx.coroutines.*

class CategoriesActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private val db by lazy { AppDatabase.getDatabase(this) }
    private val scope = CoroutineScope(Dispatchers.Main)
    private var categories = listOf<Category>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)
        
        Logger.writeLog("CategoriesActivity started")
        
        listView = findViewById(R.id.listViewCategories)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listView.adapter = adapter
        
        // Клик для редактирования
        listView.setOnItemClickListener { _, _, position, _ ->
            val category = categories[position]
            showEditDialog(category)
        }
        
        // Долгий клик для удаления
        listView.setOnItemLongClickListener { _, _, position, _ ->
            val category = categories[position]
            showDeleteDialog(category)
            true
        }
        
        findViewById<Button>(R.id.btnAddCategory).setOnClickListener {
            showAddDialog()
        }
        
        loadCategories()
    }

    private fun loadCategories() {
        scope.launch {
            categories = withContext(Dispatchers.IO) {
                db.categoryDao().getAll()
            }
            val names = categories.map { "${it.name} (${it.createdAt})" }
            adapter.clear()
            adapter.addAll(names)
            adapter.notifyDataSetChanged()
            Logger.writeLog("Loaded ${categories.size} categories")
        }
    }

    private fun showAddDialog() {
        val input = EditText(this)
        input.hint = "Название категории"
        
        AlertDialog.Builder(this)
            .setTitle("Новая категория")
            .setView(input)
            .setPositiveButton("Создать") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            db.categoryDao().insert(Category(name = name))
                        }
                        Logger.writeLog("Category created: $name")
                        loadCategories()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showEditDialog(category: Category) {
        val input = EditText(this)
        input.setText(category.name)
        
        AlertDialog.Builder(this)
            .setTitle("Редактировать категорию")
            .setView(input)
            .setPositiveButton("Сохранить") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            db.categoryDao().update(category.copy(name = name))
                        }
                        Logger.writeLog("Category updated: ${category.name} -> $name")
                        loadCategories()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showDeleteDialog(category: Category) {
        AlertDialog.Builder(this)
            .setTitle("Удалить категорию?")
            .setMessage("Все отчеты в этой категории будут удалены")
            .setPositiveButton("Удалить") { _, _ ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        db.categoryDao().delete(category)
                    }
                    Logger.writeLog("Category deleted: ${category.name}")
                    loadCategories()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
