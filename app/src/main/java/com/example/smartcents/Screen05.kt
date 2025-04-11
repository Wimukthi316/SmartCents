package com.example.smartcents

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Screen05 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_screen05)

        val image40 = findViewById<ImageView>(R.id.imageView40)

        image40.setOnClickListener { navigateToScreen04("Screen05") }

        var button5 = findViewById<Button>(R.id.button5)

        button5.setOnClickListener{
            val intent = Intent(this, Screen06::class.java)
            startActivity(intent)

        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun navigateToScreen04(s: String) {
        val intent = Intent(this, Screen04::class.java)
        startActivity(intent)

    }
}