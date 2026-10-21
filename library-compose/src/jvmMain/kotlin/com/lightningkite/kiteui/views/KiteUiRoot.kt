@file:OptIn(OverrideOnly::class)

package com.lightningkite.kiteui.views

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.PlatformStorage
import com.lightningkite.kiteui.assertMainThread
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.KeyCodeWithModifiers
import com.lightningkite.kiteui.models.KeyCodes
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.models.WindowStatistics
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.reactive.context.ReactiveContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

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

    init {
        // Elements built off the main thread race their own reactive setup, which runs on it.
        assertMainThread()
        PlatformStorage.appId = appId
        current = this
        writer.app()
    }

    /** The app's main navigator, which `appBase` binds here.  Back and closing the window go through it. */
    internal var navigator: PageNavigator? = null

    // Like Android's back button: the top dialog closes before any page does.
    private fun back() {
        if (!context.dismissTopDialog()) navigator?.goBack()
    }

    /**
     * Passes a key the focused element didn't use on to [AppState.onUniversalKeyboard]'s handlers, then to back
     * navigation (Alt+Left, as in browsers).  The window calls this; nothing in the composition can, since
     * Compose only delivers keys there while something is focused.
     */
    public fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        val key = KeyCodeWithModifiers(
            code = event.key.keyCode,
            alt = event.isAltPressed,
            ctrl = event.isCtrlPressed,
            shift = event.isShiftPressed,
            meta = event.isMetaPressed,
        )
        // A copy, since handlers may unregister themselves (or others) when called.
        if (AppState.keyboardHandlers.toList().any { it(key) }) return true
        if (key == KeyCodeWithModifiers(KeyCodes.left, alt = true)) {
            back()
            return true
        }
        return false
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    public fun Content() {
        val density = LocalDensity.current.density
        val layer = rememberGraphicsLayer()
        val scope = rememberCoroutineScope()
        SideEffect {
            this.density = density
            this.layer = layer
            animationContext = scope.coroutineContext.minusKey(Job)
        }
        root?.underlyingNativeElement?.Render(
            Modifier
                .fillMaxSize()
                .onPointerEvent(PointerEventType.Press) { if (it.button == PointerButton.Back) back() }
                .onSizeChanged {
                    AppState._windowInfo.value = WindowStatistics(Dimension(it.width / density), Dimension(it.height / density), density)
                }
                .drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                }
        )
    }

    /** Stops the app: shuts down the element tree. */
    public fun close() {
        animationContext = null
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
        Window(
            onCloseRequest = {
                // Like the web's beforeunload: a page with unsaved changes gets to ask first.
                if (root.navigator?.allowNavigate != false) {
                    root.close()
                    exitApplication()
                }
            },
            title = title,
            onKeyEvent = root::onKeyEvent,
        ) {
            val focused = LocalWindowInfo.current.isWindowFocused
            LaunchedEffect(focused) { AppState._inForeground.value = focused }
            root.Content()
        }
    }
}

/**
 * Runs a KiteUI app with no window, drawing it offscreen at [width] x [height] pixels until the process ends.
 * For driving the app with the AI driver where no display is available (CI, sandboxes).
 */
public fun kiteUiHeadless(
    appId: String,
    theme: ReactiveContext.() -> Theme,
    width: Int = 1280,
    height: Int = 800,
    app: ViewWriter.() -> Unit,
) {
    // Must be set before anything touches AWT (Dispatchers.Main is the Swing thread), or AWT looks for a display.
    System.setProperty("java.awt.headless", "true")
    runBlocking(Dispatchers.Main) {
        val root = KiteUiRoot(appId, theme, app)
        val scene = ImageComposeScene(width, height) { root.Content() }
        // Nothing else drives frames without a window; the root's screenshots read the last one drawn.
        while (true) {
            scene.render(System.nanoTime())
            delay(16)
        }
    }
}
