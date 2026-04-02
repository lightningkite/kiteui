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


class VideoView(private val frame: Frame) : Element by frame {
    constructor(context: ElementContext) : this(Frame(context))

    data class Info(
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

    val time: MutableReactive<Double> = current.lens { it?.time ?: Signal(0.0) }.flatten()
    val playing: MutableReactive<Boolean> = current.lens { it?.playing ?: Signal(false) }.flatten()
    val volume: MutableReactive<Float> = current.lens { it?.volume ?: Signal(0f) }.flatten()
    var showControls: Boolean = false
        set(value) {
            field = value
            current.value?.showControls = value
        }
    var loop: Boolean = false
        set(value) {
            field = value
            current.value?.loop = value
        }
    var info: Info? = null
        set(value) {
            field = value
            if (ready) refresh()
        }
    var source: VideoSource?
        get() = info?.sources?.firstOrNull()
        set(value) {
            info = info?.copy(sources = listOfNotNull(value)) ?: Info(listOfNotNull(value), ImageScaleType.Fit, null)
        }
    var scaleType: ImageScaleType
        get() = info?.scaleType ?: ImageScaleType.Fit
        set(value) {
            info = info?.copy(scaleType = value) ?: Info(listOf(), value, null)
        }
    var description: String?
        get() = info?.description
        set(value) {
            info = info?.copy(description = value) ?: Info(listOf(), ImageScaleType.Fit, value)
        }

    var ready = false
        private set

    @OverrideOnly
    override fun onStartup() {
        frame.onStartup()
        ready = true
        refresh()
    }

    private var lastRendered: Info? = null
    private var lastRender: List<RawVideoView>? = null

    val activityIndicator: ActivityIndicator = frame.centered.activityIndicator { opacity = 0.0 }

    val shownInfo = RawReactive<Info?>(ReactiveState(null))
    var cannotBeCovered = false

    @OptIn(ExperimentalKiteUi::class)
    fun refresh() {
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

    var showLoadingIndicator: Boolean by activityIndicator::shown

    @Deprecated("No longer needed", ReplaceWith("this"))
    inline val rView: Element get() = this
}