package com.cateatmars_dev.yoedodurian

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.appbar.MaterialToolbar
import java.io.InputStream
import java.util.concurrent.Executors

class ImageAnalysisActivity : BaseActivity() {

    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var isActivityDestroyed = false

    data class DiseaseMeta(
        val titleRes: Int,
        val descRes: Int,
        val badgeRes: Int,
        val colorRes: Int,
        val bgRes: Int
    )

    private val labelMap = mapOf(
        "Algal spot" to DiseaseMeta(R.string.disease_algal_spot_title, R.string.disease_algal_spot_desc, R.string.badge_watch, R.color.statusInfo, R.color.statusInfoSurface),
        "Anthracnose" to DiseaseMeta(R.string.disease_anthracnose_title, R.string.disease_anthracnose_desc, R.string.badge_critical, R.color.statusDanger, R.color.statusDangerSurface),
        "Healthy" to DiseaseMeta(R.string.disease_healthy_title, R.string.disease_healthy_desc, R.string.badge_healthy, R.color.statusHealthy, R.color.statusHealthySurface),
        "Other" to DiseaseMeta(R.string.disease_others_title, R.string.disease_others_desc, R.string.badge_other, R.color.textColorSecondary, R.color.background_light),
        "Phomopsis" to DiseaseMeta(R.string.disease_phomopsis_title, R.string.disease_phomopsis_desc, R.string.badge_warning, R.color.statusWarning, R.color.statusWarningSurface),
        "Phytophtora" to DiseaseMeta(R.string.disease_phytophthora_title, R.string.disease_phytophthora_desc, R.string.badge_critical, R.color.statusDanger, R.color.statusDangerSurface),
        "Rhizoctonia" to DiseaseMeta(R.string.disease_rhizoctonia_title, R.string.disease_rhizoctonia_desc, R.string.badge_warning, R.color.statusWarning, R.color.statusWarningSurface)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_analysis)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarAnalysis)
        toolbar.setNavigationOnClickListener { finish() }

        val imageUriStr = intent.getStringExtra("IMAGE_URI")
        if (imageUriStr == null) {
            Toast.makeText(this, getString(R.string.image_not_found), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val uri = Uri.parse(imageUriStr)
        val ivSelectedImage = findViewById<ImageView>(R.id.ivSelectedImage)

        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap == null) {
                Toast.makeText(this, getString(R.string.error_loading_image), Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            ivSelectedImage.setImageBitmap(bitmap)

            objectDetectorHelper = ObjectDetectorHelper(this)
            analyzeImage(bitmap)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_loading_image), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun analyzeImage(bitmap: Bitmap) {
        val layoutLoading = findViewById<LinearLayout>(R.id.layoutLoading)
        val layoutResults = findViewById<LinearLayout>(R.id.layoutResults)
        val overlayAnalysis = findViewById<OverlayView>(R.id.overlayAnalysis)

        layoutLoading.visibility = View.VISIBLE
        layoutResults.visibility = View.GONE
        overlayAnalysis?.clearResults()

        executor.execute {
            val detections = objectDetectorHelper.detect(bitmap)

            handler.post {
                if (isActivityDestroyed || isFinishing) return@post
                layoutLoading.visibility = View.GONE
                layoutResults.visibility = View.VISIBLE
                overlayAnalysis?.setResults(detections, bitmap.width, bitmap.height)
                displayResults(detections)
            }
        }
    }

    private fun displayResults(detections: List<DetectionResult>) {
        val container = findViewById<LinearLayout>(R.id.containerResults)
        container.removeAllViews()

        val tvResultsHeader = findViewById<TextView>(R.id.tvResultsHeader)

        if (detections.isEmpty()) {
            tvResultsHeader.text = getString(R.string.results_header_not_found)
            val emptyMsg = TextView(this).apply {
                text = getString(R.string.results_empty_desc)
                textSize = 15f
                setTextColor(getColor(R.color.textColorPrimary))
                setPadding(0, 0, 0, 32)
            }
            container.addView(emptyMsg)
            return
        }

        tvResultsHeader.text = getString(R.string.results_header_detected)

        val aggregated = mutableMapOf<String, DetectionResult>()
        for (d in detections) {
            val current = aggregated[d.label]
            if (current == null || d.confidence > current.confidence) {
                aggregated[d.label] = d
            }
        }

        val sortedDetections = aggregated.values.sortedByDescending { it.confidence }
        val inflater = LayoutInflater.from(this)

        for (result in sortedDetections) {
            val view = inflater.inflate(R.layout.item_analysis_result_card, container, false)

            val tvDiseaseTitle = view.findViewById<TextView>(R.id.tvDiseaseTitle)
            val tvSeverityBadge = view.findViewById<TextView>(R.id.tvSeverityBadge)
            val analysisGuideContainer = view.findViewById<LinearLayout>(R.id.analysisGuideContainer)

            val meta = labelMap[result.label]
            val confidencePercent = "%.1f".format(result.confidence * 100)

            val severityColor = if (meta != null) getColor(meta.colorRes) else getColor(R.color.textColorSecondary)
            val severityTint = if (meta != null) getColor(meta.bgRes) else getColor(R.color.background_light)
            val pathogenColor = ThemeUtils.getPathogenColor(this, result.label)

            val titleStr = if (meta != null) getString(meta.titleRes) else result.label
            tvDiseaseTitle.text = "$confidencePercent% $titleStr"
            tvDiseaseTitle.setTextColor(pathogenColor)

            tvSeverityBadge.text = if (meta != null) getString(meta.badgeRes) else getString(R.string.badge_other)
            tvSeverityBadge.setTextColor(severityColor)
            val badgeBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f
                setColor(severityTint)
            }
            tvSeverityBadge.background = badgeBg

            val diseaseKey = DiseaseGuideRenderer.resolveKey(meta?.titleRes ?: -1, result.label)
            DiseaseGuideRenderer.populateGuide(this, analysisGuideContainer, diseaseKey)

            container.addView(view)
        }

        UiAnimations.playStaggeredEntrance(container, startDelayMs = 40L, stepMs = 70L)
    }

    override fun onDestroy() {
        isActivityDestroyed = true
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
        if (::objectDetectorHelper.isInitialized) {
            objectDetectorHelper.close()
        }
        executor.shutdown()
    }
}
