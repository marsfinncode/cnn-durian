package com.example.diseasesdetectionapplication

import android.content.Context
import android.os.Bundle
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class SettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarSettings)
        toolbar.setNavigationOnClickListener { finish() }

        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val currentSize = prefs.getFloat("FONT_SCALE", 1.0f)

        val radioGroup = findViewById<RadioGroup>(R.id.rgFontSize)
        when (currentSize) {
            1.0f -> radioGroup.check(R.id.rbNormal)
            1.2f -> radioGroup.check(R.id.rbLarge)
            1.5f -> radioGroup.check(R.id.rbExtraLarge)
            else -> radioGroup.check(R.id.rbNormal)
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val scale = when (checkedId) {
                R.id.rbNormal -> 1.0f
                R.id.rbLarge -> 1.2f
                R.id.rbExtraLarge -> 1.5f
                else -> 1.0f
            }
            if (scale != currentSize) {
                prefs.edit().putFloat("FONT_SCALE", scale).apply()
                recreate() // Recreate activity to apply new font scale immediately
            }
        }
    }
}
