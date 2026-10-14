@file:OptIn(OverrideOnly::class)

package com.lightningkite.kiteui.views

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.PlatformStorage
import com.lightningkite.kiteui.assertMainThread
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.models.WindowStatistics
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.reactive.context.ReactiveContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import javax.swing.Timer

/**
 * Hosts a KiteUI app in Compose: builds the element tree with [app], then draws it from [Content].
 * The desktop counterpart of Android's `KiteUiActivity`.  [appId] keys the app's stored settings.
 */
public class KiteUiRoot(
    appId: String,
    private val theme: ReactiveContext.() -> Theme,
    app: ViewWriter.() -> Unit,
) {
    public val context: ElementContext = ElementContext()
    public var root: Element? by mutableStateOf(null)
        private set
    private var layer: GraphicsLayer? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val writer = object : ViewWriter, CoroutineScope by scope {
        override val context: ElementContext get() = this@KiteUiRoot.context

        @OverrideOnly
        override fun willAddChild(element: Element) {
            element::themeChoice { ThemeDerivation.SetAsBase(theme()) }
        }

        @OverrideOnly
        override fun addChild(element: Element) {
            root = element
        }
    }

    // Android drives this from a ValueAnimator that never stops; a timer is the desktop equivalent.
    private val animationFrames = Timer(16) { AppState._animationFrame.invokeAll() }

    init {
        // Elements built off the main thread race their own reactive setup, which runs on it.
        assertMainThread()
        PlatformStorage.appId = appId
        current = this
        writer.app()
        animationFrames.start()
    }

    @Composable
    public fun Content() {
        val density = LocalDensity.current.density
        this.density = density
        val layer = rememberGraphicsLayer()
        this.layer = layer
        root?.underlyingNativeElement?.Render(
            Modifier
                .fillMaxSize()
                .onSizeChanged {
                    AppState._windowInfo.value = WindowStatistics(Dimension(it.width / density), Dimension(it.height / density), density)
                }
                .drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                }
        )
    }

    /** Stops the app: shuts down the element tree and the timers driving it. */
    public fun close() {
        animationFrames.stop()
        root?.onShutdown()
        scope.cancel()
        if (current == this) current = null
    }

    /** The window's pixels per dp. */
    public var density: Float = 1f
        private set

    /** The whole window as last drawn. */
    public suspend fun screenshot(): ImageBitmap = checkNotNull(layer) { "KiteUiRoot.screenshot() before it was composed" }.toImageBitmap()

    public companion object {
        /** The root being shown; there is one per process, as with a single-activity Android app. */
        public var current: KiteUiRoot? = null
            private set
    }
}

/** Runs a KiteUI app in a desktop window until the window is closed. */
public fun kiteUiWindow(
    appId: String,
    title: String,
    theme: ReactiveContext.() -> Theme,
    app: ViewWriter.() -> Unit,
) {
    // Elements must be created on the main (Swing event) thread.
    val root = runBlocking(Dispatchers.Main) { KiteUiRoot(appId, theme, app) }
    application {
        Window(onCloseRequest = { root.close(); exitApplication() }, title = title) { root.Content() }
    }
}
