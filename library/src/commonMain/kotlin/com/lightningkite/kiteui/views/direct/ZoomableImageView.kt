package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.ImageSource
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.onRemove
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.RawReactive
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.ReactiveState
import com.lightningkite.reactive.extensions.addAndRunStateListener

public class ZoomableImageView(private val frame: Frame) : Element by frame {
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

    public val shownInfo: Reactive<Info?>
        field = RawReactive<Info?>(ReactiveState(null))

    private val spinner = frame.centered.activityIndicator {
        this@ZoomableImageView.shownInfo
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
    private var lastRender: List<RawImageViewZoomable>? = null

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

        if (info == null || info.sources.isEmpty()) {
            shownInfo.state = ReactiveState(null)
            spinner.opacity = 0.0
            lastRender = null
            return
        }

        shownInfo.state = ReactiveState.notReady

        val writer = frame.themed(
            ThemeDerivation { if (frame.themeAndBack.drawBackground) it.withBack else it.withoutBack }
        )
        val view = this

        lastRender = info.sources.map { image ->
            writer.rawImageZoomable(image, info.description ?: "", info.scaleType) {
                themeBase = NativeElementCommonCode.GetBaseTheme.fromParentNonCascading
                opacity = 0.0

                this@rawImageZoomable.state.addAndRunStateListener { state ->
                    state.handle(
                        success = {
                            opacity = 1.0
                            if (view.lastRendered === info) {
                                view.shownInfo.state = ReactiveState(info)
                            }
                        },
                        exception = {
                            if (view.lastRendered === info) {
                                view.shownInfo.state = ReactiveState.exception(it)
                                // Claude: Nothing worth keeping is on screen, so let the same info retry.
                                view.lastRendered = null
                            }
                        },
                        notReady = {}
                    )
                }.also(::onRemove)
            }
        }
    }

    @Deprecated("No longer needed", ReplaceWith("this"))
    public val rView: Element get() = this
}
