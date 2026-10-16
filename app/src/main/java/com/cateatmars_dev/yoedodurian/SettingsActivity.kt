package com.cateatmars_dev.yoedodurian

import android.content.Context
import android.os.Bundle
import android.widget.RadioGroup
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
        
        findViewById<android.view.View>(R.id.btnManageStorage)?.setOnClickListener {
            val intent = android.content.Intent(this, ScanHistoryActivity::class.java)
            startActivity(intent)
        }

        // App Version
        val tvAppVersion = findViewById<android.widget.TextView>(R.id.tvAppVersion)
        val versionName = try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
        tvAppVersion?.text = versionName

        // Privacy Policy
        findViewById<android.view.View>(R.id.btnPrivacyPolicy)?.setOnClickListener {
            openUrl("https://marsfinncode.github.io/cnn-durian/privacy-policy.html")
        }

        // GitHub Project Link
        findViewById<android.view.View>(R.id.btnGithubRepo)?.setOnClickListener {
            openUrl("https://github.com/marsfinncode/cnn-durian")
        }

        // MWIT Logo Redirect & Image Load
        val ivLogoMwit = findViewById<android.widget.ImageView>(R.id.ivLogoMwit)
        if (ivLogoMwit != null) {
            loadMwitLogo(ivLogoMwit)
        }
        findViewById<android.view.View>(R.id.btnLogoMwit)?.setOnClickListener {
            openUrl("https://mwit.ac.th/")
        }

        // GitHub Logo Redirect
        findViewById<android.view.View>(R.id.btnLogoGithub)?.setOnClickListener {
            openUrl("https://github.com/marsfinncode/cnn-durian")
        }

        // Apache License Notice
        findViewById<android.view.View>(R.id.tvLicenseNotice)?.setOnClickListener {
            openUrl("https://www.apache.org/licenses/LICENSE-2.0")
        }
    }

    private fun loadMwitLogo(imageView: android.widget.ImageView) {
        val drawableResId = resources.getIdentifier("logo_mwit", "drawable", packageName)
        if (drawableResId != 0) {
            imageView.setImageResource(drawableResId)
            return
        }

        val cacheFile = java.io.File(cacheDir, "logo_mwit.png")
        if (cacheFile.exists()) {
            val bitmap = android.graphics.BitmapFactory.decodeFile(cacheFile.absolutePath)
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
                return
            }
        }

        kotlin.concurrent.thread {
            try {
                val url = java.net.URL("https://upload.wikimedia.org/wikipedia/th/4/47/Mwit-logo.png")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                if (conn.responseCode == 200) {
                    val bytes = conn.inputStream.readBytes()
                    cacheFile.writeBytes(bytes)
                    val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        runOnUiThread {
                            imageView.setImageBitmap(bitmap)
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback to ic_logo_mwit vector drawable already set in layout
            }
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "ไม่สามารถเปิดเบราว์เซอร์ได้", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

