package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.canvas.*
import com.lightningkite.kiteui.views.direct.*
import kotlin.math.PI

@Routable("canvas-api-test")
object CanvasApiTestPage : Page {
    override fun ViewWriter.render() {
        scrolling.col {
            h1 { content = "Canvas API Test" }
            text { content = "Testing all new canvas APIs for cross-platform consistency" }

            // Test 1: Clipping
            h3 { content = "1. Clipping (clip with rect)" }
            sizeConstraints(width = 100.px, height = 100.px).canvas { delegate = ClipTestDelegate() }

            // Test 2: Line Dash
            h3 { content = "2. Line Dash" }
            sizeConstraints(width = 100.px, height = 100.px).canvas { delegate = LineDashTestDelegate() }

            // Test 3: Round Rect (uniform)
            h3 { content = "3. RoundRect (uniform radius)" }
            sizeConstraints(width = 100.px, height = 100.px).canvas { delegate = RoundRectUniformDelegate() }

            // Test 4: Round Rect (per-corner)
            h3 { content = "4. RoundRect (per-corner radii)" }
            sizeConstraints(width = 100.px, height = 100.px).canvas { delegate = RoundRectPerCornerDelegate() }

            // Test 5: Ellipse
            h3 { content = "5. Ellipse" }
            sizeConstraints(width = 100.px, height = 100.px).canvas { delegate = EllipseTestDelegate() }

            // Test 6: Shadows
            h3 { content = "6. Shadows" }
            sizeConstraints(width = 100.px, height = 100.px).canvas { delegate = ShadowTestDelegate() }

            // Test 7: Linear Gradient with positioning
            h3 { content = "7. Linear Gradient (positioned)" }
            sizeConstraints(width = 100.px, height = 100.px).canvas { delegate = LinearGradientTestDelegate() }

            // Test 8: Radial Gradient with positioning
            h3 { content = "8. Radial Gradient (positioned)" }
            sizeConstraints(width = 100.px, height = 100.px).canvas { delegate = RadialGradientTestDelegate() }

            // Test 9: Transform
            h3 { content = "9. Transform" }
            sizeConstraints(width = 100.px, height = 100.px).canvas { delegate = TransformTestDelegate() }

            // Test 10: Even-Odd Fill Rule
            h3 { content = "10. Even-Odd Fill Rule" }
            sizeConstraints(width = 100.px, height = 100.px).canvas { delegate = EvenOddTestDelegate() }

            // Test 11: Line Cap and Join
            h3 { content = "11. Line Cap & Join" }
            sizeConstraints(width = 100.px, height = 100.px).canvas { delegate = LineCapJoinTestDelegate() }

            // Test 12: Bezier curves
            h3 { content = "12. Bezier Curves" }
            sizeConstraints(width = 100.px, height = 100.px).canvas { delegate = BezierTestDelegate() }

            space { }
        }
    }
}

// Test Delegates

class ClipTestDelegate : CanvasDelegate() {
    override fun draw(context: DrawingContext2D) = with(context) {
        // Draw a blue background
        fillPaint = Color.blue
        fillRect(0.0, 0.0, width, height)

        // Create a clipping region
        save()
        beginPath()
        rect(20.0, 20.0, 60.0, 60.0)
        clip()

        // Draw red circle - should be clipped
        fillPaint = Color.red
        beginPath()
        appendArc(50.0, 50.0, 40.0, Angle.zero, Angle(1f), false)
        fill()
        restore()
    }
}

class LineDashTestDelegate : CanvasDelegate() {
    override fun draw(context: DrawingContext2D) = with(context) {
        strokePaint = Color.black
        lineWidth = 3.0

        // Solid line
        beginPath()
        moveTo(10.0, 20.0)
        lineTo(90.0, 20.0)
        stroke()

        // Dashed line
        setLineDash(listOf(10.0, 5.0))
        beginPath()
        moveTo(10.0, 50.0)
        lineTo(90.0, 50.0)
        stroke()

        // Dotted line
        setLineDash(listOf(3.0, 3.0))
        beginPath()
        moveTo(10.0, 80.0)
        lineTo(90.0, 80.0)
        stroke()
    }
}

class RoundRectUniformDelegate : CanvasDelegate() {
    override fun draw(context: DrawingContext2D) = with(context) {
        fillPaint = Color.green
        beginPath()
        roundRect(10.0, 10.0, 80.0, 80.0, 15.0)
        fill()

        strokePaint = Color.black
        lineWidth = 2.0
        beginPath()
        roundRect(10.0, 10.0, 80.0, 80.0, 15.0)
        stroke()
    }
}

class RoundRectPerCornerDelegate : CanvasDelegate() {
    override fun draw(context: DrawingContext2D) = with(context) {
        fillPaint = Color.fromHexString("#FF9800")
        beginPath()
        roundRect(10.0, 10.0, 80.0, 80.0,
            topLeftRadius = 0.0,
            topRightRadius = 10.0,
            bottomRightRadius = 20.0,
            bottomLeftRadius = 30.0
        )
        fill()

        strokePaint = Color.black
        lineWidth = 2.0
        beginPath()
        roundRect(10.0, 10.0, 80.0, 80.0,
            topLeftRadius = 0.0,
            topRightRadius = 10.0,
            bottomRightRadius = 20.0,
            bottomLeftRadius = 30.0
        )
        stroke()
    }
}

