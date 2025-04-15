package com.surendramaran.yolov8tflite

// DetectionUtils.kt
object DetectionUtils {
    // Shared class thresholds
    val classThresholds = mapOf(
        "person" to 0.03f,    // 3% of screen area
        "bicycle" to 0.04f,
        "car" to 0.05f,
        "motorcycle" to 0.04f,
        "bus" to 0.06f,
        "truck" to 0.07f,

        // Medium priority
        "traffic light" to 0.02f,
        "fire hydrant" to 0.015f,
        "stop sign" to 0.01f,
        "bench" to 0.04f,
        "dog" to 0.02f,
        "cat" to 0.015f,

        // Low priority/rare obstacles (higher thresholds)
        "chair" to 0.05f,
//        "couch" to 0.08f,
        "potted plant" to 0.04f,
        //"bed" to 0.1f,
        //"dining table" to 0.1f,
        //"tv" to 0.05f,
        //"laptop" to 0.02f,
//        "mouse" to 0.003f,
//        "remote" to 0.004f,
        //"keyboard" to 0.015f,
//        "cell phone" to 0.005f,
        //"microwave" to 0.03f,
        //"oven" to 0.04f,
        //"toaster" to 0.01f,

        // Default threshold for unlisted classes
        "default" to 0.05f

    )

    // Shared position weights
    val positionWeights = mapOf(
        "floor" to 1.2f,
        "center" to 1.0f,
        "left" to 0.7f,
        "right" to 0.7f
    )

    fun calculatePriorityScore(box: BoundingBox): Float {
        val normalizedArea = box.w * box.h
        val classWeight = 1 - (classThresholds[box.clsName] ?: classThresholds["default"]!!)
        val positionWeight = getPositionWeight(box)
        return (normalizedArea * 2.5f) + (classWeight * 1.8f) + (positionWeight * 2.0f)
    }

    fun getPositionWeight(box: BoundingBox): Float {
        val (xCenter, yBottom) = box.cx to box.y2
        return when {
            yBottom > 0.8f -> positionWeights["floor"]!!
            xCenter in 0.3f..0.7f -> positionWeights["center"]!!
            xCenter < 0.3f -> positionWeights["left"]!!
            else -> positionWeights["right"]!!
        }
    }

    fun filterAndPrioritize(boxes: List<BoundingBox>): List<BoundingBox> {
        val filtered = boxes.filter { box ->
            val normalizedArea = box.w * box.h
            normalizedArea >= (classThresholds[box.clsName] ?: classThresholds["default"]!!)
        }

        return filtered.map { box ->
            Pair(box, calculatePriorityScore(box))
        }.sortedByDescending { it.second }
            .map { it.first }
    }
}