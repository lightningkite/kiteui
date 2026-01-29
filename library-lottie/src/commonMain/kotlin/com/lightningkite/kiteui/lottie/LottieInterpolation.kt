package com.lightningkite.kiteui.lottie

import com.lightningkite.kiteui.lottie.models.*
import kotlin.math.pow

/**
 * Interpolation utilities for Lottie animations.
 * Handles linear, bezier, and hold interpolation between keyframes.
 */
object LottieInterpolation {

    /**
     * Evaluate a single animated value at a given frame.
     */
    fun evaluate(value: LottieAnimatedValue, frame: Double): Double {
        if (!value.animated || value.keyframes.isEmpty()) {
            return value.staticValue
        }
        return evaluateKeyframes(value.keyframes, frame) { it?.firstOrNull() ?: 0.0 }
    }

    /**
     * Evaluate a multi-dimensional animated value at a given frame.
     * Returns a list of doubles (e.g., [x, y] for position, [r, g, b, a] for color).
     */
    fun evaluate(value: LottieAnimatedMultiValue, frame: Double): List<Double> {
        if (!value.animated || value.keyframes.isEmpty()) {
            return value.staticValue
        }
        return evaluateMultiKeyframes(value.keyframes, frame)
    }

    /**
     * Evaluate an animated bezier path at a given frame.
     */
    fun evaluate(value: LottieAnimatedShape, frame: Double): LottieBezierPath? {
        if (!value.animated || value.keyframes.isEmpty()) {
            return value.staticValue
        }
        return evaluateShapeKeyframes(value.keyframes, frame)
    }

    /**
     * Generic keyframe evaluation for scalar values.
     */
    private fun evaluateKeyframes(
        keyframes: List<LottieKeyframe>,
        frame: Double,
        getValue: (List<Double>?) -> Double
    ): Double {
        if (keyframes.isEmpty()) return 0.0

        // Before first keyframe
        if (frame <= keyframes.first().time) {
            return getValue(keyframes.first().startValue)
        }

        // After last keyframe
        if (frame >= keyframes.last().time) {
            return getValue(keyframes.last().startValue)
        }

        // Find the two keyframes we're between
        for (i in 0 until keyframes.lastIndex) {
            val k1 = keyframes[i]
            val k2 = keyframes[i + 1]

            if (frame >= k1.time && frame < k2.time) {
                // Hold keyframe - no interpolation
                if (k1.hold == 1) {
                    return getValue(k1.startValue)
                }

                val startVal = getValue(k1.startValue)
                val endVal = getValue(k2.startValue) // Next keyframe's start value

                // Calculate progress (0 to 1)
                val duration = k2.time - k1.time
                val progress = if (duration > 0) (frame - k1.time) / duration else 0.0

                // Apply easing
                val easedProgress = applyEasing(progress, k1.outTangent, k1.inTangent)

                // Linear interpolation with eased progress
                return lerp(startVal, endVal, easedProgress)
            }
        }

        return getValue(keyframes.last().startValue)
    }

    /**
     * Evaluate multi-dimensional keyframes (positions, scales, colors, etc.).
     */
    private fun evaluateMultiKeyframes(
        keyframes: List<LottieMultiKeyframe>,
        frame: Double
    ): List<Double> {
        if (keyframes.isEmpty()) return listOf(0.0, 0.0)

        // Before first keyframe
        if (frame <= keyframes.first().time) {
            return keyframes.first().startValue ?: listOf(0.0, 0.0)
        }

        // After last keyframe
        if (frame >= keyframes.last().time) {
            return keyframes.last().startValue ?: listOf(0.0, 0.0)
        }

        // Find the two keyframes we're between
        for (i in 0 until keyframes.lastIndex) {
            val k1 = keyframes[i]
            val k2 = keyframes[i + 1]

            if (frame >= k1.time && frame < k2.time) {
                // Hold keyframe
                if (k1.hold == 1) {
                    return k1.startValue ?: listOf(0.0, 0.0)
                }

                val startValues = k1.startValue ?: listOf(0.0, 0.0)
                val endValues = k2.startValue ?: listOf(0.0, 0.0)

                val duration = k2.time - k1.time
                val progress = if (duration > 0) (frame - k1.time) / duration else 0.0

                val easedProgress = applyEasing(progress, k1.outTangent, k1.inTangent)

                // Interpolate each component
                return startValues.mapIndexed { index, startVal ->
                    val endVal = endValues.getOrElse(index) { startVal }
                    lerp(startVal, endVal, easedProgress)
                }
            }
        }

        return keyframes.last().startValue ?: listOf(0.0, 0.0)
    }

