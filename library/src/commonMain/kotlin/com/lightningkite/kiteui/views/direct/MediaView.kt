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
import com.lightningkite.kiteui.views.NativeElementCommonCode
import com.lightningkite.kiteui.views.areAnimationsEnabled
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.theme
import com.lightningkite.kiteui.views.themed
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.flatten
import com.lightningkite.reactive.lensing.lens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        onRemove {
            removeListener()
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

    public var backgroundTheme: ThemeDerivation = ThemeDerivation { it.withoutBack }

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
                afterTimeout(it.theme.transitionDuration.inWholeMilliseconds) {
                    removeChildrenLater(listOf(it))
                }
            } else {
                removeChildrenLater(listOf(it))
            }
        }
    }

    /**
     * Fades the video's volume to zero, then pauses it. The video keeps playing during the fade
     * so the audio change is audible.
     */
    private fun fadeOutVideo(video: RawVideoView) {
        launch {
            val transitionTime = video.theme.transitionDuration * 3 / 4
            val start = Clock.System.now()
            val startVolume = video.volume()
            while (Clock.System.now() - start < transitionTime) {
                delay(1.seconds / 30)
                val t = ((Clock.System.now() - start) / transitionTime).toFloat().coerceIn(0f, 1f)
                video.volume set startVolume * (1f - t)
            }
            video.volume set 0f
            video.playing set false
        }
    }

    public val shownInfo: RawReactive<Info?> = RawReactive<Info?>(ReactiveState(null))
    public var cannotBeCovered: Boolean = false

    /**
     * Re-renders from [info]. Call this directly to retry after a failed load: a failure clears
     * the record of what was shown, but reassigning an equal [info] is a no-op.
     */
    @OptIn(ExperimentalKiteUi::class)
    public fun refresh() {
        if (!ready) return
        val info = info
        if (lastRendered != info) {
            val keyChanged = lastRendered != null && info != null && lastRendered?.key != info.key
            val replacingPlaceholder = lastRendered?.isPlaceholder == true
            val animateOut = keyChanged && !replacingPlaceholder && !opaqueTransitions

            lastRender?.let { renders ->
                renders.forEach { render ->
                    if (render is RawVideoView) {
                        if (animateOut) fadeOutVideo(render)
                        else launch { render.playing set false }
                    }
                    if (animateOut) render.opacity = 0.0
                }
                retiredRenders.addAll(renders)
            }
            lastRender = null
            shownInfo.state = ReactiveState.notReady
            lastRendered = info
            activityIndicator.opacity = 1.0

            if (info == null) {
                removeRetiredRenders()
                this@MediaView.shownInfo.state = ReactiveState(null)
                activityIndicator.opacity = 0.0
            } else {
                val self = this@MediaView
                // Published before the loop so a synchronous success can see the sources built so far.
                val renders = ArrayList<Element>()
                lastRender = renders

                for ((sourceIndex, source) in info.sources.withIndex()) {
                    when (source) {
                        is ImageSource -> {
                            renders.add(
                                frame.themed(
                                    backgroundTheme
                                ).rawImage(source, info.description ?: "", info.scaleType) {
                                    themeBase = NativeElementCommonCode.GetBaseTheme.fromParentNonCascading
                                    themeChoice

                                    if (!self.opaqueTransitions) {
                                        opacity = 0.0
                                    }

                                    reactive {
                                        this@rawImage.state.state().handle(
                                            success = {
                                                if (self.lastRendered == info) {
                                                    //  DO NOT animate fade-in if opaqueTransitions is true
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
                                                self.removeChildrenLater(listOf(this@rawImage))
                                                if (self.lastRendered == info) {
                                                    self.activityIndicator.opacity = 0.0
                                                    self.shownInfo.state = ReactiveState.exception(it)
                                                    self.lastRendered = null
                                                    // Load failed: don't leave the previous render(s) stranded in retiredRenders.
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
                            renders.add(
                                frame.themed(
                                    backgroundTheme
                                ).rawVideo(source, info.description ?: "", info.scaleType) {
                                    themeBase = NativeElementCommonCode.GetBaseTheme.fromParentNonCascading
                                    themeChoice

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
                                                        val transitionTime = theme.transitionDuration * 3 / 4
                                                        val start = Clock.System.now()
                                                        while (Clock.System.now() - start < transitionTime) {
                                                            delay(1.seconds / 30)
                                                            volume set ((Clock.System.now() - start) / transitionTime).toFloat()
                                                                .coerceIn(0f, 1f)
                                                        }
                                                        volume set 1f
                                                    }

                                                    //DO NOT animate fade-in if opaqueTransitions is true
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
                                                self.removeChildrenLater(listOf(this@rawVideo))
                                                if (self.lastRendered == info) {
                                                    self.activityIndicator.opacity = 0.0
                                                    self.shownInfo.state = ReactiveState.exception(it)
                                                    self.lastRendered = null

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
        }
    }

    public var showLoadingIndicator: Boolean by activityIndicator::shown

    @Deprecated("no longer needed", ReplaceWith("this"))
    public inline val rView: Element get() = this
}
