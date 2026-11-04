package com.cateatmars_dev.yoedodurian

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

open class BaseActivity : AppCompatActivity() {

    /**
     * When true, the activity handles its own top/bottom system bar insets
     * so backgrounds (Splash, Home Hero Header, Camera Viewfinder) can bleed edge-to-edge.
     */
    protected open val isFullBleedScreen: Boolean = false

    /**
     * When true, uses light icons on a dark navigation bar (Splash, Camera, TreeScan).
     */
    protected open val isDarkNavigationBar: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        val navStyle = if (isDarkNavigationBar) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        // All screens feature a deep emerald header, splash, or dark camera preview at top,
        // so white status bar icons (SystemBarStyle.dark) provide crisp contrast everywhere.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = navStyle
        )
        super.onCreate(savedInstanceState)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (isFullBleedScreen) return

        val contentView = findViewById<View>(android.R.id.content)
        val topBar = findTopToolbarView()

        val initialTopBarHeight = topBar?.layoutParams?.height ?: -1
        val initialTopBarPadLeft = topBar?.paddingLeft ?: 0
        val initialTopBarPadTop = topBar?.paddingTop ?: 0
        val initialTopBarPadRight = topBar?.paddingRight ?: 0
        val initialTopBarPadBottom = topBar?.paddingBottom ?: 0

        ViewCompat.setOnApplyWindowInsetsListener(contentView) { view, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            if (topBar != null) {
                view.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
                if (initialTopBarHeight > 0) {
                    val lp = topBar.layoutParams
                    lp.height = initialTopBarHeight + systemBars.top
                    topBar.layoutParams = lp
                }
                topBar.setPadding(
                    initialTopBarPadLeft,
                    initialTopBarPadTop + systemBars.top,
                    initialTopBarPadRight,
                    initialTopBarPadBottom
                )
            } else {
                view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            }
            insets
        }
    }

    private fun findTopToolbarView(): View? {
        return findViewById<View>(R.id.toolbar)
            ?: findViewById<View>(R.id.toolbarDetail)
            ?: findViewById<View>(R.id.toolbarAnalysis)
            ?: findViewById<View>(R.id.toolbarHistory)
            ?: findViewById<View>(R.id.toolbarSettings)
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
