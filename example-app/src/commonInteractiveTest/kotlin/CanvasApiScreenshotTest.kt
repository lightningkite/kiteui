package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.testing.GraphicsMode
import com.lightningkite.kiteui.testing.GraphicsModeEnum
import com.lightningkite.kiteui.testing.JUnitRunWith
import com.lightningkite.kiteui.testing.RobolectricConfig
import com.lightningkite.kiteui.testing.RobolectricTestRunner
import com.lightningkite.kiteui.testing.withTestHarness
import com.lightningkite.kiteui.views.canvas.*
import com.lightningkite.kiteui.views.direct.*
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Screenshot tests for the new Canvas API features.
 */
@JUnitRunWith(RobolectricTestRunner::class)
@RobolectricConfig
@GraphicsMode(GraphicsModeEnum.NATIVE)
class CanvasApiScreenshotTest {

    @Test
    fun testClipping() {
        withTestHarness { harness ->
            println("\n=== Test: Canvas Clipping ===")
            harness.render {
                sizeConstraints(width = 100.px, height = 100.px).canvas {
                    delegate = object : CanvasDelegate() {
                        override fun draw(context: DrawingContext2D) = with(context) {
                            fillPaint = Color.blue
                            fillRect(0.0, 0.0, width, height)
                            save()
                            beginPath()
                            rect(20.0, 20.0, 60.0, 60.0)
                            clip()
                            fillPaint = Color.red
                            beginPath()
                            appendArc(50.0, 50.0, 40.0, Angle.zero, Angle(1f), false)
                            fill()
                            restore()
                        }
                    }
                }
            }
            harness.screenshot("canvas-clipping")
            println("✓ Clipping test complete")
        }
    }

    @Test
    fun testLineDash() {
        withTestHarness { harness ->
            println("\n=== Test: Canvas Line Dash ===")
            harness.render {
                sizeConstraints(width = 100.px, height = 100.px).canvas {
                    delegate = object : CanvasDelegate() {
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
                }
            }
            harness.screenshot("canvas-line-dash")
            println("✓ Line dash test complete")
        }
    }

    @Test
    fun testRoundRect() {
        withTestHarness { harness ->
            println("\n=== Test: Canvas RoundRect ===")
            harness.render {
                sizeConstraints(width = 100.px, height = 100.px).canvas {
                    delegate = object : CanvasDelegate() {
                        override fun draw(context: DrawingContext2D) = with(context) {
                            fillPaint = Color.green
                            beginPath()
                            roundRect(10.0, 10.0, 80.0, 80.0, 15.0)
                            fill()
                        }
                    }
                }
            }
            harness.screenshot("canvas-roundrect")
            println("✓ RoundRect test complete")
        }
    }

    @Test
    fun testRoundRectPerCorner() {
        withTestHarness { harness ->
            println("\n=== Test: Canvas RoundRect Per-Corner ===")
            harness.render {
                sizeConstraints(width = 100.px, height = 100.px).canvas {
                    delegate = object : CanvasDelegate() {
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
                        }
                    }
                }
            }
            harness.screenshot("canvas-roundrect-per-corner")
            println("✓ RoundRect per-corner test complete")
        }
    }

