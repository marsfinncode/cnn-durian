package com.example.diseasesdetectionapplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
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
        textSize = 40f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }

    private val colors = listOf(
        Color.parseColor("#FF4444"),
        Color.parseColor("#FF8800"),
        Color.parseColor("#00CC44"),
        Color.parseColor("#0088FF"),
        Color.parseColor("#CC00FF"),
        Color.parseColor("#FF0088"),
        Color.parseColor("#00CCCC")
    )

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

        results.forEachIndexed { index, result ->
            val color = colors[index % colors.size]
            boxPaint.color = color
            labelBgPaint.color = Color.argb(200,
                Color.red(color), Color.green(color), Color.blue(color))

            // Map the 4 rotated corners from normalized image space to screen space
            val corners = result.corners  // 8 floats: x0,y0,x1,y1,x2,y2,x3,y3
            val screenCorners = FloatArray(8)
            for (i in corners.indices step 2) {
                screenCorners[i]   = offsetX + corners[i]   * displayW
                screenCorners[i+1] = offsetY + corners[i+1] * displayH
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
            val labelX = screenCorners[0].coerceIn(0f, viewW - 200f)
            val labelY = screenCorners[1].coerceIn(40f, viewH)

            val label = "${result.label} ${"%.0f".format(result.confidence * 100)}%"
            val textW = textPaint.measureText(label) + 20f
            val textH = textPaint.textSize + 12f
            val labelTop = (labelY - textH).coerceAtLeast(0f)
            val labelRect = RectF(labelX, labelTop, labelX + textW, labelTop + textH)
            canvas.drawRoundRect(labelRect, 6f, 6f, labelBgPaint)
            canvas.drawText(label, labelX + 10f, labelTop + textH - 6f, textPaint)
        }
    }

    fun clearResults() {
        results = emptyList()
        invalidate()
    }
}