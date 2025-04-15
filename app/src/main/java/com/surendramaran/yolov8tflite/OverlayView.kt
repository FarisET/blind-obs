package com.surendramaran.yolov8tflite

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.surendramaran.yolov8tflite.DetectionUtils.classThresholds
import java.util.LinkedList
import kotlin.math.max

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    // Class-specific minimum area thresholds (normalized area: w * h)
//    private val classThresholds = mapOf(
//        // High priority obstacles (lower thresholds)
//        "person" to 0.03f,    // 3% of screen area
//        "bicycle" to 0.04f,
//        "car" to 0.05f,
//        "motorcycle" to 0.04f,
//        "bus" to 0.06f,
//        "truck" to 0.07f,
//
//        // Medium priority
//        "traffic light" to 0.02f,
//        "fire hydrant" to 0.015f,
//        "stop sign" to 0.01f,
//        "bench" to 0.04f,
//        "dog" to 0.02f,
//        "cat" to 0.015f,
//
//        // Low priority/rare obstacles (higher thresholds)
//        "chair" to 0.05f,
//        "couch" to 0.08f,
//        "potted plant" to 0.04f,
//        "bed" to 0.1f,
//        "dining table" to 0.1f,
//        "tv" to 0.05f,
//        "laptop" to 0.02f,
//        "mouse" to 0.003f,
//        "remote" to 0.004f,
//        "keyboard" to 0.015f,
//        "cell phone" to 0.005f,
//        "microwave" to 0.03f,
//        "oven" to 0.04f,
//        "toaster" to 0.01f,
//
//        // Default threshold for unlisted classes
//        "default" to 0.05f
//    )

    // Position grid weights (higher = more dangerous area)
//    private val positionWeights = mapOf(
//        "floor" to 1.2f,   // Bottom 20% of screen (trip hazards)
//        "center" to 1.0f,  // Central path
//        "left" to 0.7f,
//        "right" to 0.7f
//    )

    private var results = listOf<BoundingBox>()
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()



    private var bounds = Rect()

    init {
        initPaints()
    }


    fun clear() {
        results = listOf()
        invalidate()
    }

    private fun initPaints() {
        textBackgroundPaint.apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            textSize = 50f
        }

        textPaint.apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            textSize = 50f
        }

        boxPaint.apply {
            color = ContextCompat.getColor(context!!, R.color.bounding_box_color)
            strokeWidth = 8F
            style = Paint.Style.STROKE
        }
    }



    private val gridPaint = Paint().apply {
        color = Color.argb(100, 255, 255, 255) // Semi-transparent white
        style = Paint.Style.STROKE
        strokeWidth = 4f
        pathEffect = DashPathEffect(floatArrayOf(20f, 10f), 0f) // Dashed lines
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        drawPositionGrid(canvas)

        val prioritized = DetectionUtils.filterAndPrioritize(results).take(1)
        prioritized.forEach { box ->
            drawBoundingBox(canvas, box)
            drawPositionIndicator(canvas, box)
        }


    }

    private fun drawPositionGrid(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()

        // Floor zone (bottom 20%)
        val floorLineY = height * 0.8f
        canvas.drawLine(0f, floorLineY, width, floorLineY, gridPaint)

        // Center path boundaries (30% and 70% of width)
        val leftCenterLineX = width * 0.3f
        val rightCenterLineX = width * 0.7f
        canvas.drawLine(leftCenterLineX, 0f, leftCenterLineX, floorLineY, gridPaint)
        canvas.drawLine(rightCenterLineX, 0f, rightCenterLineX, floorLineY, gridPaint)

        // Label each zone
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 40f
        }

        // Floor label
        canvas.drawText("FLOOR ZONE", width * 0.4f, height * 0.9f, textPaint)

        // Center label
        canvas.drawText("CENTER PATH", width * 0.4f, height * 0.4f, textPaint)

        // Side labels
        canvas.drawText("LEFT", width * 0.1f, height * 0.4f, textPaint)
        canvas.drawText("RIGHT", width * 0.8f, height * 0.4f, textPaint)
    }


    private fun drawPositionIndicator(canvas: Canvas, box: BoundingBox) {
        val position = when {
            box.y2 > 0.8f -> "FLOOR"
            box.cx < 0.3f -> "LEFT"
            box.cx > 0.7f -> "RIGHT"
            else -> "CENTER"
        }

        // Draw position text below bounding box
        val text = "${box.clsName} - $position"
        val textY = box.y2 * height + 50f // 50px below box

        canvas.drawText(
            text,
            box.x1 * width,
            textY,
            textPaint
        )
    }

    private fun drawBoundingBox(canvas: Canvas, box: BoundingBox) {
        val left = box.x1 * width
        val top = box.y1 * height
        val right = box.x2 * width
        val bottom = box.y2 * height

        // Draw bounding box
        canvas.drawRect(left, top, right, bottom, boxPaint)

        // Draw label
        val boxWidthPx = (box.w * width).toInt()
        val boxHeightPx = (box.h * height).toInt()
        val labelText = "${box.clsName} (${"%.2f".format(box.cnf)}) [${boxWidthPx}x${boxHeightPx}]"

        textBackgroundPaint.getTextBounds(labelText, 0, labelText.length, bounds)
        canvas.drawRect(
            left,
            top,
            left + bounds.width() + BOUNDING_RECT_TEXT_PADDING,
            top + bounds.height() + BOUNDING_RECT_TEXT_PADDING,
            textBackgroundPaint
        )
        canvas.drawText(labelText, left, top + bounds.height(), textPaint)
    }


    fun setResults(boundingBoxes: List<BoundingBox>) {
        results = boundingBoxes
        invalidate()
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}