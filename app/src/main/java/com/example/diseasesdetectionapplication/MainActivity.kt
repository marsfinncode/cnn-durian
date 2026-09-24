package com.example.diseasesdetectionapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : BaseActivity() {

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }

    private val pickImageLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val intent = Intent(this, ImageAnalysisActivity::class.java)
            intent.putExtra("IMAGE_URI", uri.toString())
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Fade in
        val rootLayout = findViewById<android.view.View>(R.id.mainRootLayout)
        val fadeInAnim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.fade_in)
        rootLayout?.startAnimation(fadeInAnim)

        // Scan button (now a LinearLayout inside a card)
        findViewById<android.view.View>(R.id.btnStartDetecting)?.setOnClickListener {
            checkCameraPermissionAndStart()
        }

        // Analyze Image button
        findViewById<android.view.View>(R.id.btnAnalyzeImage)?.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Disease info button
        findViewById<android.view.View>(R.id.btnDiseaseInfo)?.setOnClickListener {
            startActivity(Intent(this, DiseaseListActivity::class.java))
        }

        // Settings
        findViewById<android.view.View>(R.id.btnSettings)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun checkCameraPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCameraActivity()
            }
            else -> {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST_CODE
                )
            }
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
                Toast.makeText(
                    this,
                    getString(R.string.camera_permission_denied),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun startCameraActivity() {
        startActivity(Intent(this, CameraActivity::class.java))
    }
}