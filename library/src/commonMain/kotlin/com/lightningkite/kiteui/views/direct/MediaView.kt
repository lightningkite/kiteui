package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.ImageSource
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.models.VideoSource
import com.lightningkite.kiteui.models.VisualMediaSource
import com.lightningkite.kiteui.views.Element
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


class MediaView(viewWriter: ElementWriter) : CoroutineScope {
    val rView: Frame = with(viewWriter) { frame { } }
    override val coroutineContext: CoroutineContext get() = rView.coroutineContext

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
        onRemove {
            removeListener()
        }
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
    fun postSetup() {
        ready = true
        refresh()
    }

    private var lastRendered: Info? = null
    private var lastRender: List<Element>? = null

    val activityIndicator: ActivityIndicator

    init {
        with(rView) {
            centered.activityIndicator {
                cannotBeCovered = false
                activityIndicator = this
                opacity = 0.0
            }
        }
    }

    val shownInfo = RawReactive<Info?>(ReactiveState(null))
    val shown by rView::shown
    var cannotBeCovered = false

    @OptIn(ExperimentalKiteUi::class)
    fun refresh() {
        if (!ready) return
        val info = info
        if (lastRendered != info) {
            lastRender?.forEach {
                if (rView.areAnimationsEnabled) {
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
                        rView.removeChild(it)
                    }
                } else {
                    rView.removeChild(it)
                }
            }
            shownInfo.state = ReactiveState.notReady
            lastRendered = info
            activityIndicator.opacity = 1.0
            lastRender = info?.let {
                buildList {
                    with(rView) {
                        for (source in it.sources) {
                            when (source) {
                                is ImageSource -> {

                                    add(themed(
                                        ThemeDerivation { if (rView.themeAndBack.drawBackground) it.withBack else it.withoutBack }
                                    ).rawImage(source, it.description ?: "", it.scaleType) {
                                        themeBase = NativeElementCommonCode.GetBaseTheme.fromParentNonCascading
                                        themeChoice
                                        opacity = 0.0
                                        reactive {
                                            this@rawImage.state.state().handle(
                                                success = {
                                                    opacity = 1.0
                                                    if (lastRendered == info) {
                                                        activityIndicator.opacity = 0.0
                                                        this@MediaView.shownInfo.state = ReactiveState(info)
                                                        currentRawMediaView.value = this@rawImage
                                                    }
                                                },
                                                exception = {
                                                    if (lastRendered == info) {
                                                        activityIndicator.opacity = 0.0
                                                        this@MediaView.shownInfo.state = ReactiveState.exception(it)
                                                        lastRendered = null
                                                        if (this@MediaView.info !== info) {
                                                            refresh()
                                                        }
                                                    }
                                                },
                                                notReady = {}
                                            )
                                        }
                                    })
                                }

                                is VideoSource -> {
                                    add(themed(
                                        ThemeDerivation { if (rView.themeAndBack.drawBackground) it.withBack else it.withoutBack }
                                    ).rawVideo(source, it.description ?: "", it.scaleType) {
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
                                                        var start = Clock.System.now()
                                                        while (Clock.System.now() - start < transitionTime) {
                                                            delay(1.seconds / 30)
                                                            volume set ((Clock.System.now() - start) / transitionTime).toFloat()
                                                                .coerceIn(0f, 1f)
                                                        }
                                                        volume set 1f
                                                    }
                                                    opacity = 1.0
                                                    if (lastRendered == info) {
                                                        activityIndicator.opacity = 0.0
                                                        this@MediaView.shownInfo.state = ReactiveState(info)
                                                        currentRawMediaView.value = this@rawVideo
                                                    }
                                                },
                                                exception = {
                                                    if (lastRendered == info) {
                                                        activityIndicator.opacity = 0.0
                                                        this@MediaView.shownInfo.state = ReactiveState.exception(it)
                                                        lastRendered = null
                                                        if (this@MediaView.info !== info) {
                                                            refresh()
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
            } ?: run {
                this@MediaView.shownInfo.state = ReactiveState(null)
                activityIndicator.opacity = 0.0
                null
            }
        }
    }

    var showLoadingIndicator: Boolean by activityIndicator::shown
}