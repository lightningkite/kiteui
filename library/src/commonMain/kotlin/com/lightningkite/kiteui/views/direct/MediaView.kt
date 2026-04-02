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


class MediaView(private val frame: Frame) : Element by frame {
    constructor(context: ElementContext) : this(Frame(context))

    data class Info(
        val sources: List<VisualMediaSource>,
        val scaleType: ImageScaleType,
        val description: String?
    )

    val currentRawMediaView = Signal<Element?>(null)

    init {
        val removeListener = currentRawMediaView.addListener {
            (currentRawMediaView.value as? RawVideoView)?.showControls = showControls
            (currentRawMediaView.value as? RawVideoView)?.loop = loop
        }
        onRemove(removeListener)
    }

    var info: Info? = null
        set(value) {
            field = value
            if (ready) refresh()
        }
    var source: VisualMediaSource?
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

    var opaqueTransitions: Boolean = false

    var showControls: Boolean = false
        set(value) {
            field = value
            (currentRawMediaView.value as? RawVideoView)?.showControls = value
        }
    var loop: Boolean = false
        set(value) {
            field = value
            (currentRawMediaView.value as? RawVideoView)?.loop = value
        }

    val time: MutableReactive<Double> = currentRawMediaView.lens( get = { (it as? RawVideoView)?.time ?: Signal(0.0) } ).flatten()
    val playing: MutableReactive<Boolean> = currentRawMediaView.lens { (it as? RawVideoView)?.playing ?: Signal(false) }.flatten()
    val volume: MutableReactive<Float> = currentRawMediaView.lens { (it as? RawVideoView)?.volume ?: Signal(0f) }.flatten()


    var ready = false

    @OverrideOnly
    override fun onStartup() {
        frame.onStartup()
        ready = true
        refresh()
    }

    private var lastRendered: Info? = null
    private var lastRender: List<Element>? = null

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
                    if(!opaqueTransitions) it.opacity = 0.0
                    if(it is RawVideoView) {
                        launch {
                            val transitionTime = it.theme.transitionDuration * 3 / 4
                            var start = Clock.System.now()
                            while (Clock.System.now() - start < transitionTime) {
                                delay(1.seconds / 30)
                                it.volume set ((Clock.System.now() - start) / transitionTime).toFloat()
                                    .coerceIn(0f, 1f)
                                    .let { 1f - it }
                            }
                            it.volume set 0f
                        }
                    }
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
            lastRender = info?.let { info ->
                val self = this@MediaView

                buildList {
                    for (source in info.sources) {
                        when (source) {
                            is ImageSource -> {
                                add(frame.themed(
                                    ThemeDerivation { if (frame.themeAndBack.drawBackground) it.withBack else it.withoutBack }
                                ).rawImage(source, info.description ?: "", info.scaleType) {
                                    themeBase = NativeElementCommonCode.GetBaseTheme.fromParentNonCascading
                                    themeChoice
                                    opacity = 0.0
                                    reactive {
                                        this@rawImage.state.state().handle(
                                            success = {
                                                opacity = 1.0
                                                if (self.lastRendered == info) {
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
                                add(frame.themed(
                                    ThemeDerivation { if (frame.themeAndBack.drawBackground) it.withBack else it.withoutBack }
                                ).rawVideo(source, info.description ?: "", info.scaleType) {
                                    themeBase = NativeElementCommonCode.GetBaseTheme.fromParentNonCascading
                                    themeChoice
                                    opacity = 0.0
                                    launch { volume set 0f }
                                    this.showControls = this@MediaView.showControls
                                    this.loop = this@MediaView.loop
                                    reactive {
                                        this@rawVideo.state.state().handle(
                                            success = {
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
                                                opacity = 1.0
                                                if (self.lastRendered == info) {
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
                this@MediaView.shownInfo.state = ReactiveState(null)
                activityIndicator.opacity = 0.0
                null
            }
        }
    }

    var showLoadingIndicator: Boolean by activityIndicator::shown

    @Deprecated("no longer needed", ReplaceWith("this"))
    inline val rView: Element get() = this
}