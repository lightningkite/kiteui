package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.fetch
import com.lightningkite.kiteui.lottie.LottieRenderer
import com.lightningkite.kiteui.lottie.models.LottieAnimation
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.canvas.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.*
import kotlinx.coroutines.*
import kotlin.time.TimeSource

/**
 * Test page for the pure Kotlin Lottie renderer.
 * Uses Canvas directly to render animations with LottieRenderer.
 */
@Routable("lottie-renderer-test")
object LottieRendererTestPage : Page {

    // Simple animation URLs for testing
    private const val SIMPLE_ANIMATION = "https://assets2.lottiefiles.com/packages/lf20_p8bfn5to.json"
    private const val SHAPES_ANIMATION = "https://assets5.lottiefiles.com/packages/lf20_jbrw3hcz.json"

    override val title: Reactive<String> = Constant("Lottie Renderer Test")

    override fun ViewWriter.render(): Unit = run {
        scrolling.col {
            h1 { content = "Pure Kotlin Lottie Renderer Test" }

            text { content = "This page tests the pure Kotlin LottieRenderer directly using Canvas." }

            space()

            // Test animation 1
            h2 { content = "Test Animation 1 (Loading Spinner)" }
            sizeConstraints(width = 300.px, height = 300.px).canvas {
                delegate = LottieCanvasDelegate(SIMPLE_ANIMATION)
            }

            space()

            // Test animation 2
            h2 { content = "Test Animation 2 (Checkmark)" }
            sizeConstraints(width = 300.px, height = 300.px).canvas {
                delegate = LottieCanvasDelegate(SHAPES_ANIMATION)
            }

            space()

            // Inline JSON animation test
            h2 { content = "Inline JSON Animation (Simple Circle)" }
            sizeConstraints(width = 300.px, height = 300.px).canvas {
                delegate = LottieCanvasDelegate(jsonData = SIMPLE_CIRCLE_ANIMATION)
            }

            space()

            h2 { content = "Inline JSON Animation (Rectangle)" }
            sizeConstraints(width = 300.px, height = 300.px).canvas {
                delegate = LottieCanvasDelegate(jsonData = SIMPLE_RECT_ANIMATION)
            }
        }
    }
}

/**
 * Canvas delegate that renders a Lottie animation using the pure Kotlin renderer.
 */
