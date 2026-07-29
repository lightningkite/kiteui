package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.VideoSource
import com.lightningkite.kiteui.models.ThemeDerivation
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
import kotlin.coroutines.CoroutineContext


/**
 * Displays one or more video sources with optional controls, looping, and scale behavior.
 *
 * Set [source] (or [info] for multi-source fallback) to load a video. The element manages
 * an internal [RawVideoView] per source and fades between them on change.
 *
 * Key properties:
 * - [source] / [info]: what to play
 * - [scaleType]: how the video fills the available space (default: [ImageScaleType.Fit])
 * - [description]: accessibility label for the video content
 * - [showControls]: show/hide platform playback controls (default false)
 * - [loop]: whether playback loops automatically (default false)
 * - [time], [playing], [volume]: reactive state mirroring the active player
 * - [showLoadingIndicator]: whether the built-in spinner is visible while loading
 */
public class VideoView(private val frame: Frame) : Element by frame {
    public constructor(context: ElementContext) : this(Frame(context))

    public data class Info(
        val sources: List<VideoSource>,
        val scaleType: ImageScaleType,
        val description: String?
    )

    private val current = Signal<RawVideoView?>(null)

    init {
        val removeListener = current.addListener {
            current.value?.showControls = showControls
            current.value?.loop = loop
        }
        onRemove {
            removeListener()
        }
    }

    public val time: MutableReactive<Double> = current.lens { it?.time ?: Signal(0.0) }.flatten()
    public val playing: MutableReactive<Boolean> = current.lens { it?.playing ?: Signal(false) }.flatten()
    public val volume: MutableReactive<Float> = current.lens { it?.volume ?: Signal(0f) }.flatten()
    public var showControls: Boolean = false
        set(value) {
            field = value
            current.value?.showControls = value
        }
    public var loop: Boolean = false
        set(value) {
            field = value
            current.value?.loop = value
        }
    public var info: Info? = null
        set(value) {
            field = value
            if (ready) refresh()
        }
    public var source: VideoSource?
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

    private var ready = false

    @OverrideOnly
    override fun onStartup() {
        frame.onStartup()
        ready = true
        refresh()
    }

    private var lastRendered: Info? = null
    private var lastRender: List<RawVideoView>? = null

    public val activityIndicator: ActivityIndicator = frame.centered.activityIndicator { opacity = 0.0 }

    public val shownInfo = RawReactive<Info?>(ReactiveState(null))
    public var cannotBeCovered = false

    @OptIn(ExperimentalKiteUi::class)
    public fun refresh() {
        if (!ready) return
        val info = info
        if (lastRendered != info) {
            lastRender?.forEach {
                if (frame.areAnimationsEnabled) {
                    it.opacity = 0.0
                    afterTimeout(it.theme.transitionDuration.inWholeMilliseconds) {
                        frame.removeChild(it)
                    }
                } else {
                    frame.removeChild(it)
                }
            }
            shownInfo.state = ReactiveState.notReady
            lastRendered = info
            activityIndicator.opacity = 1.0
            lastRender = info?.let {
                val self = this@VideoView

                buildList {
                    with(frame) {
                        for (videoSource in it.sources) {
                            add(
                                themed(
                                ThemeDerivation { if (self.frame.themeAndBack.drawBackground) it.withBack else it.withoutBack }
                            ).rawVideo(videoSource, it.description ?: "", it.scaleType) {
                                themeBase = NativeElementCommonCode.GetBaseTheme.fromParentNonCascading
                                themeChoice
                                opacity = 0.0
                                reactive {
                                    this@rawVideo.state.state().handle(
                                        success = {
                                            opacity = 1.0
                                            if (self.lastRendered == info) {
                                                self.activityIndicator.opacity = 0.0
                                                self.shownInfo.state = ReactiveState(info)
                                            }
                                        },
                                        exception = {
                                            if (self.lastRendered == info) {
                                                self.activityIndicator.opacity = 0.0
                                                self.shownInfo.state = ReactiveState.exception(it)
                                                self.lastRendered = null
                                                if (self.info !== info) {
                                                    self.refresh()
                                                }
                                            }
                                        },
                                        notReady = {}
                                    )
                                }
                            }.also {
                                self.current.value = it
                            })
                        }
                    }
                }
            } ?: run {
                this@VideoView.shownInfo.state = ReactiveState(null)
                activityIndicator.opacity = 0.0
                null
            }
        }
    }

    public var showLoadingIndicator: Boolean by activityIndicator::shown

    @Deprecated("No longer needed", ReplaceWith("this"))
    public inline val rView: Element get() = this
}