package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.ImageSource
import com.lightningkite.kiteui.models.UrlCacheStrategy
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.areAnimationsEnabled
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.withoutAnimation
import com.lightningkite.readable.RawReadable
import com.lightningkite.readable.ReadableState
import com.lightningkite.readable.reactive
import kotlin.jvm.JvmInline
import kotlin.contracts.*
import kotlin.coroutines.CoroutineContext


class ImageView(viewWriter: ViewWriter) : ViewModifiable {
    override val rView: Frame = with(viewWriter) { frame { } }
    override val coroutineContext: CoroutineContext get() = rView.coroutineContext

    data class Info(
        val sources: List<ImageSource>,
        val scaleType: ImageScaleType,
        val description: String?
    )

    var info: Info? = null
        set(value) {
            field = value
            if (ready) refresh()
        }
    var source: ImageSource?
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
    var refreshOnParamChange: Boolean = false
    var naturalSize: Boolean = false

    var ready = false
    fun postSetup() {
        ready = true
        refresh()
    }

    private var lastRendered: Info? = null
    private var lastRender: List<RawImageView>? = null

    val activityIndicator: ActivityIndicator
    init {
        with(rView) {
            centered - activityIndicator {
                activityIndicator = this
                opacity = 0.0
            }
        }
    }

    val shown = RawReadable<Info?>(ReadableState(null))
    fun refresh() {
        if (!ready) return
        val info = info
        if (lastRendered != info) {
            lastRender?.forEach {
                if(rView.areAnimationsEnabled) {
                    it.opacity = 0.0
                    afterTimeout(it.theme.transitionDuration.inWholeMilliseconds) {
                        rView.removeChild(it)
                    }
                } else {
                    rView.removeChild(it)
                }
            }
            shown.state = ReadableState.notReady
            lastRendered = info
            activityIndicator.opacity = 1.0
            lastRender = info?.let {
                buildList {
                    with(rView) {
                        for (imageSource in it.sources) {
                            add(rawImage(imageSource, it.description ?: "", it.scaleType) {
                                opacity = 0.0
                                reactive {
                                    this@rawImage.state.state().handle(
                                        success = {
                                            opacity = 1.0
                                            if(lastRendered == info) {
                                                activityIndicator.opacity = 0.0
                                                this@ImageView.shown.state = ReadableState(info)
                                            }
                                          },
                                        exception = {
                                            if(lastRendered == info) {
                                                activityIndicator.opacity = 0.0
                                                this@ImageView.shown.state = ReadableState.exception(it)
                                                lastRendered = null
                                                if(this@ImageView.info !== info) {
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
            } ?: run{
                this@ImageView.shown.state = ReadableState(null)
                activityIndicator.opacity = 0.0
                null
            }
        }
    }
    var showLoadingIndicator: Boolean by activityIndicator::shown
}