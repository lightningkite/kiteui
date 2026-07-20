package com.lightningkite.kiteui.models

/**
 * Cubic bezier easing curve defined by two control points.
 * Maps 1:1 to platform APIs: CSS cubic-bezier(), Android PathInterpolator, iOS CAMediaTimingFunction.
 *
 * Control points define the curve shape: P0=(0,0), P1=(x1,y1), P2=(x2,y2), P3=(1,1).
 */
public data class Easing(val x1: Float, val y1: Float, val x2: Float, val y2: Float) {

    /**
     * Evaluate the easing curve at parameter [t] (0..1), returning the eased value (0..1).
     * Used by platforms that don't have native cubic bezier support (e.g. Swing).
     */
    public fun evaluate(t: Float): Float {
        // Newton-Raphson iteration to find the bezier parameter for a given x value,
        // then evaluate the y component at that parameter.
        val bezierT = solveCurveX(t.toDouble())
        return bezierY(bezierT).toFloat()
    }

    private fun bezierX(t: Double): Double {
        val t2 = t * t
        val t3 = t2 * t
        return 3.0 * x1 * t * (1 - t) * (1 - t) + 3.0 * x2 * t2 * (1 - t) + t3
    }

    private fun bezierY(t: Double): Double {
        val t2 = t * t
        val t3 = t2 * t
        return 3.0 * y1 * t * (1 - t) * (1 - t) + 3.0 * y2 * t2 * (1 - t) + t3
    }

    private fun bezierXDerivative(t: Double): Double {
        val a = 3.0 * x1
        val b = 3.0 * x2
        return a * (1 - t) * (1 - t) + 2.0 * (b - a) * t * (1 - t) + (1 - b) * 3.0 * t * t
    }

    private fun solveCurveX(x: Double): Double {
        // Newton-Raphson with fallback to bisection
        var t = x
        for (i in 0 until 8) {
            val error = bezierX(t) - x
            if (kotlin.math.abs(error) < 1e-6) return t
            val d = bezierXDerivative(t)
            if (kotlin.math.abs(d) < 1e-6) break
            t -= error / d
        }
        // Bisection fallback
        var lo = 0.0
        var hi = 1.0
        t = x
        for (i in 0 until 20) {
            val xEst = bezierX(t)
            if (kotlin.math.abs(xEst - x) < 1e-6) return t
            if (x > xEst) lo = t else hi = t
            t = (lo + hi) / 2.0
        }
        return t
    }

    public companion object {
        public val Linear: Easing = Easing(0f, 0f, 1f, 1f)
        public val EaseOut: Easing = Easing(0f, 0f, 0.58f, 1f)
        public val EaseIn: Easing = Easing(0.42f, 0f, 1f, 1f)
        public val EaseInOut: Easing = Easing(0.42f, 0f, 0.58f, 1f)
        public val Spring: Easing = Easing(0.175f, 0.885f, 0.32f, 1.275f)
    }
}
