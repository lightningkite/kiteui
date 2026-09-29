package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.ImageSource
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.models.VideoSource
import com.lightningkite.kiteui.models.VisualMediaSource
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.NativeElementCommonCode
import com.lightningkite.kiteui.views.areAnimationsEnabled
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.theme
import com.lightningkite.kiteui.views.themed
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.flatten
import com.lightningkite.reactive.lensing.lens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit


public class MediaView(private val frame: Frame) : Element by frame {
    public constructor(context: ElementContext) : this(Frame(context))

    public data class Info(
        val sources: List<VisualMediaSource>,
        val scaleType: ImageScaleType,
        val description: String?,
        val key: Any? = null,
        val isPlaceholder: Boolean = false
    )

    public val currentRawMediaView: Signal<Element?> = Signal<Element?>(null)

    init {
        val removeListener = currentRawMediaView.addListener {
            (currentRawMediaView.value as? RawVideoView)?.showControls = showControls
            (currentRawMediaView.value as? RawVideoView)?.loop = loop
        }
        onRemove(removeListener)
        onRemove {
            (currentRawMediaView.value as? RawVideoView)?.let { videoView ->
                launch { videoView.playing set false }
            }

            lastRender?.forEach { render ->
                if (render is RawVideoView) launch { render.playing set false }
            }
            retiredRenders.forEach { render ->
                if (render is RawVideoView) launch { render.playing set false }
            }
        }
    }

    public var info: Info? = null
        set(value) {
            if (field != value) {
                field = value
                if (ready) refresh()
            }
        }
    public var source: VisualMediaSource?
        get() = info?.sources?.firstOrNull()
        set(value) {
            info = info?.copy(sources = listOfNotNull(value)) ?: Info(listOfNotNull(value), ImageScaleType.Fit, null)
        }
    public var scaleType: ImageScaleType
        get() = info?.scaleType ?: ImageScaleType.Fit
        set(value) {
            info = info?.copy(scaleType = value) ?: Info(listOf(), value, null)
        }
    public var description: String?
        get() = info?.description
        set(value) {
            info = info?.copy(description = value) ?: Info(listOf(), ImageScaleType.Fit, value)
        }

    public var opaqueTransitions: Boolean = false

    /**
     * Controls the theme used for background drawing behind rendered media.
     * Defaults to "never draw a themed background" behavior.
     * To restore the original MediaView behavior (draw the background only
     * when the frame's theme says so), set this to:
     *
     *   backgroundThemeDerivation = ThemeDerivation {
     *       if (frame.themeAndBack.drawBackground) it.withBack else it.withoutBack
     *   }
     *
     * Set before the first `refresh()` (i.e. before onStartup / info is assigned)
     * for it to apply cleanly, since it's only read while building new renders.
     */
    public var backgroundThemeDerivation: ThemeDerivation = ThemeDerivation { it.withoutBack }

    public var showControls: Boolean = false
        set(value) {
            field = value
            (currentRawMediaView.value as? RawVideoView)?.showControls = value
        }
    public var loop: Boolean = false
        set(value) {
            field = value
            (currentRawMediaView.value as? RawVideoView)?.loop = value
        }

    public val time: MutableReactive<Double> = currentRawMediaView.lens(get = {
        (it as? RawVideoView)?.currentTime?.lens(
            get = { it.toDouble(DurationUnit.SECONDS) },
            set = { it.seconds }) ?: Signal(0.0)
    }).flatten()
    public val playing: MutableReactive<Boolean> =
        currentRawMediaView.lens { (it as? RawVideoView)?.playing ?: Signal(false) }.flatten()
    public val volume: MutableReactive<Float> =
        currentRawMediaView.lens { (it as? RawVideoView)?.volume ?: Signal(0f) }.flatten()


    public var ready: Boolean = false

    @OverrideOnly
    override fun onStartup() {
        frame.onStartup()
        ready = true
        refresh()
    }

