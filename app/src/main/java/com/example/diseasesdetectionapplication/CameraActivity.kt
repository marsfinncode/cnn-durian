package com.example.diseasesdetectionapplication

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CameraActivity : BaseActivity() {

    companion object {
        private const val TAG = "CameraActivity"
    }

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var tvDetectionInfo: TextView
    private lateinit var btnBack: MaterialButton

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var objectDetectorHelper: ObjectDetectorHelper

    private var frameCount = 0
    private val INFERENCE_EVERY_N_FRAMES = 3
    @Volatile private var isDestroyed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        previewView     = findViewById(R.id.previewView)
        overlayView     = findViewById(R.id.overlayView)
        tvDetectionInfo = findViewById(R.id.tvDetectionInfo)
        btnBack         = findViewById(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        cameraExecutor = Executors.newSingleThreadExecutor()
        objectDetectorHelper = ObjectDetectorHelper(this)

        startCamera()
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

            // Pass rotated dimensions so overlay scales correctly
            overlayView.setResults(
                detections,
                rotatedBitmap.width,   // width AFTER rotation
                rotatedBitmap.height   // height AFTER rotation
            )

            if (detections.isEmpty()) {
                tvDetectionInfo.text = "No diseases detected"
            } else {
                val summary = detections.take(3).joinToString(" | ") {
                    "${it.label}: ${"%.0f".format(it.confidence * 100)}%"
                }
                tvDetectionInfo.text = "Detected: $summary"
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
        // Shutdown executor and wait for in-flight inference to finish
        // before closing the interpreter
        cameraExecutor.shutdown()
        cameraExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)
        objectDetectorHelper.close()
    }
}