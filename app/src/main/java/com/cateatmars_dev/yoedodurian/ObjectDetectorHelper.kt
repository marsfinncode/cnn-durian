package com.cateatmars_dev.yoedodurian

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

data class DetectionResult(
    val boundingBox: RectF,
    val corners: FloatArray,
    val angle: Float,
    val label: String,
    val confidence: Float
)

class ObjectDetectorHelper(private val context: Context) {

    companion object {
        private const val TAG = "ObjectDetectorHelper"
        private const val MODEL_FILE = "model.tflite"
        private const val CONFIDENCE_THRESHOLD = 0.50f  // default 50% for other classes
        private const val RHIZOCTONIA_CONFIDENCE_THRESHOLD = 0.75f  // 75% for Rhizoctonia
        private const val IOU_THRESHOLD = 0.45f
        private const val MAX_DETECTIONS = 10
        private const val NUM_CLASSES = 7
        private const val MIN_BOX_SIZE = 0.03f
    }

    private var interpreter: Interpreter? = null
    private var inputSize = 832
    @Volatile private var isClosed = false

    private val labels = listOf(
        "Algal spot", "Anthracnose", "Healthy",
        "Other", "Phomopsis", "Phytophtora", "Rhizoctonia"
    )

    init { setupInterpreter() }

    private fun sigmoid(x: Float): Float = 1f / (1f + exp(-x))

    private fun setupInterpreter() {
        try {
            val options = Interpreter.Options().apply { numThreads = 4 }
            interpreter = Interpreter(loadModelFile(), options)
            val inputShape = interpreter!!.getInputTensor(0).shape()
            // Handle both NHWC [1, 832, 832, 3] and NCHW [1, 3, 832, 832]
            inputSize = if (inputShape[1] == 3) inputShape[2] else inputShape[1]
            Log.d(TAG, "Model loaded. Input: ${inputShape.toList()}, inputSize=$inputSize")
            for (i in 0 until interpreter!!.outputTensorCount)
                Log.d(TAG, "Output[$i]: ${interpreter!!.getOutputTensor(i).shape().toList()}")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model: ${e.message}", e)
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val afd = context.assets.openFd(MODEL_FILE)
        return FileInputStream(afd.fileDescriptor).channel
            .map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
    }

    // Letterbox: resize maintaining aspect ratio, pad with grey
    private fun letterbox(bitmap: Bitmap): Triple<Bitmap, Float, Pair<Float, Float>> {
        val scale = min(
            inputSize.toFloat() / bitmap.width,
            inputSize.toFloat() / bitmap.height
        )
        val scaledW = (bitmap.width * scale).toInt()
        val scaledH = (bitmap.height * scale).toInt()
        val padX = (inputSize - scaledW) / 2f
        val padY = (inputSize - scaledH) / 2f

        val result = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.rgb(114, 114, 114))
        val scaled = Bitmap.createScaledBitmap(bitmap, scaledW, scaledH, true)
        canvas.drawBitmap(scaled, padX, padY, null)
        scaled.recycle()

        return Triple(result, scale, Pair(padX, padY))
    }

    // Compute 4 rotated corners from cx, cy, w, h, angle (normalized 0-1 space)
    private fun getRotatedCorners(
        cx: Float, cy: Float,
        w: Float,  h: Float,
        angle: Float
    ): FloatArray {
        val cosA = cos(angle)
        val sinA = sin(angle)
        val hw = w / 2f
        val hh = h / 2f
        return floatArrayOf(
            cx + cosA * (-hw) - sinA * (-hh),
            cy + sinA * (-hw) + cosA * (-hh),
            cx + cosA * ( hw) - sinA * (-hh),
            cy + sinA * ( hw) + cosA * (-hh),
            cx + cosA * ( hw) - sinA * ( hh),
            cy + sinA * ( hw) + cosA * ( hh),
            cx + cosA * (-hw) - sinA * ( hh),
            cy + sinA * (-hw) + cosA * ( hh)
        )
    }

    // Axis-aligned bounding box from rotated corners
    private fun cornersToAABB(corners: FloatArray): RectF {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (i in corners.indices step 2) {
            if (corners[i]     < minX) minX = corners[i]
            if (corners[i]     > maxX) maxX = corners[i]
            if (corners[i + 1] < minY) minY = corners[i + 1]
            if (corners[i + 1] > maxY) maxY = corners[i + 1]
        }
        return RectF(
            minX.coerceIn(0f, 1f),
            minY.coerceIn(0f, 1f),
            maxX.coerceIn(0f, 1f),
            maxY.coerceIn(0f, 1f)
        )
    }

