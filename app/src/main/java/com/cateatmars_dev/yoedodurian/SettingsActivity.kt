package com.cateatmars_dev.yoedodurian

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.android.material.appbar.MaterialToolbar
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class SettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarSettings)
        toolbar.setNavigationOnClickListener { finish() }

        UiAnimations.playStaggeredEntrance(findViewById<ViewGroup>(R.id.settingsCardsContainer), startDelayMs = 40L, stepMs = 45L)
        UiAnimations.applyPressEffect(
            findViewById(R.id.cardManageStorage),
            findViewById(R.id.btnLogoBuu),
            findViewById(R.id.btnLogoMwit)
        )

        // Language Selection
        val rgLanguage = findViewById<RadioGroup>(R.id.rgLanguage)
        val currentAppLocales = AppCompatDelegate.getApplicationLocales()
        if (currentAppLocales.isEmpty) {
            rgLanguage?.check(R.id.rbLangSystem)
        } else {
            val primaryLang = currentAppLocales[0]?.language
            when (primaryLang) {
                "th" -> rgLanguage?.check(R.id.rbLangTh)
                "en" -> rgLanguage?.check(R.id.rbLangEn)
                else -> rgLanguage?.check(R.id.rbLangSystem)
            }
        }

        rgLanguage?.setOnCheckedChangeListener { _, checkedId ->
            val targetLocale = when (checkedId) {
                R.id.rbLangTh -> LocaleListCompat.forLanguageTags("th")
                R.id.rbLangEn -> LocaleListCompat.forLanguageTags("en")
                else -> LocaleListCompat.getEmptyLocaleList()
            }
            if (targetLocale != currentAppLocales) {
                AppCompatDelegate.setApplicationLocales(targetLocale)
            }
        }

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
                recreate()
            }
        }

        findViewById<View>(R.id.btnManageStorage)?.setOnClickListener {
            startActivity(Intent(this, ScanHistoryActivity::class.java))
        }

        // App Version
        val tvAppVersion = findViewById<TextView>(R.id.tvAppVersion)
        val versionName = try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
        tvAppVersion?.text = versionName

        // Privacy Policy
        findViewById<View>(R.id.btnPrivacyPolicy)?.setOnClickListener {
            openUrl("https://marsfinncode.github.io/cnn-durian/privacy-policy.html")
        }

        // GitHub Project Link
        findViewById<View>(R.id.btnGithubRepo)?.setOnClickListener {
            openUrl("https://github.com/marsfinncode/cnn-durian")
        }

        // Burapha University Logo Redirect
        findViewById<View>(R.id.btnLogoBuu)?.setOnClickListener {
            openUrl("https://www.buu.ac.th/")
        }

        // MWIT Logo Redirect & Image Load
        val ivLogoMwit = findViewById<ImageView>(R.id.ivLogoMwit)
        if (ivLogoMwit != null) {
            loadMwitLogo(ivLogoMwit)
        }
        findViewById<View>(R.id.btnLogoMwit)?.setOnClickListener {
            openUrl("https://mwit.ac.th/")
        }


        // Apache License Notice
        findViewById<View>(R.id.tvLicenseNotice)?.setOnClickListener {
            openUrl("https://www.apache.org/licenses/LICENSE-2.0")
        }
    }

    private fun loadMwitLogo(imageView: ImageView) {
        val drawableResId = resources.getIdentifier("logo_mwit", "drawable", packageName)
        if (drawableResId != 0) {
            imageView.setImageResource(drawableResId)
            return
        }

        val cacheFile = File(cacheDir, "logo_mwit.png")
        if (cacheFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
                return
            }
        }

        thread {
            try {
                val url = URL("https://upload.wikimedia.org/wikipedia/th/4/47/Mwit-logo.png")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                if (conn.responseCode == 200) {
                    val bytes = conn.inputStream.readBytes()
                    cacheFile.writeBytes(bytes)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        runOnUiThread {
                            imageView.setImageBitmap(bitmap)
                        }
                    }
                }
            } catch (_: Exception) {
                // Fallback to ic_logo_mwit vector drawable already set in layout
            }
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, getString(R.string.cannot_open_browser), Toast.LENGTH_SHORT).show()
        }
    }
}
