package com.cateatmars_dev.yoedodurian

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : BaseActivity() {

    override val isFullBleedScreen: Boolean = true

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
        private const val TREE_SCAN_PERMISSION_REQUEST_CODE = 200
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val intent = Intent(this, ImageAnalysisActivity::class.java)
            intent.putExtra("IMAGE_URI", uri.toString())
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupEdgeToEdgeInsets()
        setupAnimations()

        // Scan button
        findViewById<View>(R.id.btnStartDetecting)?.setOnClickListener {
            checkCameraPermissionAndStart()
        }

        // Analyze Image button
        findViewById<View>(R.id.btnAnalyzeImage)?.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Tree Scan button
        findViewById<View>(R.id.btnTreeScan)?.setOnClickListener {
            checkCameraPermissionAndStartTreeScan()
        }

        // Disease info button
        findViewById<View>(R.id.btnDiseaseInfo)?.setOnClickListener {
            startActivity(Intent(this, DiseaseListActivity::class.java))
        }

        // Settings
        findViewById<View>(R.id.btnSettings)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun setupEdgeToEdgeInsets() {
        val rootScroll = findViewById<View>(R.id.mainRootLayout)
        val heroTopBar = findViewById<View>(R.id.heroTopBar)
        val cardsContainer = findViewById<View>(R.id.cardsContainer)

        val initTopPad = heroTopBar.paddingTop
        val initLeftPad = heroTopBar.paddingLeft
        val initRightPad = heroTopBar.paddingRight
        val initCardsPadBottom = cardsContainer.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(rootScroll) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            heroTopBar.setPadding(
                initLeftPad + bars.left,
                initTopPad + bars.top,
                initRightPad + bars.right,
                heroTopBar.paddingBottom
            )
            cardsContainer.setPadding(
                cardsContainer.paddingLeft,
                cardsContainer.paddingTop,
                cardsContainer.paddingRight,
                initCardsPadBottom + bars.bottom
            )
            insets
        }
    }

    private fun setupAnimations() {
        // Smooth hero fade + subtle scale entrance
        val heroContent = findViewById<View>(R.id.heroContentContainer)
        heroContent?.apply {
            alpha = 0f
            translationY = -12f * resources.displayMetrics.density
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(380L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        // Smooth staggered slide-up + fade-in on action cards
        val cardsContainer = findViewById<ViewGroup>(R.id.cardsContainer)
        UiAnimations.playStaggeredEntrance(cardsContainer, startDelayMs = 60L, stepMs = 60L)

        // Tactile press feedback on all cards & settings icon
        UiAnimations.applyPressEffect(
            findViewById(R.id.cardStartDetecting),
            findViewById(R.id.cardTreeScan),
            findViewById(R.id.cardAnalyzeImage),
            findViewById(R.id.cardDiseaseInfo),
            findViewById(R.id.btnSettings)
        )
    }

    private fun checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCameraActivity()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun checkCameraPermissionAndStartTreeScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startTreeScanActivity()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                TREE_SCAN_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCameraActivity()
            } else {
                Toast.makeText(this, getString(R.string.camera_permission_denied), Toast.LENGTH_LONG).show()
            }
        } else if (requestCode == TREE_SCAN_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startTreeScanActivity()
            } else {
                Toast.makeText(this, getString(R.string.camera_permission_denied), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startCameraActivity() {
        startActivity(Intent(this, CameraActivity::class.java))
    }

    private fun startTreeScanActivity() {
        startActivity(Intent(this, TreeScanActivity::class.java))
    }
}
