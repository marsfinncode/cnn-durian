package com.cateatmars_dev.yoedodurian

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity

import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        val contentView = findViewById<android.view.View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(contentView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val fontScale = prefs.getFloat("FONT_SCALE", 1.0f)
        
        val configuration = Configuration(newBase.resources.configuration)
        configuration.fontScale = fontScale
        val context = newBase.createConfigurationContext(configuration)
        super.attachBaseContext(context)
    }
}
