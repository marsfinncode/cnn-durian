package com.example.diseasesdetectionapplication

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val fontScale = prefs.getFloat("FONT_SCALE", 1.0f)
        
        val configuration = Configuration(newBase.resources.configuration)
        configuration.fontScale = fontScale
        val context = newBase.createConfigurationContext(configuration)
        super.attachBaseContext(context)
    }
}
