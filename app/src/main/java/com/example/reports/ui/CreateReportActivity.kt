package com.example.reports.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.reports.R
import com.example.reports.data.*
import com.example.reports.utils.Logger
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class CreateReportActivity : AppCompatActivity() {
    private lateinit var spinnerCategory: Spinner
    private lateinit var fieldsContainer: LinearLayout
    private val db by lazy { AppDatabase.getDatabase(this) }
    private val scope = CoroutineScope(Dispatchers.Main)
    private var selectedCategory: Category? = null
    private val fieldValues = mutableMapOf<String, String>()
    private var lastLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_report)
        
        spinnerCategory = findViewById(R.id.spinnerCategory)
        fieldsContainer = findViewById(R.id.fieldsContainer)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)
        
        loadCategories()
        
        btnSubmit.setOnClickListener {
            validateAndSubmit()
        }
    }

    private fun loadCategories() {
        scope.launch {
            try {
                val categories = withContext(Dispatchers.IO) {
                    db.categoryDao().getAll()
                }
                
                if (categories.isEmpty()) {
                    Toast.makeText(this@CreateReportActivity, "Сначала создайте категорию", Toast.LENGTH_LONG).show()
                    return@launch
                }
                
                val adapter = ArrayAdapter(
                    this@CreateReportActivity,
                    android.R.layout.simple_spinner_item,
                    categories.map { it.name }
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerCategory.adapter = adapter
                
                spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                        selectedCategory = categories[position]
                        loadFieldsForCategory(selectedCategory!!)
                    }
                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
            } catch (e: Exception) {
                Toast.makeText(this@CreateReportActivity, "Ошибка загрузки: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadFieldsForCategory(category: Category) {
        scope.launch {
            try {
                val fields = withContext(Dispatchers.IO) {
                    db.fieldDao().getByCategoryId(category.id)
                }
                
                runOnUiThread {
                    fieldsContainer.removeAllViews()
                    fieldValues.clear()
                    
                    if (fields.isEmpty()) {
                        Toast.makeText(this@CreateReportActivity, "Нет полей для этой категории", Toast.LENGTH_SHORT).show()
                    }
                    
                    fields.forEach { field ->
                        addFieldView(field)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@CreateReportActivity, "Ошибка загрузки полей", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addFieldView(field: Field) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 16)
        }
        
        val label = TextView(this).apply {
            text = if (field.isRequired) "${field.name} *" else field.name
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        container.addView(label)
        
        when (field.type) {
            FieldType.TEXT -> {
                val input = TextInputEditText(this).apply { 
                    hint = "Введите текст"
                }
                container.addView(TextInputLayout(this).apply { addView(input) })
                fieldValues[field.id] = ""
                input.addTextChangedListener(object : android.text.TextWatcher {
                    override fun afterTextChanged(s: android.text.Editable?) {
                        fieldValues[field.id] = s.toString()
                    }
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                })
            }
            
            FieldType.DATE_TIME -> {
                val button = Button(this).apply {
                    text = "Выбрать дату и время"
                    setOnClickListener {
                        showDateTimePicker { datetime ->
                            text = datetime
                            fieldValues[field.id] = datetime
                        }
                    }
                }
                container.addView(button)
                fieldValues[field.id] = ""
            }
            
            FieldType.LOCATION -> {
                val button = Button(this).apply {
                    text = "Получить координаты"
                    setOnClickListener {
                        if (checkLocationPermission()) {
                            getLocation { location ->
                                val coords = "${location.latitude}, ${location.longitude}"
                                text = coords
                                fieldValues[field.id] = coords
                            }
                        }
                    }
                }
                container.addView(button)
                fieldValues[field.id] = ""
            }
            
            FieldType.SWITCH_YES_NO -> {
                val switch = SwitchMaterial(this).apply { text = "Нет" }
                container.addView(switch)
                fieldValues[field.id] = "false"
                switch.setOnCheckedChangeListener { _, isChecked ->
                    switch.text = if (isChecked) "Да" else "Нет"
                    fieldValues[field.id] = isChecked.toString()
                }
            }
            
            FieldType.NUMBER -> {
                val input = TextInputEditText(this).apply {
                    hint = "Введите число"
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                }
                container.addView(TextInputLayout(this).apply { addView(input) })
                fieldValues[field.id] = ""
                input.addTextChangedListener(object : android.text.TextWatcher {
                    override fun afterTextChanged(s: android.text.Editable?) {
                        fieldValues[field.id] = s.toString()
                    }
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                })
            }
            
            FieldType.PHOTO -> {
                val button = Button(this).apply {
                    text = "Выбрать фото"
                    setOnClickListener {
                        Toast.makeText(this@CreateReportActivity, "Фото пока не реализовано", Toast.LENGTH_SHORT).show()
                    }
                }
                container.addView(button)
                fieldValues[field.id] = ""
            }
        }
        fieldsContainer.addView(container)
    }

    private fun validateAndSubmit() {
        if (selectedCategory == null) {
            Toast.makeText(this, "Выберите категорию", Toast.LENGTH_SHORT).show()
            return
        }
        
        val missingFields = fieldValues.filter { it.value.isEmpty() }
        if (missingFields.isNotEmpty()) {
            Toast.makeText(this, "Заполните все обязательные поля", Toast.LENGTH_SHORT).show()
            return
        }
        
        val report = Report(
            categoryId = selectedCategory!!.id,
            title = "Отчет ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())}",
            data = fieldValues,
            latitude = lastLocation?.latitude,
            longitude = lastLocation?.longitude,
            synced = false
        )
        
        scope.launch {
            try {
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

    private fun showDateTimePicker(callback: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                TimePickerDialog(
                    this,
                    { _, hour, minute ->
                        val datetime = Calendar.getInstance().apply {
                            set(year, month, day, hour, minute)
                        }
                        val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                        callback(format.format(datetime.time))
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun checkLocationPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return false
        }
        return true
    }

    private fun getLocation(callback: (Location) -> Unit) {
        if (!checkLocationPermission()) return
        
        val client = LocationServices.getFusedLocationProviderClient(this)
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    lastLocation = location
                    callback(location)
                } else {
                    Toast.makeText(this, "Не удалось получить координаты", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Ошибка GPS: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