    @Test
    fun testEllipse() {
        withTestHarness { harness ->
            println("\n=== Test: Canvas Ellipse ===")
            harness.render {
                sizeConstraints(width = 100.px, height = 100.px).canvas {
                    delegate = object : CanvasDelegate() {
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
                }
            }
            harness.screenshot("canvas-ellipse")
            println("✓ Ellipse test complete")
        }
    }

    @Test
    fun testShadows() {
        withTestHarness { harness ->
            println("\n=== Test: Canvas Shadows ===")
            harness.render {
                sizeConstraints(width = 100.px, height = 100.px).canvas {
                    delegate = object : CanvasDelegate() {
                        override fun draw(context: DrawingContext2D) = with(context) {
                            shadowColorValue = Color.black.copy(alpha = 0.5f)
                            shadowBlur = 10.0
                            shadowOffsetX = 5.0
                            shadowOffsetY = 5.0
                            fillPaint = Color.fromHexString("#2196F3")
                            fillRect(20.0, 20.0, 60.0, 60.0)
                        }
                    }
                }
            }
            harness.screenshot("canvas-shadows")
            println("✓ Shadows test complete")
        }
    }

    @Test
    fun testLinearGradient() {
        withTestHarness { harness ->
            println("\n=== Test: Canvas Linear Gradient ===")
            harness.render {
                sizeConstraints(width = 100.px, height = 100.px).canvas {
                    delegate = object : CanvasDelegate() {
                        override fun draw(context: DrawingContext2D) = with(context) {
                            // by Claude - updated to new LinearGradient API
                            fillPaint = LinearGradient(
                                stops = listOf(
                                    GradientStop(0f, Color.red),
                                    GradientStop(0.5f, Color.yellow),
                                    GradientStop(1f, Color.green)
                                ),
                                angle = Angle.zero  // left-to-right
                            )
                            fillRect(10.0, 10.0, 80.0, 80.0)
                        }
                    }
                }
            }
            harness.screenshot("canvas-linear-gradient")
            println("✓ Linear gradient test complete")
        }
    }

    @Test
    fun testRadialGradient() {
        withTestHarness { harness ->
            println("\n=== Test: Canvas Radial Gradient ===")
            harness.render {
                sizeConstraints(width = 100.px, height = 100.px).canvas {
                    delegate = object : CanvasDelegate() {
                        override fun draw(context: DrawingContext2D) = with(context) {
                            // by Claude - updated to new RadialGradient API
                            fillPaint = RadialGradient(
                                stops = listOf(
                                    GradientStop(0f, Color.white),
                                    GradientStop(0.5f, Color.blue),
                                    GradientStop(1f, Color.fromHexString("#000080"))
                                )
                            )
                            beginPath()
                            appendArc(50.0, 50.0, 40.0, Angle.zero, Angle(1f), false)
                            fill()
                        }
                    }
                }
            }
            harness.screenshot("canvas-radial-gradient")
            println("✓ Radial gradient test complete")
        }
    }

    @Test
    fun testTransform() {
        withTestHarness { harness ->
            println("\n=== Test: Canvas Transform ===")
            harness.render {
                sizeConstraints(width = 100.px, height = 100.px).canvas {
                    delegate = object : CanvasDelegate() {
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
                }
            }
            harness.screenshot("canvas-transform")
            println("✓ Transform test complete")
        }
    }

    @Test
    fun testEvenOddFillRule() {
        withTestHarness { harness ->
            println("\n=== Test: Canvas Even-Odd Fill Rule ===")
            harness.render {
                sizeConstraints(width = 100.px, height = 100.px).canvas {
                    delegate = object : CanvasDelegate() {
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
                }
            }
            harness.screenshot("canvas-even-odd")
            println("✓ Even-odd fill rule test complete")
        }
    }

    @Test
    fun testLineCap() {
        withTestHarness { harness ->
            println("\n=== Test: Canvas Line Cap ===")
            harness.render {
                sizeConstraints(width = 100.px, height = 100.px).canvas {
                    delegate = object : CanvasDelegate() {
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
                }
            }
            harness.screenshot("canvas-line-cap")
            println("✓ Line cap test complete")
        }
    }

    @Test
    fun testBezierCurves() {
        withTestHarness { harness ->
            println("\n=== Test: Canvas Bezier Curves ===")
            harness.render {
                sizeConstraints(width = 100.px, height = 100.px).canvas {
                    delegate = object : CanvasDelegate() {
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
                }
            }
            harness.screenshot("canvas-bezier")
            println("✓ Bezier curves test complete")
        }
    }
}