class LottieCanvasDelegate(
    private val url: String? = null,
    private val jsonData: String? = null
) : CanvasDelegate() {

    private var animation: LottieAnimation? = null
    private var renderer: LottieRenderer? = null
    private var startTime: TimeSource.Monotonic.ValueTimeMark? = null
    private var animationFrameRemover: (() -> Unit)? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        loadAnimation()
    }

    private fun loadAnimation() {
        scope.launch {
            try {
                val json = when {
                    jsonData != null -> jsonData
                    url != null -> fetch(url).text()
                    else -> return@launch
                }

                animation = LottieAnimation.parse(json)
                renderer = animation?.let { LottieRenderer(it) }
                startTime = TimeSource.Monotonic.markNow()

                // Start animation loop
                animationFrameRemover = AppState.animationFrame.addListener {
                    invalidate()
                }

                invalidate()
            } catch (e: Exception) {
                println("Failed to load Lottie animation: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    override fun draw(context: DrawingContext2D) {
        val anim = animation ?: run {
            // Draw loading text
            context.fillPaint = com.lightningkite.kiteui.models.Color.gray
            context.fillRect(0.0, 0.0, context.width, context.height)
            return
        }
        val rend = renderer ?: return

        context.clear()

        // Calculate current frame based on elapsed time
        val elapsed = startTime?.elapsedNow()?.inWholeMilliseconds?.toDouble() ?: 0.0
        val elapsedSeconds = elapsed / 1000.0
        val progress = (elapsedSeconds / anim.durationSeconds) % 1.0
        val frame = anim.inPoint + (progress * anim.totalFrames)

        // Scale to fit the canvas
        val canvasWidth = context.width
        val canvasHeight = context.height
        val scaleX = canvasWidth / anim.width.toDouble()
        val scaleY = canvasHeight / anim.height.toDouble()
        val scale = minOf(scaleX, scaleY)

        context.save()
        context.translate(
            (canvasWidth - anim.width * scale) / 2.0,
            (canvasHeight - anim.height * scale) / 2.0
        )
        context.scale(scale, scale)

        rend.render(context, frame)

        context.restore()
    }
}

// Simple circle animation JSON for testing
private val SIMPLE_CIRCLE_ANIMATION = """
{
  "v": "5.5.7",
  "fr": 30,
  "ip": 0,
  "op": 60,
  "w": 200,
  "h": 200,
  "layers": [
    {
      "ty": 4,
      "nm": "Circle Layer",
      "ip": 0,
      "op": 60,
      "st": 0,
      "ks": {
        "o": {"a": 0, "k": 100},
        "r": {"a": 0, "k": 0},
        "p": {"a": 0, "k": [100, 100]},
        "a": {"a": 0, "k": [0, 0]},
        "s": {"a": 1, "k": [
          {"t": 0, "s": [100, 100], "e": [150, 150], "i": {"x": [0.5], "y": [1]}, "o": {"x": [0.5], "y": [0]}},
          {"t": 30, "s": [150, 150], "e": [100, 100], "i": {"x": [0.5], "y": [1]}, "o": {"x": [0.5], "y": [0]}},
          {"t": 60, "s": [100, 100]}
        ]}
      },
      "shapes": [
        {
          "ty": "gr",
          "it": [
            {
              "ty": "el",
              "p": {"a": 0, "k": [0, 0]},
              "s": {"a": 0, "k": [80, 80]}
            },
            {
              "ty": "fl",
              "c": {"a": 1, "k": [
                {"t": 0, "s": [0.2, 0.6, 1, 1], "e": [1, 0.4, 0.4, 1], "i": {"x": [0.5], "y": [1]}, "o": {"x": [0.5], "y": [0]}},
                {"t": 30, "s": [1, 0.4, 0.4, 1], "e": [0.2, 0.6, 1, 1], "i": {"x": [0.5], "y": [1]}, "o": {"x": [0.5], "y": [0]}},
                {"t": 60, "s": [0.2, 0.6, 1, 1]}
              ]},
              "o": {"a": 0, "k": 100}
            },
            {
              "ty": "tr",
              "p": {"a": 0, "k": [0, 0]},
              "a": {"a": 0, "k": [0, 0]},
              "s": {"a": 0, "k": [100, 100]},
              "r": {"a": 0, "k": 0},
              "o": {"a": 0, "k": 100}
            }
          ]
        }
      ]
    }
  ]
}
""".trimIndent()

// Simple rectangle animation for testing
private val SIMPLE_RECT_ANIMATION = """
{
  "v": "5.5.7",
  "fr": 30,
  "ip": 0,
  "op": 90,
  "w": 200,
  "h": 200,
  "layers": [
    {
      "ty": 4,
      "nm": "Rectangle Layer",
      "ip": 0,
      "op": 90,
      "st": 0,
      "ks": {
        "o": {"a": 0, "k": 100},
        "r": {"a": 1, "k": [
          {"t": 0, "s": [0], "e": [360], "i": {"x": [0.5], "y": [1]}, "o": {"x": [0.5], "y": [0]}},
          {"t": 90, "s": [360]}
        ]},
        "p": {"a": 0, "k": [100, 100]},
        "a": {"a": 0, "k": [0, 0]},
        "s": {"a": 0, "k": [100, 100]}
      },
      "shapes": [
        {
          "ty": "gr",
          "it": [
            {
              "ty": "rc",
              "p": {"a": 0, "k": [0, 0]},
              "s": {"a": 0, "k": [60, 60]},
              "r": {"a": 0, "k": 8}
            },
            {
              "ty": "fl",
              "c": {"a": 0, "k": [0.4, 0.8, 0.4, 1]},
              "o": {"a": 0, "k": 100}
            },
            {
              "ty": "st",
              "c": {"a": 0, "k": [0.2, 0.5, 0.2, 1]},
              "o": {"a": 0, "k": 100},
              "w": {"a": 0, "k": 3}
            },
            {
              "ty": "tr",
              "p": {"a": 0, "k": [0, 0]},
              "a": {"a": 0, "k": [0, 0]},
              "s": {"a": 0, "k": [100, 100]},
              "r": {"a": 0, "k": 0},
              "o": {"a": 0, "k": 100}
            }
          ]
        }
      ]
    }
  ]
}
""".trimIndent()
