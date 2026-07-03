package com.example.reports.ui

import android.Manifest
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

class CreateReportActivity : AppCompatActivity() {
    private lateinit var categorySpinner: Spinner
    private lateinit var fieldsContainer: LinearLayout
    private val db by lazy { AppDatabase.getDatabase(this) }
    private val scope = CoroutineScope(Dispatchers.Main)
    private var selectedCategory: Category? = null
    private val fieldValues = mutableMapOf<String, String>()
    private var lastLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.writeLog("CreateReportActivity onCreate started")
        
        try {
            setContentView(R.layout.activity_create_report)
            Logger.writeLog("CreateReportActivity layout set")

            categorySpinner = findViewById(R.id.spinnerCategory)
            fieldsContainer = findViewById(R.id.fieldsContainer)
            val btnSubmit = findViewById<Button>(R.id.btnSubmit)

            loadCategories()

            btnSubmit.setOnClickListener {
                Logger.writeLog("Submit report button clicked")
                if (validateAndSubmit()) {
                    finish()
                }
            }
            
            Logger.writeLog("CreateReportActivity initialized successfully")
            
        } catch (e: Exception) {
            Logger.writeError("CreateReportActivity onCreate error", e)
            Toast.makeText(this, "Ошибка загрузки", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadCategories() {
        try {
            scope.launch {
                val categories = withContext(Dispatchers.IO) {
                    db.categoryDao().getAll()
                }

                if (categories.isEmpty()) {
                    Logger.writeLog("No categories found")
                    Toast.makeText(
                        this@CreateReportActivity,
                        "Сначала создайте категорию",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                Logger.writeLog("Loaded ${categories.size} categories")
                
                val adapter = ArrayAdapter(
                    this@CreateReportActivity,
                    android.R.layout.simple_spinner_item,
                    categories.map { it.name }
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                categorySpinner.adapter = adapter

                categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>,
                        view: android.view.View?,
                        position: Int,
                        id: Long
                    ) {
                        selectedCategory = categories[position]
                        Logger.writeLog("Category selected: ${selectedCategory?.name}")
                        loadFieldsForCategory(selectedCategory!!)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
            }
        } catch (e: Exception) {
            Logger.writeError("loadCategories error", e)
        }
    }

    private fun loadFieldsForCategory(category: Category) {
        try {
            scope.launch {
                val fields = withContext(Dispatchers.IO) {
                    db.fieldDao().getByCategoryId(category.id)
                }

                Logger.writeLog("Loaded ${fields.size} fields for category ${category.name}")

                runOnUiThread {
                    fieldsContainer.removeAllViews()
                    fieldValues.clear()

                    if (fields.isEmpty()) {
                        Toast.makeText(
                            this@CreateReportActivity,
                            "Нет полей для этой категории",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    fields.forEach { field ->
                        addFieldView(field)
                    }
                }
            }
        } catch (e: Exception) {
            Logger.writeError("loadFieldsForCategory error", e)
        }
    }

    private fun addFieldView(field: Field) {
        try {
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 16, 0, 16)
            }

            val label = TextView(this).apply {
                text = field.name
                textSize = 16f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            container.addView(label)

            when (field.type) {
                FieldType.TEXT -> {
                    val input = TextInputEditText(this).apply { hint = "Введите текст" }
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
            
        } catch (e: Exception) {
            Logger.writeError("addFieldView error", e)
        }
    }

    private fun validateAndSubmit(): Boolean {
        try {
            if (selectedCategory == null) {
                Toast.makeText(this, "Выберите категорию", Toast.LENGTH_SHORT).show()
                return false
            }

            val missingFields = fieldValues.filter { it.value.isEmpty() }
            if (missingFields.isNotEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                return false
            }

            val report = Report(
                categoryId = selectedCategory!!.id,
                title = "Отчет ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}",
                data = fieldValues,
                latitude = lastLocation?.latitude,
                longitude = lastLocation?.longitude,
                synced = false
            )

            Logger.writeLog("Submitting report: ${report.title}")

            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        db.reportDao().insert(report)
                    }
                    Logger.writeLog("Report saved successfully")
                    runOnUiThread {
                        Toast.makeText(this@CreateReportActivity, "Отчет сохранен!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Logger.writeError("Failed to save report", e)
                    runOnUiThread {
                        Toast.makeText(this@CreateReportActivity, "Ошибка сохранения", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            return true
            
        } catch (e: Exception) {
            Logger.writeError("validateAndSubmit error", e)
            Toast.makeText(this, "Ошибка валидации", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    private fun showDateTimePicker(callback: (String) -> Unit) {
        try {
            val calendar = java.util.Calendar.getInstance()
            android.app.DatePickerDialog(
                this,
                { _, year, month, day ->
                    android.app.TimePickerDialog(
                        this,
                        { _, hour, minute ->
                            val datetime = java.util.Calendar.getInstance().apply {
                                set(year, month, day, hour, minute)
                            }
                            val format = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                            callback(format.format(datetime.time))
                        },
                        calendar.get(java.util.Calendar.HOUR_OF_DAY),
                        calendar.get(java.util.Calendar.MINUTE),
                        true
                    ).show()
                },
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH),
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
            ).show()
        } catch (e: Exception) {
            Logger.writeError("showDateTimePicker error", e)
        }
    }

    private fun checkLocationPermission(): Boolean {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
                return false
            }
            return true
        } catch (e: Exception) {
            Logger.writeError("checkLocationPermission error", e)
            return false
        }
    }

    private fun getLocation(callback: (Location) -> Unit) {
        try {
            if (!checkLocationPermission()) return
            
            val client = LocationServices.getFusedLocationProviderClient(this)
            
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        lastLocation = location
                        Logger.writeLog("Location obtained: ${location.latitude}, ${location.longitude}")
                        callback(location)
                    } else {
                        Toast.makeText(this, "Не удалось получить координаты", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Logger.writeError("getCurrentLocation failed", e)
                    Toast.makeText(this, "Ошибка получения координат", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Logger.writeError("getLocation error", e)
            Toast.makeText(this, "Ошибка GPS", Toast.LENGTH_SHORT).show()
        }
    }
}
