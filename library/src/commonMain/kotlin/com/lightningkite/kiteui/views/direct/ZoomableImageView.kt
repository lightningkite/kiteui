package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.ImageSource
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.theme
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext


public class ZoomableImageView(viewWriter: ElementWriter) : CoroutineScope {
    public val rView: Frame = with(viewWriter) { frame { } }
    override val coroutineContext: CoroutineContext get() = rView.coroutineContext

    public data class Info(
        val sources: List<ImageSource>,
        val scaleType: ImageScaleType,
        val description: String?
    )

    public var info: Info? = null
        set(value) {
            field = value
            if (ready) afterTimeout(10) { refresh() }
        }
    public var source: ImageSource?
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
    public var refreshOnParamChange: Boolean = false
    public var naturalSize: Boolean = false

    public var ready = false
    public fun postSetup() {
        ready = true
        refresh()
    }

    private var lastRendered: Info? = null
    private var lastRender: List<RawImageViewZoomable>? = null

    public val activityIndicator: ActivityIndicator
    init {
        with(rView) {
            centered.activityIndicator {
                activityIndicator = this
                opacity = 0.0
            }
        }
    }

    public val shown = RawReactive<Info?>(ReactiveState(null))
    public fun refresh() {
        if (!ready) return
        val info = info
        if (lastRendered != info) {
            lastRender?.forEach {
                it.opacity = 0.0
                afterTimeout(it.theme.transitionDuration.inWholeMilliseconds) {
                    rView.removeChild(it)
                }
            }
            shown.state = ReactiveState.notReady
            lastRendered = info
            activityIndicator.opacity = 1.0
            lastRender = info?.let {
                buildList {
                    with(rView) {
                        for (imageSource in it.sources) {
                            add(rawImageZoomable(imageSource, it.description ?: "", it.scaleType) {
                                opacity = 0.0
                                reactive {
                                    this@rawImageZoomable.state.state().handle(
                                        success = {
                                            opacity = 1.0
                                            if(lastRendered == info) {
                                                activityIndicator.opacity = 0.0
                                                this@ZoomableImageView.shown.state = ReactiveState(info)
                                            }
                                          },
                                        exception = {
                                            if(lastRendered == info) {
                                                activityIndicator.opacity = 0.0
                                                this@ZoomableImageView.shown.state = ReactiveState.exception(it)
                                            }
                                        },
                                        notReady = {}
                                    )
                                }
                            })
                        }
                    }
                }
            } ?: run{
                this@ZoomableImageView.shown.state = ReactiveState(null)
                activityIndicator.opacity = 0.0
                null
            }
        }
    }
    public var showLoadingIndicator: Boolean by activityIndicator::shown
}