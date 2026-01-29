package com.lightningkite.kiteui.lottie.views.direct

import com.lightningkite.kiteui.lottie.models.LottieRaw
import com.lightningkite.kiteui.lottie.models.LottieRemote
import com.lightningkite.kiteui.lottie.models.LottieSource
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.core.*
import com.lightningkite.readable.*
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Lottie animation view for Swing/Desktop using JavaFX WebView bridge.
 * This embeds a WebView running lottie-web to render animations.
 */
actual class LottieView actual constructor(
    context: RContext,
    actual val source: LottieSource,
    actual val description: String,
) : RView(context) {

    private val panel = JPanel(BorderLayout())
    private var jfxPanel: JFXPanel? = null
    private var webView: WebView? = null
    private var isInitialized = false

    override val native: Component get() = panel

    private val _state = RawReactive<Unit>()
    actual val state: Reactive<Unit> = _state

    private val _duration = RawReactive<Duration?>(ReactiveState(null))
    actual val duration: Reactive<Duration?> = _duration

    private val _playing = Signal(false)
    actual val playing: MutableReactive<Boolean> = object : MutableReactive<Boolean> {
        override suspend fun set(value: Boolean) {
            _playing.value = value
            runOnFxThread {
                if (value) {
                    webView?.engine?.executeScript("if(window.lottieAnim) window.lottieAnim.play();")
                } else {
                    webView?.engine?.executeScript("if(window.lottieAnim) window.lottieAnim.pause();")
                }
            }
        }

        override val state: ReactiveState<Boolean> get() = _playing.state
        override fun addListener(listener: () -> Unit) = _playing.addListener(listener)
    }

    private var _loop = true
    actual var loop: Boolean
        get() = _loop
        set(value) {
            _loop = value
            runOnFxThread {
                webView?.engine?.executeScript("if(window.lottieAnim) window.lottieAnim.loop = $value;")
            }
        }

    private var _speed = 1f
    actual var speed: Float
        get() = _speed
        set(value) {
            _speed = value
            runOnFxThread {
                webView?.engine?.executeScript("if(window.lottieAnim) window.lottieAnim.setSpeed($value);")
            }
        }

    private val _progress = Signal(0f)
    actual val progress: MutableReactive<Float> = object : MutableReactive<Float> {
        override suspend fun set(value: Float) {
            _progress.value = value
            runOnFxThread {
                webView?.engine?.executeScript(
                    "if(window.lottieAnim) { var f = Math.floor($value * window.lottieAnim.totalFrames); window.lottieAnim.goToAndStop(f, true); }"
                )
            }
        }

        override val state: ReactiveState<Float> get() = _progress.state
        override fun addListener(listener: () -> Unit) = _progress.addListener(listener)
    }

    private var _autoPlay = true
    actual var autoPlay: Boolean
        get() = _autoPlay
        set(value) { _autoPlay = value }

    private val _completedPlay = mutableListOf<() -> Unit>()
    actual val completedPlay: Listenable = object : Listenable {
        override fun addListener(listener: () -> Unit): () -> Unit {
            _completedPlay.add(listener)
            return { _completedPlay.remove(listener) }
        }
    }

    init {
        panel.accessibleContext.accessibleDescription = description

        // Initialize JavaFX
        initJavaFX()
    }

    private fun initJavaFX() {
        SwingUtilities.invokeLater {
            jfxPanel = JFXPanel()
            panel.add(jfxPanel, BorderLayout.CENTER)

            Platform.runLater {
                try {
                    webView = WebView()
                    val scene = Scene(webView)
                    jfxPanel?.scene = scene

                    // Load lottie-web from CDN and set up the animation
                    val htmlContent = buildLottieHtml()
                    webView?.engine?.loadContent(htmlContent)

                    webView?.engine?.loadWorker?.stateProperty()?.addListener { _, _, newState ->
                        if (newState == Worker.State.SUCCEEDED) {
                            isInitialized = true
                            // Get duration from the animation
                            try {
                                val result = webView?.engine?.executeScript(
                                    "window.lottieDuration || 0"
                                )
                                val durationMs = (result as? Number)?.toLong() ?: 0L
                                if (durationMs > 0) {
                                    _duration.state = ReactiveState(durationMs.milliseconds)
                                }
                                _state.state = ReactiveState(Unit)
                            } catch (e: Exception) {
                                // Duration not available yet
                            }
                        } else if (newState == Worker.State.FAILED) {
                            _state.state = ReactiveState.exception(
                                Exception("Failed to load Lottie animation")
                            )
                        }
                    }
                } catch (e: Exception) {
                    _state.state = ReactiveState.exception(e)
                }
            }
        }
    }

    private fun buildLottieHtml(): String {
        val animationConfig = when (val src = source) {
            is LottieRemote -> """path: "${src.url}""""
            is LottieRaw -> """animationData: ${src.json}"""
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    * { margin: 0; padding: 0; }
                    html, body { width: 100%; height: 100%; overflow: hidden; }
                    #lottie { width: 100%; height: 100%; }
                </style>
                <script src="https://cdnjs.cloudflare.com/ajax/libs/lottie-web/5.12.2/lottie.min.js"></script>
            </head>
            <body>
                <div id="lottie"></div>
                <script>
                    window.lottieAnim = lottie.loadAnimation({
                        container: document.getElementById('lottie'),
                        renderer: 'svg',
                        loop: ${_loop},
                        autoplay: ${_autoPlay},
                        $animationConfig
                    });

                    window.lottieAnim.addEventListener('DOMLoaded', function() {
                        var totalFrames = window.lottieAnim.totalFrames || 0;
                        var frameRate = window.lottieAnim.frameRate || 30;
                        window.lottieDuration = Math.round((totalFrames / frameRate) * 1000);
                    });

                    window.lottieAnim.addEventListener('complete', function() {
                        window.lottieComplete = true;
                    });
                </script>
            </body>
            </html>
        """.trimIndent()
    }

    private fun runOnFxThread(action: () -> Unit) {
        // Only run if JavaFX is initialized (webView exists means JFXPanel was created)
        if (webView == null) return
        try {
            if (Platform.isFxApplicationThread()) {
                action()
            } else {
                Platform.runLater(action)
            }
        } catch (e: IllegalStateException) {
            // Toolkit not initialized yet - ignore, initial values are in HTML
        }
    }

    actual fun play() {
        _playing.value = true
        runOnFxThread {
            webView?.engine?.executeScript("if(window.lottieAnim) window.lottieAnim.play();")
        }
    }

    actual fun pause() {
        _playing.value = false
        runOnFxThread {
            webView?.engine?.executeScript("if(window.lottieAnim) window.lottieAnim.pause();")
        }
    }

    actual fun stop() {
        _playing.value = false
        _progress.value = 0f
        runOnFxThread {
            webView?.engine?.executeScript("if(window.lottieAnim) window.lottieAnim.stop();")
        }
    }

    actual fun seekToFrame(frame: Int) {
        runOnFxThread {
            webView?.engine?.executeScript("if(window.lottieAnim) window.lottieAnim.goToAndStop($frame, true);")
        }
    }

    actual fun seekToProgress(progress: Float) {
        _progress.value = progress
        runOnFxThread {
            webView?.engine?.executeScript(
                "if(window.lottieAnim) { var f = Math.floor($progress * window.lottieAnim.totalFrames); window.lottieAnim.goToAndStop(f, true); }"
            )
        }
    }
}