    fun detect(bitmap: Bitmap): List<DetectionResult> {
        if (isClosed) return emptyList()
        val interp = interpreter ?: return emptyList()
        val results = mutableListOf<DetectionResult>()

        try {
            val (letterboxed, scale, padding) = letterbox(bitmap)
            val (padX, padY) = padding

            val inputBuffer = ByteBuffer
                .allocateDirect(1 * inputSize * inputSize * 3 * 4)
                .apply { order(ByteOrder.nativeOrder()) }

            val pixels = IntArray(inputSize * inputSize)
            letterboxed.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
            val isNCHW = interp.getInputTensor(0).shape()[1] == 3
            if (isNCHW) {
                // NCHW format: all R first, then all G, then all B
                val totalPx = inputSize * inputSize
                for (px in pixels) inputBuffer.putFloat(((px shr 16) and 0xFF) / 255f) // R
                for (px in pixels) inputBuffer.putFloat(((px shr 8)  and 0xFF) / 255f) // G
                for (px in pixels) inputBuffer.putFloat((px           and 0xFF) / 255f) // B
            } else {
                // NHWC format: RGB RGB RGB...
                for (px in pixels) {
                    inputBuffer.putFloat(((px shr 16) and 0xFF) / 255f)
                    inputBuffer.putFloat(((px shr 8)  and 0xFF) / 255f)
                    inputBuffer.putFloat((px           and 0xFF) / 255f)
                }
            }
            inputBuffer.rewind()
            letterboxed.recycle()

            val outShape = interp.getOutputTensor(0).shape()
            Log.d(TAG, "Model output shape: ${outShape.toList()}")

            // Detect whether model outputs [1, max_det, 7] (postprocessed) or [1, 12, numAnchors] (raw)
            val isPostprocessed = (outShape.size == 3 && outShape[2] == 7)
            val totalOutputFloats = outShape.fold(1) { acc, dim -> acc * dim }

            val outputBuffer = ByteBuffer
                .allocateDirect(totalOutputFloats * 4)
                .apply { order(ByteOrder.nativeOrder()) }

            synchronized(this) {
                if (isClosed) return emptyList()
                interp.run(inputBuffer, outputBuffer)
            }
            outputBuffer.rewind()

            val data = FloatArray(totalOutputFloats) { outputBuffer.float }

            // Active image area after letterbox padding
            val activeW = inputSize - 2 * padX
            val activeH = inputSize - 2 * padY

            if (isPostprocessed) {
                // Ultralytics OBB TFLite Postprocessed format: [1, numDets (e.g. 300), 7]
                // 7 values per detection: [cx, cy, w, h, score, class_id, angle]
                val numDets = outShape[1]
                for (i in 0 until numDets) {
                    val base = i * 7
                    val score = data[base + 4]
                    val maxClass = data[base + 5].toInt().coerceIn(0, NUM_CLASSES - 1)

                    val threshold = if (labels.getOrNull(maxClass) == "Rhizoctonia") {
                        RHIZOCTONIA_CONFIDENCE_THRESHOLD
                    } else {
                        CONFIDENCE_THRESHOLD
                    }
                    if (score < threshold) continue

                    val cxRaw = data[base + 0]
                    val cyRaw = data[base + 1]
                    val wRaw  = data[base + 2]
                    val hRaw  = data[base + 3]
                    val angle = data[base + 6]

                    // Convert coordinates: handle both normalized [0..1] and pixel space [0..inputSize]
                    val isNormalized = (cxRaw <= 1.05f && cyRaw <= 1.05f && wRaw <= 1.05f && hRaw <= 1.05f)
                    val cxPx = if (isNormalized) cxRaw * inputSize else cxRaw
                    val cyPx = if (isNormalized) cyRaw * inputSize else cyRaw
                    val wPx  = if (isNormalized) wRaw  * inputSize else wRaw
                    val hPx  = if (isNormalized) hRaw  * inputSize else hRaw

                    // Remove letterbox padding → active image space
                    val cxActive = cxPx - padX
                    val cyActive = cyPx - padY

                    // Normalize to 0-1 in original image space
                    val cx = cxActive / activeW
                    val cy = cyActive / activeH
                    val w  = wPx / activeW
                    val h  = hPx / activeH

                    if (w < MIN_BOX_SIZE || h < MIN_BOX_SIZE) continue

                    val corners = getRotatedCorners(cx, cy, w, h, angle)
                    val aabb = cornersToAABB(corners)

                    if ((aabb.right  - aabb.left) < MIN_BOX_SIZE ||
                        (aabb.bottom - aabb.top)  < MIN_BOX_SIZE) continue

                    results.add(DetectionResult(
                        boundingBox = aabb,
                        corners     = corners,
                        angle       = angle,
                        label       = labels.getOrElse(maxClass) { "Class $maxClass" },
                        confidence  = score
                    ))
                }
            } else {
                // Raw YOLO OBB format: [1, 12, numAnchors]
                val numFields  = 12
                val numAnchors = totalOutputFloats / numFields

                for (a in 0 until numAnchors) {
                    var maxScore = 0f
                    var maxClass = 0
                    for (c in 0 until NUM_CLASSES) {
                        val score = sigmoid(data[(5 + c) * numAnchors + a])
                        if (score > maxScore) {
                            maxScore = score
                            maxClass = c
                        }
                    }

                    val threshold = if (labels.getOrNull(maxClass) == "Rhizoctonia") {
                        RHIZOCTONIA_CONFIDENCE_THRESHOLD
                    } else {
                        CONFIDENCE_THRESHOLD
                    }
                    if (maxScore < threshold) continue

                    val cxNorm = data[0 * numAnchors + a]
                    val cyNorm = data[1 * numAnchors + a]
                    val wNorm  = data[2 * numAnchors + a]
                    val hNorm  = data[3 * numAnchors + a]
                    val angle  = data[4 * numAnchors + a]

                    val cxPx = cxNorm * inputSize
                    val cyPx = cyNorm * inputSize
                    val wPx  = wNorm  * inputSize
                    val hPx  = hNorm  * inputSize

                    val cxActive = cxPx - padX
                    val cyActive = cyPx - padY

                    val cx = cxActive / activeW
                    val cy = cyActive / activeH
                    val w  = wPx / activeW
                    val h  = hPx / activeH

                    if (w < MIN_BOX_SIZE || h < MIN_BOX_SIZE) continue

                    val corners = getRotatedCorners(cx, cy, w, h, angle)
                    val aabb = cornersToAABB(corners)

                    if ((aabb.right  - aabb.left) < MIN_BOX_SIZE ||
                        (aabb.bottom - aabb.top)  < MIN_BOX_SIZE) continue

                    results.add(DetectionResult(
                        boundingBox = aabb,
                        corners     = corners,
                        angle       = angle,
                        label       = labels.getOrElse(maxClass) { "Class $maxClass" },
                        confidence  = maxScore
                    ))
                }
            }

            val nmsResults = applyNMS(results)
            Log.d(TAG, "Raw: ${results.size}, After NMS: ${nmsResults.size}")
            nmsResults.forEach {
                Log.d(TAG, "  → ${it.label} ${(it.confidence * 100).toInt()}% " +
                        "angle=${it.angle} box=${it.boundingBox}")
            }
            return nmsResults

        } catch (e: Exception) {
            Log.e(TAG, "Detection error: ${e.message}", e)
        }
        return emptyList()
    }

