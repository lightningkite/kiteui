package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.ImageRemote
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.ImageSource
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElementCommonCode
import com.lightningkite.kiteui.views.areAnimationsEnabled
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.theme
import com.lightningkite.kiteui.views.themed
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.addAndRunStateListener

public class ImageView(private val frame: Frame) : Element by frame {
    public constructor(context: ElementContext) : this(Frame(context))

    public data class Info(
        val sources: List<ImageSource>,
        val scaleType: ImageScaleType,
        val description: String?
    )

    public var info: Info? = null
        set(value) {
            field = value
            refresh()
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

    private val _shownInfo = RawReactive<Info?>(ReactiveState(null))
    public val shownInfo: Reactive<Info?> get() = _shownInfo

    private val spinner = frame.centered.activityIndicator {
        this@ImageView.shownInfo
            .addAndRunStateListener { s ->
                opacity = if (s.success) 0.0 else 1.0
            }
            .also(::onRemove)
    }

    public var showLoadingIndicator: Boolean by spinner::shown

    private var ready = false

    @OverrideOnly
    override fun onStartup() {
        frame.onStartup()
        ready = true
        refresh()
    }

    private var lastRendered: Info? = null
    private var lastRender: List<RawImageView>? = null

    @OptIn(ExperimentalKiteUi::class)
    public fun refresh() {
        if (!ready) return

        val info = info

        if (lastRendered == info) return

        lastRender?.forEach {
            if (areAnimationsEnabled) {
                it.opacity = 0.0
                afterTimeout(it.theme.transitionDuration.inWholeMilliseconds) {
                    frame.removeChild(it)
                }
            } else frame.removeChild(it)
        }

        lastRendered = info

        if (info == null) {
            _shownInfo.state = ReactiveState(null)
            spinner.opacity = 0.0
            lastRender = null
            return
        }

        _shownInfo.state = ReactiveState.notReady

        val writer = frame.themed(
            ThemeDerivation { if (frame.themeAndBack.drawBackground) it.withBack else it.withoutBack }
        )
        val view = this

        lastRender = info.sources.map {
            writer.rawImage(it, info.description ?: "", info.scaleType) {
                themeBase = NativeElementCommonCode.GetBaseTheme.fromParentNonCascading
                opacity = 0.0
                reactive {
                    this@rawImage.state.state().handle(
                        success = {
                            opacity = 1.0
                            if (view.lastRendered == info) {
                                view._shownInfo.state = ReactiveState(info)
                            }
                        },
                        exception = {
                            if (view.lastRendered == info) {
                                val latestInfo = view.info
                                // If a fresher URL is available for any source (same path, rotated
                                // signature), retry silently rather than surfacing the error.
                                val hasFresherUrl = latestInfo != null &&
                                    latestInfo.sources.size == info.sources.size &&
                                    latestInfo.sources.zip(info.sources).any { (latest, current) ->
                                        latest is ImageRemote && current is ImageRemote &&
                                            latest.url != current.url
                                    }
                                if (hasFresherUrl) {
                                    view.lastRendered = null
                                    view.refresh()
                                } else {
                                    view._shownInfo.state = ReactiveState.exception(it)
                                    view.lastRendered = null
                                    if (view.info !== info) view.refresh()
                                }
                            }
                        },
                        notReady = {}
                    )
                }
            }
        }
    }

    @Deprecated("No longer needed", ReplaceWith("this")) public val rView: Element get() = this
}