class EllipseTestDelegate : CanvasDelegate() {
    override fun draw(context: DrawingContext2D) = with(context) {
        fillPaint = Color.fromHexString("#9C27B0")
        beginPath()
        ellipse(50.0, 50.0, 40.0, 25.0, 0.0, 0.0, 2 * PI, false)
        fill()

        // Rotated ellipse
        fillPaint = Color.fromHexString("#E91E63").copy(alpha = 0.5f)
        beginPath()
        ellipse(50.0, 50.0, 40.0, 25.0, PI / 4, 0.0, 2 * PI, false)
        fill()
    }
}

class ShadowTestDelegate : CanvasDelegate() {
    override fun draw(context: DrawingContext2D) = with(context) {
        shadowColorValue = Color.black.copy(alpha = 0.5f)
        shadowBlur = 10.0
        shadowOffsetX = 5.0
        shadowOffsetY = 5.0

        fillPaint = Color.fromHexString("#2196F3")
        fillRect(20.0, 20.0, 60.0, 60.0)
    }
}


class LinearGradientTestDelegate : CanvasDelegate() {
    override fun draw(context: DrawingContext2D) = with(context) {
        fillPaint = LinearGradient(
            stops = listOf(
                GradientStop(0f, Color.red),
                GradientStop(0.5f, Color.yellow),
                GradientStop(1f, Color.green)
            ),
            x0 = 10.0, y0 = 50.0,
            x1 = 90.0, y1 = 50.0
        )
        fillRect(10.0, 10.0, 80.0, 80.0)
    }
}

class RadialGradientTestDelegate : CanvasDelegate() {
    override fun draw(context: DrawingContext2D) = with(context) {
        fillPaint = RadialGradient(
            stops = listOf(
                GradientStop(0f, Color.white),
                GradientStop(0.5f, Color.blue),
                GradientStop(1f, Color.fromHexString("#000080"))
            ),
            cx = 50.0, cy = 50.0,
            radius = 40.0
        )
        beginPath()
        appendArc(50.0, 50.0, 40.0, Angle.zero, Angle(1f), false)
        fill()
    }
}

class TransformTestDelegate : CanvasDelegate() {
    override fun draw(context: DrawingContext2D) = with(context) {
        // Draw original rect
        fillPaint = Color.gray
        fillRect(10.0, 10.0, 30.0, 30.0)

        // Save, transform, draw
        save()
        translate(50.0, 50.0)
        rotate(PI / 6) // 30 degrees

        fillPaint = Color.red
        fillRect(-15.0, -15.0, 30.0, 30.0)

        restore()

        // Test setTransform to reset
        setTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
        fillPaint = Color.blue.copy(alpha = 0.5f)
        fillRect(60.0, 10.0, 30.0, 30.0)
    }
}

class EvenOddTestDelegate : CanvasDelegate() {
    override fun draw(context: DrawingContext2D) = with(context) {
        fillPaint = Color.fromHexString("#4CAF50")
        beginPath()
        // Outer circle
        appendArc(50.0, 50.0, 40.0, Angle.zero, Angle(1f), false)
        // Inner circle (opposite direction creates hole with even-odd)
        appendArc(50.0, 50.0, 20.0, Angle.zero, Angle(1f), true)
        fillEvenOdd()
    }
}

class LineCapJoinTestDelegate : CanvasDelegate() {
    override fun draw(context: DrawingContext2D) = with(context) {
        strokePaint = Color.black
        lineWidth = 10.0

        // Butt cap
        lineCapStyle = LineCap.butt
        beginPath()
        moveTo(20.0, 20.0)
        lineTo(80.0, 20.0)
        stroke()

        // Round cap
        lineCapStyle = LineCap.round
        beginPath()
        moveTo(20.0, 50.0)
        lineTo(80.0, 50.0)
        stroke()

        // Square cap
        lineCapStyle = LineCap.square
        beginPath()
        moveTo(20.0, 80.0)
        lineTo(80.0, 80.0)
        stroke()
    }
}


class BezierTestDelegate : CanvasDelegate() {
    override fun draw(context: DrawingContext2D) = with(context) {
        strokePaint = Color.fromHexString("#673AB7")
        lineWidth = 3.0

        // Quadratic bezier
        beginPath()
        moveTo(10.0, 80.0)
        quadraticCurveTo(50.0, 10.0, 90.0, 80.0)
        stroke()

        // Cubic bezier
        strokePaint = Color.fromHexString("#FF5722")
        beginPath()
        moveTo(10.0, 50.0)
        bezierCurveTo(30.0, 10.0, 70.0, 90.0, 90.0, 50.0)
        stroke()
    }
}