    private fun applyNMS(detections: List<DetectionResult>): List<DetectionResult> {
        if (detections.isEmpty()) return emptyList()
        val sorted = detections.sortedByDescending { it.confidence }.toMutableList()
        val kept = mutableListOf<DetectionResult>()
        while (sorted.isNotEmpty() && kept.size < MAX_DETECTIONS) {
            val best = sorted.removeAt(0)
            kept.add(best)
            sorted.removeAll { iou(best.boundingBox, it.boundingBox) > IOU_THRESHOLD }
        }
        return kept
    }

    private fun iou(a: RectF, b: RectF): Float {
        val interLeft   = maxOf(a.left,   b.left)
        val interTop    = maxOf(a.top,    b.top)
        val interRight  = minOf(a.right,  b.right)
        val interBottom = minOf(a.bottom, b.bottom)
        val interW = (interRight  - interLeft).coerceAtLeast(0f)
        val interH = (interBottom - interTop ).coerceAtLeast(0f)
        val interArea = interW * interH
        val aArea = (a.right - a.left) * (a.bottom - a.top)
        val bArea = (b.right - b.left) * (b.bottom - b.top)
        val unionArea = aArea + bArea - interArea
        return if (unionArea <= 0f) 0f else interArea / unionArea
    }

    fun close() {
        isClosed = true
        synchronized(this) {
            interpreter?.close()
            interpreter = null
        }
    }
}