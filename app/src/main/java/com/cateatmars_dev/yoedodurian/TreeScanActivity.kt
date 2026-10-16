package com.cateatmars_dev.yoedodurian

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import com.google.gson.Gson
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TreeScanActivity : BaseActivity() {

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
    private val INFERENCE_EVERY_N_FRAMES = 3
    @Volatile private var isDestroyed = false
    
    private var isScanning = false
    private val scanResultsMap = mutableMapOf<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tree_scan)

        previewView     = findViewById(R.id.previewView)
        overlayView     = findViewById(R.id.overlayView)
        tvDetectionInfo = findViewById(R.id.tvDetectionInfo)
        btnBack         = findViewById(R.id.btnBack)
        btnToggleScan   = findViewById(R.id.btnToggleScan)

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

        val input = android.widget.EditText(this)
        input.hint = "เช่น ต้นทุเรียนหลังบ้าน..."
        input.setPadding(40, 40, 40, 40)

        android.app.AlertDialog.Builder(this)
            .setTitle("ตั้งชื่อการสแกน")
            .setView(input)
            .setPositiveButton("บันทึก") { _, _ ->
                val name = input.text.toString().trim()
                saveScanAndProceed(name)
            }
            .setNegativeButton("ยกเลิก") { dialog, _ ->
                dialog.cancel()
                scanResultsMap.clear()
            }
            .setCancelable(false)
            .show()
    }

    private fun saveScanAndProceed(customName: String) {
        val scansDir = File(filesDir, "scans")
        if (!scansDir.exists()) scansDir.mkdirs()
        
        val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("th", "TH"))
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
        if (isDestroyed) {
            imageProxy.close()
            return
        }

        frameCount++
        if (frameCount % INFERENCE_EVERY_N_FRAMES != 0) {
            imageProxy.close()
            return
        }

        val bitmap = imageProxy.toBitmap()
        val rotatedBitmap = rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees.toFloat())
        imageProxy.close()

        if (isDestroyed) return

        val detections = objectDetectorHelper.detect(rotatedBitmap)

        runOnUiThread {
            if (isDestroyed) return@runOnUiThread

            overlayView.setResults(
                detections,
                rotatedBitmap.width,
                rotatedBitmap.height
            )

            if (isScanning && detections.isNotEmpty()) {
                // Find highest confidence detection
                val bestDetection = detections.maxByOrNull { it.confidence }
                if (bestDetection != null && bestDetection.confidence > 0.5f) { // threshold
                    val label = bestDetection.label
                    scanResultsMap[label] = scanResultsMap.getOrDefault(label, 0) + 1
                }
            }

            if (detections.isEmpty()) {
                tvDetectionInfo.text = getString(R.string.scanning)
            } else {
                val summary = detections.take(3).joinToString(" | ") {
                    "${it.label}: ${"%.0f".format(it.confidence * 100)}%"
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
        cameraExecutor.shutdown()
        cameraExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)
        objectDetectorHelper.close()
    }
}