    /**
     * Evaluate shape keyframes (bezier paths).
     * Interpolates vertices and tangents.
     */
    private fun evaluateShapeKeyframes(
        keyframes: List<LottieShapeKeyframe>,
        frame: Double
    ): LottieBezierPath? {
        if (keyframes.isEmpty()) return null

        // Before first keyframe
        if (frame <= keyframes.first().time) {
            return keyframes.first().startValue?.firstOrNull()
        }

        // After last keyframe
        if (frame >= keyframes.last().time) {
            return keyframes.last().startValue?.firstOrNull()
        }

        // Find the two keyframes we're between
        for (i in 0 until keyframes.lastIndex) {
            val k1 = keyframes[i]
            val k2 = keyframes[i + 1]

            if (frame >= k1.time && frame < k2.time) {
                // Hold keyframe
                if (k1.hold == 1) {
                    return k1.startValue?.firstOrNull()
                }

                val startShape = k1.startValue?.firstOrNull() ?: return null
                val endShape = k2.startValue?.firstOrNull() ?: return startShape

                val duration = k2.time - k1.time
                val progress = if (duration > 0) (frame - k1.time) / duration else 0.0

                val easedProgress = applyEasing(progress, k1.outTangent, k1.inTangent)

                return interpolateBezierPaths(startShape, endShape, easedProgress)
            }
        }

        return keyframes.last().startValue?.firstOrNull()
    }

    /**
     * Interpolate between two bezier paths.
     */
    private fun interpolateBezierPaths(
        start: LottieBezierPath,
        end: LottieBezierPath,
        t: Double
    ): LottieBezierPath {
        // If vertex counts don't match, just return start or end based on progress
        if (start.vertices.size != end.vertices.size) {
            return if (t < 0.5) start else end
        }

        val interpolatedVertices = start.vertices.mapIndexed { i, startVertex ->
            val endVertex = end.vertices.getOrElse(i) { startVertex }
            startVertex.mapIndexed { j, startVal ->
                val endVal = endVertex.getOrElse(j) { startVal }
                lerp(startVal, endVal, t)
            }
        }

        val interpolatedInTangents = start.inTangents.mapIndexed { i, startTangent ->
            val endTangent = end.inTangents.getOrElse(i) { startTangent }
            startTangent.mapIndexed { j, startVal ->
                val endVal = endTangent.getOrElse(j) { startVal }
                lerp(startVal, endVal, t)
            }
        }

        val interpolatedOutTangents = start.outTangents.mapIndexed { i, startTangent ->
            val endTangent = end.outTangents.getOrElse(i) { startTangent }
            startTangent.mapIndexed { j, startVal ->
                val endVal = endTangent.getOrElse(j) { startVal }
                lerp(startVal, endVal, t)
            }
        }

        return LottieBezierPath(
            closed = start.closed, // Use start's closed state
            vertices = interpolatedVertices,
            inTangents = interpolatedInTangents,
            outTangents = interpolatedOutTangents
        )
    }

    /**
     * Apply bezier easing to a linear progress value.
     * Uses cubic bezier curve for easing.
     */
    private fun applyEasing(
        progress: Double,
        outTangent: LottieEasing?,
        inTangent: LottieEasing?
    ): Double {
        // If no easing defined, use linear interpolation
        if (outTangent == null && inTangent == null) {
            return progress
        }

        // Get bezier control points
        // Lottie uses normalized coordinates (0-1) for easing
        val ox = outTangent?.getX()?.firstOrNull() ?: 0.0
        val oy = outTangent?.getY()?.firstOrNull() ?: 0.0
        val ix = inTangent?.getX()?.firstOrNull() ?: 1.0
        val iy = inTangent?.getY()?.firstOrNull() ?: 1.0

        // Cubic bezier with control points at (ox, oy) and (ix, iy)
        return cubicBezier(progress, ox, oy, ix, iy)
    }

    /**
     * Evaluate a cubic bezier curve.
     * Control points are (0,0), (x1,y1), (x2,y2), (1,1)
     */
    private fun cubicBezier(t: Double, x1: Double, y1: Double, x2: Double, y2: Double): Double {
        // Find the t value that gives us the x coordinate we want
        // Then evaluate y at that t value
        val epsilon = 0.0001
        var low = 0.0
        var high = 1.0
        var bezierT = t

        // Binary search to find the bezier t that gives us the desired x
        for (i in 0 until 20) {
            val x = bezierValue(bezierT, x1, x2)
            if (kotlin.math.abs(x - t) < epsilon) break

            if (x < t) {
                low = bezierT
            } else {
                high = bezierT
            }
            bezierT = (low + high) / 2.0
        }

        return bezierValue(bezierT, y1, y2)
    }

    /**
     * Calculate bezier value at t.
     * For a cubic bezier from 0 to 1 with control points at p1 and p2.
     */
    private fun bezierValue(t: Double, p1: Double, p2: Double): Double {
        val oneMinusT = 1.0 - t
        return 3.0 * oneMinusT * oneMinusT * t * p1 +
               3.0 * oneMinusT * t * t * p2 +
               t * t * t
    }

    /**
     * Linear interpolation between two values.
     */
    private fun lerp(start: Double, end: Double, t: Double): Double {
        return start + (end - start) * t
    }
}

/**
 * Extension to easily evaluate an animated value at a frame.
 */
fun LottieAnimatedValue.at(frame: Double): Double = LottieInterpolation.evaluate(this, frame)

/**
 * Extension to easily evaluate an animated multi-value at a frame.
 */
fun LottieAnimatedMultiValue.at(frame: Double): List<Double> = LottieInterpolation.evaluate(this, frame)

/**
 * Extension to easily evaluate an animated shape at a frame.
 */
fun LottieAnimatedShape.at(frame: Double): LottieBezierPath? = LottieInterpolation.evaluate(this, frame)
