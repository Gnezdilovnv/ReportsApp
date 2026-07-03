package com.example.reports

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnTest).setOnClickListener {
            Toast.makeText(this, "Приложение работает!", Toast.LENGTH_SHORT).show()
        }
    }
}
