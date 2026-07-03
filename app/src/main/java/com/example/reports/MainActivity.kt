package com.example.reports

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.reports.databinding.ActivityMainBinding
import com.example.reports.ui.CategoriesActivity
import com.example.reports.ui.CreateReportActivity
import com.example.reports.ui.ReportsListActivity
import com.example.reports.ui.FieldManagementActivity
import com.example.reports.utils.Logger

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Logger.init(this)
        Logger.writeLog("MainActivity onCreate started")
        
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Logger.writeLog("MainActivity layout inflated")

            checkPermissions()

            binding.btnCreateReport.setOnClickListener {
                Logger.writeLog("Create Report button clicked")
                startActivity(Intent(this, CreateReportActivity::class.java))
            }

            binding.btnReportsList.setOnClickListener {
                Logger.writeLog("Reports List button clicked")
                startActivity(Intent(this, ReportsListActivity::class.java))
            }

            binding.btnCategories.setOnClickListener {
                Logger.writeLog("Categories button clicked")
                startActivity(Intent(this, CategoriesActivity::class.java))
            }

            binding.btnFields.setOnClickListener {
                Logger.writeLog("Fields Management button clicked")
                startActivity(Intent(this, FieldManagementActivity::class.java))
            }

            Logger.writeLog("MainActivity initialized successfully")
            
        } catch (e: Exception) {
            Logger.writeError("MainActivity onCreate error", e)
            Toast.makeText(this, "Ошибка инициализации", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkPermissions() {
        try {
            Logger.writeLog("Checking permissions")
            
            val permissions = mutableListOf<String>()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
                }
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
                
                if (permissions.isNotEmpty()) {
                    Logger.writeLog("Requesting permissions")
                    ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
                } else {
                    Logger.writeLog("All permissions already granted")
                }
            }
        } catch (e: Exception) {
            Logger.writeError("checkPermissions error", e)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        try {
            if (requestCode == PERMISSION_REQUEST_CODE) {
                val granted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                Logger.writeLog("Permissions result: $granted")
                if (granted) {
                    Toast.makeText(this, "Разрешения предоставлены", Toast.LENGTH_SHORT).show()
                    Logger.init(this)
                } else {
                    Toast.makeText(this, "Некоторые разрешения не предоставлены", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Logger.writeError("onRequestPermissionsResult error", e)
        }
    }
}
