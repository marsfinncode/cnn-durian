package com.cateatmars_dev.yoedodurian

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var results: List<DetectionResult> = emptyList()
    private var imageWidth: Int = 1200
    private var imageHeight: Int = 1600

    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val labelBgPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, resources.displayMetrics)
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }

    private val classColors = mapOf(
        "Healthy"     to Color.parseColor("#10B981"), // Emerald Green
        "Phytophtora" to Color.parseColor("#EF4444"), // Red
        "Anthracnose" to Color.parseColor("#F97316"), // Orange
        "Rhizoctonia" to Color.parseColor("#F59E0B"), // Amber / Gold
        "Phomopsis"   to Color.parseColor("#854D0E"), // Brownish Yellow
        "Algal spot"  to Color.parseColor("#0D9488"), // Teal
        "Other"       to Color.parseColor("#64748B")  // Slate Grey
    )

    /**
     * Compute box and label color based on class and confidence.
     * Higher confidence produces a deeper, richer, and more opaque shade.
     */
    private fun getConfidenceColor(label: String, confidence: Float): Int {
        val baseColor = classColors[label] ?: Color.parseColor("#3B82F6")
        val hsv = FloatArray(3)
        Color.colorToHSV(baseColor, hsv)

        // Normalize confidence from [0.5, 1.0] to ratio [0.0, 1.0]
        val ratio = ((confidence - 0.50f) / 0.50f).coerceIn(0f, 1f)

        // Higher confidence -> higher saturation (vivid), slightly deeper value (darker/bolder)
        hsv[1] = (hsv[1] * (0.60f + 0.40f * ratio)).coerceIn(0f, 1f)
        hsv[2] = (hsv[2] * (1.05f - 0.25f * ratio)).coerceIn(0.2f, 1f)

        // Higher confidence -> higher opacity (alpha from 160 to 255)
        val alpha = (160 + (95 * ratio)).toInt().coerceIn(160, 255)

        return Color.HSVToColor(alpha, hsv)
    }

    fun setResults(detections: List<DetectionResult>, imgWidth: Int, imgHeight: Int) {
        results = detections
        imageWidth = imgWidth
        imageHeight = imgHeight
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (results.isEmpty()) return

        val viewW = width.toFloat()
        val viewH = height.toFloat()

        // fillCenter scale: pick larger scale so image fills the view
        val scaleX = viewW / imageWidth.toFloat()
        val scaleY = viewH / imageHeight.toFloat()
        val scale = maxOf(scaleX, scaleY)

        val displayW = imageWidth * scale
        val displayH = imageHeight * scale
        val offsetX = (viewW - displayW) / 2f
        val offsetY = (viewH - displayH) / 2f

        results.forEach { result ->
            val color = getConfidenceColor(result.label, result.confidence)
            boxPaint.color = color

            // Label background: slightly more opaque version of the confidence-tuned color
            val labelAlpha = (Color.alpha(color) * 0.95f).toInt().coerceIn(180, 255)
            labelBgPaint.color = Color.argb(
                labelAlpha,
                Color.red(color), Color.green(color), Color.blue(color)
            )

            // Map the 4 rotated corners from normalized image space to screen space
            val corners = result.corners
            val screenCorners = FloatArray(8)
            for (i in corners.indices step 2) {
                screenCorners[i] = offsetX + corners[i] * displayW
                screenCorners[i + 1] = offsetY + corners[i + 1] * displayH
            }

            // Draw rotated polygon
            val path = Path().apply {
                moveTo(screenCorners[0], screenCorners[1])
                lineTo(screenCorners[2], screenCorners[3])
                lineTo(screenCorners[4], screenCorners[5])
                lineTo(screenCorners[6], screenCorners[7])
                close()
            }
            canvas.drawPath(path, boxPaint)

            // Draw label near the top corner of the rotated box
            val displayName = ThemeUtils.getDisplayLabel(context, result.label)
            val label = "$displayName ${"%.0f".format(result.confidence * 100)}%"
            val textW = textPaint.measureText(label) + 20f
            val textH = textPaint.textSize + 14f

            val labelX = screenCorners[0].coerceIn(0f, (viewW - textW).coerceAtLeast(0f))
            val labelY = screenCorners[1].coerceIn(textH, viewH)
            val labelTop = (labelY - textH).coerceAtLeast(0f)
            val labelRect = RectF(labelX, labelTop, labelX + textW, labelTop + textH)
            canvas.drawRoundRect(labelRect, 8f, 8f, labelBgPaint)
            canvas.drawText(label, labelX + 10f, labelTop + textH - 8f, textPaint)
        }
    }

    fun clearResults() {
        results = emptyList()
        invalidate()
    }
}
