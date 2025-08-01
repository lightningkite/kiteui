package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.ImageSource
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.models.ThemeDerivation.Companion.invoke
import com.lightningkite.kiteui.models.UrlCacheStrategy
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.areAnimationsEnabled
import com.lightningkite.kiteui.views.centered
import com.lightningkite.signal.RawReadable
import com.lightningkite.signal.ReadableState
import com.lightningkite.signal.reactive
import kotlin.contracts.*
import kotlin.coroutines.CoroutineContext


public class ImageView(viewWriter: ViewWriter) : ViewModifiable {
    public override val rView: Frame = with(viewWriter) { frame { } }
    public override val coroutineContext: CoroutineContext get() = rView.coroutineContext

    public data class Info(
        public val sources: List<ImageSource>,
        public val scaleType: ImageScaleType,
        public val description: String?
    )

    public var info: Info? = null
        set(value) {
            field = value
            if (ready) refresh()
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

    public var ready: Boolean = false
    public fun postSetup() {
        ready = true
        refresh()
    }

    private var lastRendered: Info? = null
    private var lastRender: List<RawImageView>? = null

    public val activityIndicator: ActivityIndicator
    init {
        with(rView) {
            centered - activityIndicator {
                activityIndicator = this
                opacity = 0.0
            }
        }
    }

    public val shownInfo: RawReadable<Info?> = RawReadable<Info?>(ReadableState(null))
    public val shown: Boolean by rView::shown

    public fun refresh() {
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
            shownInfo.state = ReadableState.notReady
            lastRendered = info
            activityIndicator.opacity = 1.0
            lastRender = info?.let {
                buildList {
                    with(rView) {
                        for (imageSource in it.sources) {
                            ThemeDerivation { if(rView.themeAndBack.drawBackground) it.withBack else it.withoutBack }.onNext
                            add(rawImage(imageSource, it.description ?: "", it.scaleType) {
                                themeTakeNonCascadingFromParent = true
                                themeChoice
                                opacity = 0.0
                                reactive {
                                    this@rawImage.state.state().handle(
                                        success = {
                                            opacity = 1.0
                                            if(lastRendered == info) {
                                                activityIndicator.opacity = 0.0
                                                this@ImageView.shownInfo.state = ReadableState(info)
                                            }
                                          },
                                        exception = {
                                            if(lastRendered == info) {
                                                activityIndicator.opacity = 0.0
                                                this@ImageView.shownInfo.state = ReadableState.exception(it)
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
                this@ImageView.shownInfo.state = ReadableState(null)
                activityIndicator.opacity = 0.0
                null
            }
        }
    }
    public var showLoadingIndicator: Boolean by activityIndicator::shown
}