    @OverrideOnly
    override fun onShutdown() {
        ready = false
        // The parent removes this frame from its native hierarchy before calling
        // onShutdown. Let the frame shut down its children without synchronously
        // calling removeAllViews(): shutdown can be triggered during a layout pass,
        // and mutating FrameLayout's native child array from inside onMeasure can
        // make FrameLayout.onMeasure read a null child.
        lastRender = null
        retiredRenders.clear()
        frame.onShutdown()
    }

    private var lastRendered: Info? = null
    private var lastRender: List<Element>? = null
    private val retiredRenders = ArrayList<Element>()

    public val activityIndicator: ActivityIndicator = frame.centered.activityIndicator { opacity = 0.0 }

    private fun removeChildIfPresent(element: Element) {
        if (frame.children.any { it === element }) {
            frame.removeChild(element)
        }
    }

    /**
     * Schedule removals on a later main-loop turn. Load callbacks (Glide size-ready,
     * reactive state) can run while a FrameLayout is still inside onMeasure; removeViewAt
     * in that window leaves getChildAt(i) null and NPEs on getVisibility().
     */
    private fun removeChildrenLater(elements: Collection<Element>) {
        if (elements.isEmpty()) return
        val toRemove = elements.toList()
        afterTimeout(0L) {
            toRemove.forEach { removeChildIfPresent(it) }
        }
    }

    /**
     * Flushes anything sitting in retiredRenders. Safe to call multiple times
     * (e.g. once from a success handler, and again defensively from an
     * exception handler) since it clears the list up front.
     */
    private fun removeRetiredRenders() {
        val renders = retiredRenders.toList()
        retiredRenders.clear()
        renders.forEach {
            if (frame.areAnimationsEnabled && !opaqueTransitions) {
                // For crossfades, wait exactly the transition duration (removed the + 500ms hack)
                afterTimeout(it.theme.transitionDuration.inWholeMilliseconds) {
                    removeChildrenLater(listOf(it))
                }
            } else {
                // Opaque: drop the old media as soon as the new one is ready — but never
                // from inside the load callback itself (see removeChildrenLater).
                removeChildrenLater(listOf(it))
            }
        }
    }

    public val shownInfo: RawReactive<Info?> = RawReactive<Info?>(ReactiveState(null))
    public var cannotBeCovered: Boolean = false

