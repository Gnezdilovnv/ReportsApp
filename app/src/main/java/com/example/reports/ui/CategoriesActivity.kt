package com.example.reports.ui

import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reports.R
import com.example.reports.data.AppDatabase
import com.example.reports.data.Category
import com.example.reports.ui.adapters.CategoriesAdapter
import kotlinx.coroutines.*

class CategoriesActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CategoriesAdapter
    private val db by lazy { AppDatabase.getDatabase(this) }
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)
        
        recyclerView = findViewById(R.id.recyclerViewCategories)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = CategoriesAdapter(
            onEdit = { category ->
                showEditCategoryDialog(category)
            },
            onDelete = { category ->
                showDeleteCategoryDialog(category)
            }
        )
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.btnAddCategory).setOnClickListener {
            showAddCategoryDialog()
        }

        loadCategories()
    }

    private fun loadCategories() {
        scope.launch {
            try {
                val categories = withContext(Dispatchers.IO) {
                    db.categoryDao().getAll()
                }
                adapter.submitList(categories)
            } catch (e: Exception) {
                Toast.makeText(this@CategoriesActivity, "Ошибка загрузки", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddCategoryDialog() {
        val input = EditText(this)
        input.hint = "Название категории"

        AlertDialog.Builder(this)
            .setTitle("Новая категория")
            .setView(input)
            .setPositiveButton("Создать") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            db.categoryDao().insert(Category(name = name))
                        }
                        loadCategories()
                        Toast.makeText(this@CategoriesActivity, "Категория создана", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@CategoriesActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showEditCategoryDialog(category: Category) {
        val input = EditText(this)
        input.setText(category.name)

        AlertDialog.Builder(this)
            .setTitle("Редактировать категорию")
            .setView(input)
            .setPositiveButton("Сохранить") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            db.categoryDao().update(category.copy(name = name))
                        }
                        loadCategories()
                        Toast.makeText(this@CategoriesActivity, "Категория обновлена", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@CategoriesActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showDeleteCategoryDialog(category: Category) {
        AlertDialog.Builder(this)
            .setTitle("Удалить категорию?")
            .setPositiveButton("Удалить") { _, _ ->
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            db.categoryDao().delete(category)
                        }
                        loadCategories()
                        Toast.makeText(this@CategoriesActivity, "Категория удалена", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@CategoriesActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
