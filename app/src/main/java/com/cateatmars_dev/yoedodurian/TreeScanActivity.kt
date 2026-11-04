package com.cateatmars_dev.yoedodurian

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TreeScanActivity : BaseActivity() {

    override val isFullBleedScreen: Boolean = true
    override val isDarkNavigationBar: Boolean = true

    companion object {
        private const val TAG = "TreeScanActivity"
    }

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var tvDetectionInfo: TextView
    private lateinit var btnBack: MaterialButton
    private lateinit var btnToggleScan: MaterialButton

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var objectDetectorHelper: ObjectDetectorHelper

    private var frameCount = 0
    private val INFERENCE_EVERY_N_FRAMES = 4
    @Volatile private var isDestroyed = false
    @Volatile private var isProcessingFrame = false

    private var isScanning = false
    private val scanResultsMap = mutableMapOf<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tree_scan)

        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)
        tvDetectionInfo = findViewById(R.id.tvDetectionInfo)
        btnBack = findViewById(R.id.btnBack)
        btnToggleScan = findViewById(R.id.btnToggleScan)

        setupInsets()
        UiAnimations.applyPressEffect(btnBack, btnToggleScan)

        btnBack.setOnClickListener { finish() }

        btnToggleScan.setOnClickListener {
            if (isScanning) {
                stopScan()
            } else {
                startScan()
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        objectDetectorHelper = ObjectDetectorHelper(this)

        startCamera()
    }

    private fun setupInsets() {
        val initBackTop = (btnBack.layoutParams as ViewGroup.MarginLayoutParams).topMargin
        val initBackStart = (btnBack.layoutParams as ViewGroup.MarginLayoutParams).marginStart
        val initToggleBottom = (btnToggleScan.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
        val initToggleStart = (btnToggleScan.layoutParams as ViewGroup.MarginLayoutParams).marginStart
        val initToggleEnd = (btnToggleScan.layoutParams as ViewGroup.MarginLayoutParams).marginEnd

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.treeScanRoot)) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val backLp = btnBack.layoutParams as ViewGroup.MarginLayoutParams
            backLp.topMargin = initBackTop + bars.top
            backLp.marginStart = initBackStart + bars.left
            btnBack.layoutParams = backLp

            val toggleLp = btnToggleScan.layoutParams as ViewGroup.MarginLayoutParams
            toggleLp.bottomMargin = initToggleBottom + bars.bottom
            toggleLp.marginStart = initToggleStart + bars.left
            toggleLp.marginEnd = initToggleEnd + bars.right
            btnToggleScan.layoutParams = toggleLp

            insets
        }
    }

    private fun startScan() {
        isScanning = true
        scanResultsMap.clear()
        btnToggleScan.text = getString(R.string.stop_scan)
        btnToggleScan.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.holo_red_dark)
    }

    private fun stopScan() {
        isScanning = false
        btnToggleScan.text = getString(R.string.start_scan)
        btnToggleScan.backgroundTintList = ContextCompat.getColorStateList(this, R.color.colorPrimary)

        if (scanResultsMap.isEmpty()) {
            return
        }

        val input = EditText(this)
        input.hint = getString(R.string.scan_dialog_hint)
        input.setPadding(40, 40, 40, 40)

        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.scan_dialog_title))
            .setView(input)
            .setPositiveButton(getString(R.string.btn_save)) { _, _ ->
                val name = input.text.toString().trim()
                saveScanAndProceed(name)
            }
            .setNegativeButton(getString(R.string.btn_cancel)) { dialog, _ ->
                dialog.cancel()
                scanResultsMap.clear()
            }
            .setCancelable(false)
            .show()
    }

    private fun saveScanAndProceed(customName: String) {
        val scansDir = File(filesDir, "scans")
        if (!scansDir.exists()) scansDir.mkdirs()

        val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        val finalName = if (customName.isEmpty()) dateFormat.format(Date()) else customName

        val wrapper = mapOf(
            "name" to finalName,
            "timestamp" to timestamp,
            "counts" to scanResultsMap
        )

        val scanFile = File(scansDir, "scan_$timestamp.json")
        val json = Gson().toJson(wrapper)
        scanFile.writeText(json)

        val intent = Intent(this, ScanResultActivity::class.java)
        intent.putExtra("SCAN_FILE_PATH", scanFile.absolutePath)
        startActivity(intent)
        finish()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImage(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
                Log.d(TAG, "Camera started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed: ${e.message}", e)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImage(imageProxy: ImageProxy) {
        if (isDestroyed || isProcessingFrame) {
            imageProxy.close()
            return
        }

        frameCount++
        if (frameCount % INFERENCE_EVERY_N_FRAMES != 0) {
            imageProxy.close()
            return
        }

        isProcessingFrame = true
        val bitmap = try {
            imageProxy.toBitmap()
        } catch (_: Exception) {
            imageProxy.close()
            isProcessingFrame = false
            return
        }
        val rotatedBitmap = rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees.toFloat())
        imageProxy.close()

        if (isDestroyed) {
            isProcessingFrame = false
            return
        }

        val detections = try {
            objectDetectorHelper.detect(rotatedBitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Detection failed", e)
            emptyList()
        } finally {
            isProcessingFrame = false
        }

        runOnUiThread {
            if (isDestroyed) return@runOnUiThread

            overlayView.setResults(
                detections,
                rotatedBitmap.width,
                rotatedBitmap.height
            )

            if (isScanning && detections.isNotEmpty()) {
                val bestDetection = detections.maxByOrNull { it.confidence }
                if (bestDetection != null && bestDetection.confidence > 0.5f) {
                    val label = bestDetection.label
                    scanResultsMap[label] = scanResultsMap.getOrDefault(label, 0) + 1
                }
            }

            if (detections.isEmpty()) {
                tvDetectionInfo.text = getString(R.string.scanning)
            } else {
                val summary = detections.take(3).joinToString(" | ") {
                    val friendlyName = ThemeUtils.getDisplayLabel(this, it.label)
                    "$friendlyName: ${"%.0f".format(it.confidence * 100)}%"
                }
                tvDetectionInfo.text = summary
            }
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return bitmap
        val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override fun onDestroy() {
        isDestroyed = true
        super.onDestroy()
        try {
            cameraExecutor.shutdownNow()
            cameraExecutor.awaitTermination(300, TimeUnit.MILLISECONDS)
        } catch (_: Exception) {}
        objectDetectorHelper.close()
    }
}