    @OptIn(ExperimentalKiteUi::class)
    public fun refresh() {
        if (!ready) return
        val info = info
        if (lastRendered != info) {
            // Determine if we are swapping to completely new media
            val keyChanged = lastRendered != null && info != null && lastRendered?.key != info.key
            val replacingPlaceholder = lastRendered?.isPlaceholder == true
            val shouldAnimateOut = keyChanged && !replacingPlaceholder

            lastRender?.let { renders ->
                // 1. Unconditionally stop playback on outgoing media
                renders.forEach { render ->
                    if (render is RawVideoView) {
                        launch { render.playing set false }
                    }
                }

                // 2. Only run the fade-out animations if we are actually swapping content
                if (shouldAnimateOut) {
                    renders.forEach { render ->
                        if (!opaqueTransitions) {
                            render.opacity = 0.0
                        }

                        if (render is RawVideoView) {
                            launch {
                                val transitionTime = render.theme.transitionDuration * 3 / 4
                                val start = Clock.System.now()
                                val startVolume = render.volume()
                                while (Clock.System.now() - start < transitionTime) {
                                    delay(1.seconds / 30)
                                    val t = ((Clock.System.now() - start) / transitionTime).toFloat().coerceIn(0f, 1f)
                                    render.volume set startVolume * (1f - t)
                                }
                                render.volume set 0f
                            }
                        }
                    }
                }
                retiredRenders.addAll(renders)
            }
            lastRender = null
            shownInfo.state = ReactiveState.notReady
            lastRendered = info
            activityIndicator.opacity = 1.0
            lastRender = info?.let { info ->
                val self = this@MediaView

                buildList {
                    for ((sourceIndex, source) in info.sources.withIndex()) {
                        when (source) {
                            is ImageSource -> {
                                add(
                                    frame.themed(
                                        backgroundThemeDerivation
                                    ).rawImage(source, info.description ?: "", info.scaleType) {
                                        themeBase = NativeElementCommonCode.GetBaseTheme.fromParentNonCascading
                                        themeChoice

                                        // 1. DO NOT start invisible if opaqueTransitions is true
                                        if (!self.opaqueTransitions) {
                                            opacity = 0.0
                                        }

                                        reactive {
                                            this@rawImage.state.state().handle(
                                                success = {
                                                    if (self.lastRendered == info) {
                                                        // 2. DO NOT animate fade-in if opaqueTransitions is true
                                                        if (!self.opaqueTransitions) opacity = 1.0

                                                        self.lastRender?.take(sourceIndex)
                                                            ?.let(self::removeChildrenLater)
                                                        self.removeRetiredRenders()
                                                        self.activityIndicator.opacity = 0.0
                                                        self.shownInfo.state = ReactiveState(info)
                                                        self.currentRawMediaView.value = this@rawImage
                                                    }
                                                },
                                                exception = {
                                                    if (self.lastRendered == info) {
                                                        self.activityIndicator.opacity = 0.0
                                                        self.shownInfo.state = ReactiveState.exception(it)
                                                        self.lastRendered = null
                                                        // Load failed: don't leave the previous
                                                        // render(s) stranded in retiredRenders.
                                                        self.removeRetiredRenders()
                                                        if (self.info !== info) {
                                                            self.refresh()
                                                        }
                                                    }
                                                },
                                                notReady = {}
                                            )
                                        }
                                    })
                            }

                            is VideoSource -> {
                                add(
                                    frame.themed(
                                        backgroundThemeDerivation
                                    ).rawVideo(source, info.description ?: "", info.scaleType) {
                                        themeBase = NativeElementCommonCode.GetBaseTheme.fromParentNonCascading
                                        themeChoice

                                        // 3. DO NOT start invisible if opaqueTransitions is true
                                        if (!self.opaqueTransitions) {
                                            opacity = 0.0
                                        }

                                        launch { volume set 0f }
                                        this.showControls = this@MediaView.showControls
                                        this.loop = this@MediaView.loop
                                        reactive {
                                            this@rawVideo.state.state().handle(
                                                success = {
                                                    if (self.lastRendered == info) {
                                                        launch {
                                                            // (keep the audio volume fade-in)
                                                            val transitionTime = theme.transitionDuration * 3 / 4
                                                            val start = Clock.System.now()
                                                            while (Clock.System.now() - start < transitionTime) {
                                                                delay(1.seconds / 30)
                                                                volume set ((Clock.System.now() - start) / transitionTime).toFloat()
                                                                    .coerceIn(0f, 1f)
                                                            }
                                                            volume set 1f
                                                        }

                                                        // 4. DO NOT animate fade-in if opaqueTransitions is true
                                                        if (!self.opaqueTransitions) opacity = 1.0

                                                        self.lastRender?.take(sourceIndex)
                                                            ?.let(self::removeChildrenLater)
                                                        self.removeRetiredRenders()
                                                        self.activityIndicator.opacity = 0.0
                                                        self.shownInfo.state = ReactiveState(info)
                                                        self.currentRawMediaView.value = this@rawVideo
                                                    }
                                                },
                                                exception = {
                                                    if (self.lastRendered == info) {
                                                        self.activityIndicator.opacity = 0.0
                                                        self.shownInfo.state = ReactiveState.exception(it)
                                                        self.lastRendered = null
                                                        // Load failed: don't leave the previous
                                                        // render(s) stranded in retiredRenders.
                                                        self.removeRetiredRenders()
                                                        if (self.info !== info) {
                                                            self.refresh()
                                                        }
                                                    }
                                                },
                                                notReady = {}
                                            )
                                        }
                                    })
                            }
                        }
                    }
                }
            } ?: run {
                removeRetiredRenders()
                this@MediaView.shownInfo.state = ReactiveState(null)
                activityIndicator.opacity = 0.0
                null
            }
        }
    }

    public var showLoadingIndicator: Boolean by activityIndicator::shown

    @Deprecated("no longer needed", ReplaceWith("this"))
    public inline val rView: Element get() = this
}