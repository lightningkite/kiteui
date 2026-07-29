package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.ImageRemote
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.ImageSource
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.models.UrlCacheStrategy
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

    /** The views currently showing [lastRendered].  Internal so tests can see what got rendered. */
    internal var lastRender: List<RawImageView>? = null
        private set

    @OptIn(ExperimentalKiteUi::class)
    public fun refresh() {
        if (!ready) return

        val info = info

        if (showsSameAs(lastRendered, info)) return

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
                            if (view.lastRendered === info) {
                                view._shownInfo.state = ReactiveState(info)
                            }
                        },
                        exception = {
                            if (view.lastRendered === info) {
                                val latestInfo = view.info
                                if (latestInfo != null && latestInfo != info) {
                                    // A newer source arrived that we skipped rendering because it
                                    // showed the same image, but what's on screen failed to load -
                                    // an expired signature, most likely.  Render the newer one.
                                    view.lastRendered = null
                                    view.refresh()
                                } else {
                                    view._shownInfo.state = ReactiveState.exception(it)
                                    // Nothing worth keeping is on screen, so let the next source
                                    // assigned render even if it would otherwise show the same.
                                    view.lastRendered = null
                                }
                            }
                        },
                        notReady = {}
                    )
                }
            }
        }
    }

    @Deprecated("No longer needed", ReplaceWith("this")) val rView: Element get() = this

    public companion object {
        /**
         * Whether the views rendered for [rendered] already show [incoming], making a re-render
         * (and thus a re-download and a fade) pointless.  See [UrlCacheStrategy].
         */
        private fun showsSameAs(rendered: Info?, incoming: Info?): Boolean {
            if (rendered == null || incoming == null) return rendered == incoming
            return rendered.scaleType == incoming.scaleType &&
                    rendered.description == incoming.description &&
                    rendered.sources.size == incoming.sources.size &&
                    rendered.sources.indices.all { showsSameAs(rendered.sources[it], incoming.sources[it]) }
        }

        private fun showsSameAs(rendered: ImageSource, incoming: ImageSource): Boolean {
            if (rendered !is ImageRemote || incoming !is ImageRemote) return rendered == incoming
            if (rendered.cacheStrategy != incoming.cacheStrategy) return false
            return when (rendered.cacheStrategy) {
                UrlCacheStrategy.None -> false
                UrlCacheStrategy.Full -> rendered.url == incoming.url
                UrlCacheStrategy.PathOnly -> rendered.url.substringBefore('?') == incoming.url.substringBefore('?')
            }
        }
    }
}