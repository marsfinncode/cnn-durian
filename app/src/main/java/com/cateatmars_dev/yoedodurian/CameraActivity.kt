package com.cateatmars_dev.yoedodurian

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CameraActivity : BaseActivity() {

    override val isFullBleedScreen: Boolean = true
    override val isDarkNavigationBar: Boolean = true

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
    private val INFERENCE_EVERY_N_FRAMES = 4
    @Volatile private var isDestroyed = false
    @Volatile private var isProcessingFrame = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)
        tvDetectionInfo = findViewById(R.id.tvDetectionInfo)
        btnBack = findViewById(R.id.btnBack)

        setupInsets()
        UiAnimations.applyPressEffect(btnBack)
        btnBack.setOnClickListener { finish() }

        cameraExecutor = Executors.newSingleThreadExecutor()
        objectDetectorHelper = ObjectDetectorHelper(this)

        startCamera()
    }

    private fun setupInsets() {
        val initBtnMarginTop = (btnBack.layoutParams as ViewGroup.MarginLayoutParams).topMargin
        val initBtnMarginStart = (btnBack.layoutParams as ViewGroup.MarginLayoutParams).marginStart
        val initInfoPadBottom = tvDetectionInfo.paddingBottom
        val initInfoPadLeft = tvDetectionInfo.paddingLeft
        val initInfoPadRight = tvDetectionInfo.paddingRight
        val initInfoPadTop = tvDetectionInfo.paddingTop

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cameraRoot)) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val lp = btnBack.layoutParams as ViewGroup.MarginLayoutParams
            lp.topMargin = initBtnMarginTop + bars.top
            lp.marginStart = initBtnMarginStart + bars.left
            btnBack.layoutParams = lp

            tvDetectionInfo.setPadding(
                initInfoPadLeft + bars.left,
                initInfoPadTop,
                initInfoPadRight + bars.right,
                initInfoPadBottom + bars.bottom
            )
            insets
        }
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

            if (detections.isEmpty()) {
                tvDetectionInfo.text = getString(R.string.no_diseases_detected)
            } else {
                val summary = detections.take(3).joinToString(" | ") {
                    val friendlyName = ThemeUtils.getDisplayLabel(this, it.label)
                    "$friendlyName: ${"%.0f".format(it.confidence * 100)}%"
                }
                tvDetectionInfo.text = getString(R.string.detected_prefix, summary)
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
