package com.cateatmars_dev.yoedodurian

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {

    override val isFullBleedScreen: Boolean = true
    override val isDarkNavigationBar: Boolean = true

    private val handler = Handler(Looper.getMainLooper())
    private var hasNavigated = false

    private val launchMainRunnable = Runnable {
        if (!hasNavigated && !isFinishing && !isDestroyed) {
            hasNavigated = true
            startActivity(Intent(this, MainActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val bottomContainer = findViewById<View>(R.id.splashBottomContainer)
        val initialBottomPad = bottomContainer.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.splashRoot)) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            bottomContainer.setPadding(
                bottomContainer.paddingLeft,
                bottomContainer.paddingTop,
                bottomContainer.paddingRight,
                initialBottomPad + bars.bottom
            )
            insets
        }

        // Clean, smooth logo & title fade + scale entrance
        val centerContainer = findViewById<View>(R.id.splashCenterContainer)
        centerContainer.alpha = 0f
        centerContainer.scaleX = 0.90f
        centerContainer.scaleY = 0.90f
        centerContainer.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(480L)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .start()

        bottomContainer.alpha = 0f
        bottomContainer.animate()
            .alpha(1f)
            .setDuration(450L)
            .setStartDelay(100L)
            .start()

        val progressSplash = findViewById<ProgressBar>(R.id.progressSplash)
        UiAnimations.animateProgress(progressSplash, 100, durationMs = 1000L)

        // Version name in bottom right
        val tvSplashVersion = findViewById<android.widget.TextView>(R.id.tvSplashVersion)
        val versionName = try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
        tvSplashVersion?.text = "v$versionName"

        handler.postDelayed(launchMainRunnable, 1120L)
    }

    override fun onDestroy() {
        handler.removeCallbacks(launchMainRunnable)
        super.onDestroy()
    }
}
