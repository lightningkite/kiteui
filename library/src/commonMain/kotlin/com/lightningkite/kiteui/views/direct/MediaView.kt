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
import com.lightningkite.kiteui.views.withoutAnimation
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
        val description: String?
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
            opaqueTransitions
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
    private var shownSource: VisualMediaSource? = null
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
     * Removes renders that a newly revealed render has replaced. Non-opaque swaps keep them
     * visible underneath until the new render has faded in, so the background never shows.
     */
    private fun removeSuperseded(elements: List<Element>) {
        if (elements.isEmpty()) return
        if (opaqueTransitions || !frame.areAnimationsEnabled) {
            removeChildrenLater(elements)
        } else {
            afterTimeout(theme.transitionDuration.inWholeMilliseconds) {
                removeChildrenLater(elements)
            }
        }
    }

    /**
     * Flushes anything sitting in retiredRenders. Safe to call multiple times
     * since it clears the list up front.
     */
    private fun removeRetiredRenders(
        transitionOut: Boolean = false,
        fadeVisual: Boolean = false,
        alreadyWithoutAnimation: Boolean = false,
    ) {
        val renders = retiredRenders.distinct()
        retiredRenders.clear()
        renders.forEach {
            if (transitionOut && !opaqueTransitions && frame.areAnimationsEnabled) {
                if (it is RawVideoView) fadeOutVideo(it)
                if (fadeVisual) it.opacity = 0.0
                removeSuperseded(listOf(it))
            } else {
                if (it is RawVideoView) launch { it.playing set false }
                if (alreadyWithoutAnimation) it.visible = false
                else it.withoutAnimation { it.visible = false }
                removeChildrenLater(listOf(it))
            }
        }
    }

    /**
     * Keep only the currently displayed element while another source set loads. Other candidates
     * from previous source sets can no longer become visible and should not accumulate in the
     * frame or in [retiredRenders].
     */
    private fun discardRetiredRendersExceptCurrent() {
        val current = currentRawMediaView.value
        val keepCurrent = current != null && retiredRenders.any { it === current }
        val obsolete = retiredRenders.filter { it !== current }
        retiredRenders.clear()
        // The current element may occur more than once if a previous render list was reused.
        if (keepCurrent) retiredRenders.add(current)
        obsolete.forEach {
            if (it is RawVideoView) launch { it.playing set false }
            it.withoutAnimation { it.visible = false }
        }
        removeChildrenLater(obsolete)
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
        val hasContentToClear = info == null &&
                (lastRender != null || retiredRenders.isNotEmpty() || currentRawMediaView.value != null)
        if (lastRendered != info || hasContentToClear) {
            lastRender?.let { renders ->
                retiredRenders.addAll(renders)
            }
            lastRender = null
            discardRetiredRendersExceptCurrent()
            shownInfo.state = ReactiveState.notReady
            lastRendered = info
            activityIndicator.opacity = 1.0

            if (info == null || info.sources.isEmpty()) {
                removeRetiredRenders(transitionOut = true, fadeVisual = true)
                this@MediaView.shownInfo.state = ReactiveState(null)
                shownSource = null
                currentRawMediaView.value = null
                activityIndicator.opacity = 0.0
            } else {
                val self = this@MediaView
                // Published before the loop so a synchronous success can see the sources built so far.
                val renders = ArrayList<Element>()
                lastRender = renders
                // Never downgrade: if the source already on screen is still in this info, only sources at
                // or above its index may replace it. Otherwise any source may be revealed first.
                val shownIndex = shownSource?.let { shown -> info.sources.indexOfFirst { it == shown } } ?: -1
                var revealedIndex = shownIndex - 1
                val firstEligibleSourceIndex = shownIndex.coerceAtLeast(0)
                val eligibleSourceCount = info.sources.size - firstEligibleSourceIndex
                val failedSources = HashSet<Int>()

                fun handleSourceFailure(sourceIndex: Int, element: Element, exception: Exception) {
                    // A displayed view may fail after it was revealed. Keep it in place rather than
                    // turning a recoverable media error into a blank frame.
                    if (self.currentRawMediaView.value !== element) {
                        if (element is RawVideoView) launch { element.playing set false }
                        self.removeChildrenLater(listOf(element))
                    }

                    // A failure of one candidate must not invalidate the remaining candidates.
                    // Only report the whole source set as failed after every eligible candidate fails.
                    if (self.lastRendered == info && sourceIndex >= firstEligibleSourceIndex) {
                        failedSources.add(sourceIndex)
                        if (failedSources.size >= eligibleSourceCount) {
                            self.activityIndicator.opacity = 0.0
                            self.shownInfo.state = ReactiveState.exception(exception)
                            self.lastRendered = null
                        }
                    }
                }

                for ((sourceIndex, source) in info.sources.withIndex()) {
                    when (source) {
                        is ImageSource -> {
                            renders.add(
                                frame.themed(
                                    backgroundTheme
                                ).rawImage(source, info.description ?: "", info.scaleType) {
                                    themeBase = NativeElementCommonCode.GetBaseTheme.fromParentNonCascading
                                    themeChoice

                                    if (self.opaqueTransitions) {
                                        withoutAnimation { visible = false }
                                    } else {
                                        withoutAnimation { opacity = 0.0 }
                                    }

                                    reactive {
                                        this@rawImage.state.state().handle(
                                            success = {
                                                if (self.lastRendered == info && sourceIndex > revealedIndex) {
                                                    revealedIndex = sourceIndex
                                                    if (self.opaqueTransitions) {
                                                        // Reveal the loaded replacement before hiding the old
                                                        // render. On iOS, hiding first can commit a frame with
                                                        // neither image visible, even though both changes are
                                                        // animation-free.
                                                        withoutAnimation {
                                                            visible = true
                                                            self.removeRetiredRenders(
                                                                transitionOut = true,
                                                                alreadyWithoutAnimation = true,
                                                            )
                                                        }
                                                    } else {
                                                        self.removeRetiredRenders(transitionOut = true)
                                                        opacity = 1.0
                                                    }

                                                    self.removeSuperseded(
                                                        self.lastRender?.take(sourceIndex) ?: emptyList()
                                                    )
                                                    self.activityIndicator.opacity = 0.0
                                                    self.shownInfo.state = ReactiveState(info)
                                                    self.shownSource = source
                                                    self.currentRawMediaView.value = this@rawImage
                                                } else if (self.currentRawMediaView.value !== this@rawImage) {
                                                    self.removeChildrenLater(listOf(this@rawImage))
                                                }
                                            },
                                            exception = {
                                                handleSourceFailure(sourceIndex, this@rawImage, it)
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

                                    if (self.opaqueTransitions) {
                                        withoutAnimation { visible = false }
                                    } else {
                                        withoutAnimation { opacity = 0.0 }
                                    }

                                    launch { volume set 0f }
                                    this.showControls = this@MediaView.showControls
                                    this.loop = this@MediaView.loop
                                    reactive {
                                        this@rawVideo.state.state().handle(
                                            success = {
                                                if (self.lastRendered == info && sourceIndex > revealedIndex) {
                                                    revealedIndex = sourceIndex
                                                    if (self.opaqueTransitions) {
                                                        // Keep this swap atomic for UIKit, where separate
                                                        // alpha commits can expose the background for a frame.
                                                        withoutAnimation {
                                                            visible = true
                                                            self.removeRetiredRenders(
                                                                transitionOut = true,
                                                                alreadyWithoutAnimation = true,
                                                            )
                                                        }
                                                    } else {
                                                        self.removeRetiredRenders(transitionOut = true)
                                                        opacity = 1.0
                                                    }

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

                                                    self.removeSuperseded(
                                                        self.lastRender?.take(sourceIndex) ?: emptyList()
                                                    )
                                                    self.activityIndicator.opacity = 0.0
                                                    self.shownInfo.state = ReactiveState(info)
                                                    self.shownSource = source
                                                    self.currentRawMediaView.value = this@rawVideo
                                                } else if (self.currentRawMediaView.value !== this@rawVideo) {
                                                    self.removeChildrenLater(listOf(this@rawVideo))
                                                }
                                            },
                                            exception = {
                                                handleSourceFailure(sourceIndex, this@rawVideo, it)
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
