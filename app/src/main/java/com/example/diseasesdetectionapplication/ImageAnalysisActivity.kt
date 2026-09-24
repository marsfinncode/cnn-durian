package com.example.diseasesdetectionapplication

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.appbar.MaterialToolbar
import java.io.InputStream
import java.util.concurrent.Executors

class ImageAnalysisActivity : BaseActivity() {

    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    data class DiseaseMeta(
        val titleRes: Int, val descRes: Int, 
        val badgeText: String, val colorRes: Int, val bgRes: Int
    )

    private val labelMap = mapOf(
        "Algal spot" to DiseaseMeta(R.string.disease_algal_spot_title, R.string.disease_algal_spot_desc, "เฝ้าดู", R.color.statusInfo, R.color.statusInfoSurface),
        "Anthracnose" to DiseaseMeta(R.string.disease_anthracnose_title, R.string.disease_anthracnose_desc, "รุนแรง", R.color.statusDanger, R.color.statusDangerSurface),
        "Healthy" to DiseaseMeta(R.string.disease_healthy_title, R.string.disease_healthy_desc, "ปกติ", R.color.statusHealthy, R.color.statusHealthySurface),
        "Other" to DiseaseMeta(R.string.disease_others_title, R.string.disease_others_desc, "อื่นๆ", R.color.textColorSecondary, R.color.background_light),
        "Phomopsis" to DiseaseMeta(R.string.disease_phomopsis_title, R.string.disease_phomopsis_desc, "ระวัง", R.color.statusWarning, R.color.statusWarningSurface),
        "Phytophtora" to DiseaseMeta(R.string.disease_phytophthora_title, R.string.disease_phytophthora_desc, "รุนแรง", R.color.statusDanger, R.color.statusDangerSurface),
        "Rhizoctonia" to DiseaseMeta(R.string.disease_rhizoctonia_title, R.string.disease_rhizoctonia_desc, "ระวัง", R.color.statusWarning, R.color.statusWarningSurface)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_analysis)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarAnalysis)
        toolbar.setNavigationOnClickListener { finish() }

        val imageUriStr = intent.getStringExtra("IMAGE_URI")
        if (imageUriStr == null) {
            Toast.makeText(this, "ไม่พบรูปภาพ", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val uri = Uri.parse(imageUriStr)
        val ivSelectedImage = findViewById<ImageView>(R.id.ivSelectedImage)

        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            ivSelectedImage.setImageBitmap(bitmap)
            
            objectDetectorHelper = ObjectDetectorHelper(this)
            analyzeImage(bitmap)
        } catch (e: Exception) {
            Toast.makeText(this, "เกิดข้อผิดพลาดในการโหลดรูปภาพ", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun analyzeImage(bitmap: Bitmap) {
        val layoutLoading = findViewById<LinearLayout>(R.id.layoutLoading)
        val layoutResults = findViewById<LinearLayout>(R.id.layoutResults)
        
        layoutLoading.visibility = View.VISIBLE
        layoutResults.visibility = View.GONE

        executor.execute {
            // Run inference
            val detections = objectDetectorHelper.detect(bitmap)

            handler.post {
                layoutLoading.visibility = View.GONE
                layoutResults.visibility = View.VISIBLE
                displayResults(detections)
            }
        }
    }

    private fun displayResults(detections: List<DetectionResult>) {
        val container = findViewById<LinearLayout>(R.id.containerResults)
        container.removeAllViews()

        val tvResultsHeader = findViewById<TextView>(R.id.tvResultsHeader)
        val cardDisclaimer = findViewById<View>(R.id.cardDisclaimer)

        if (detections.isEmpty()) {
            tvResultsHeader.text = "ไม่พบร่องรอยของโรคที่ชัดเจน"
            val emptyMsg = TextView(this).apply {
                text = "AI ไม่สามารถวิเคราะห์โรคได้อย่างมั่นใจจากรูปภาพนี้ กรุณาลองถ่ายภาพใหม่ให้ชัดเจนขึ้น หรือถ่ายใกล้บริเวณที่มีอาการ"
                textSize = 15f
                setTextColor(getColor(R.color.textColorPrimary))
                setPadding(0, 0, 0, 32)
            }
            container.addView(emptyMsg)
            return
        }

        tvResultsHeader.text = "โรคที่อาจเป็นไปได้"

        // Aggregate detections by label, finding the max confidence DetectionResult for each label
        val aggregated = mutableMapOf<String, DetectionResult>()
        for (d in detections) {
            val current = aggregated[d.label]
            if (current == null || d.confidence > current.confidence) {
                aggregated[d.label] = d
            }
        }

        // Sort by confidence descending
        val sortedDetections = aggregated.values.sortedByDescending { it.confidence }
        val inflater = LayoutInflater.from(this)

        for (result in sortedDetections) {
            val view = inflater.inflate(R.layout.item_analysis_result_card, container, false)
            
            val tvDiseaseTitle = view.findViewById<TextView>(R.id.tvDiseaseTitle)
            val tvSeverityBadge = view.findViewById<TextView>(R.id.tvSeverityBadge)
            val tvCoveragePercent = view.findViewById<TextView>(R.id.tvCoveragePercent)
            val pbCoverage = view.findViewById<ProgressBar>(R.id.pbCoverage)
            val tvDiseaseDetails = view.findViewById<TextView>(R.id.tvDiseaseDetails)

            val meta = labelMap[result.label]
            val confidencePercent = "%.1f".format(result.confidence * 100)
            
            // Calculate lesion coverage from bounding box (normalized area)
            // Area = width * height. Max is 1.0 (100%).
            val bbox = result.boundingBox
            val area = (bbox.right - bbox.left) * (bbox.bottom - bbox.top)
            val coveragePercent = (area * 100).coerceIn(0f, 100f)
            
            // Apply YOEDO Severity Logic
            val severity = ThemeUtils.estimateSeverity(coveragePercent)
            val severityColor = ThemeUtils.getSeverityColor(this, severity)
            val severityTint = ThemeUtils.getSeverityTint(this, severity)
            val pathogenColor = ThemeUtils.getPathogenColor(this, result.label)

            // Title
            val titleStr = if (meta != null) getString(meta.titleRes) else result.label
            tvDiseaseTitle.text = "$confidencePercent% $titleStr"
            tvDiseaseTitle.setTextColor(pathogenColor)

            // Severity Badge
            tvSeverityBadge.text = ThemeUtils.getSeverityLabel(severity)
            tvSeverityBadge.setTextColor(severityColor)
            val badgeBg = GradientDrawable()
            badgeBg.shape = GradientDrawable.RECTANGLE
            badgeBg.cornerRadius = 24f
            badgeBg.setColor(severityTint)
            tvSeverityBadge.background = badgeBg

            // Coverage Bar
            val coverageInt = coveragePercent.toInt()
            tvCoveragePercent.text = "$coverageInt%"
            tvCoveragePercent.setTextColor(severityColor)
            pbCoverage.progress = coverageInt
            pbCoverage.progressTintList = android.content.res.ColorStateList.valueOf(severityColor)

            // Details
            if (meta != null) {
                val desc = getString(meta.descRes)
                tvDiseaseDetails.text = Html.fromHtml(desc, Html.FROM_HTML_MODE_COMPACT)
            } else {
                tvDiseaseDetails.text = "ไม่มีคำแนะนำการรักษาเพิ่มเติมสำหรับโรคนี้"
            }

            container.addView(view)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::objectDetectorHelper.isInitialized) {
            objectDetectorHelper.close()
        }
        executor.shutdown()
    }
}
