package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement

public actual class WebView actual constructor(context: ElementContext) : NativeElement(context) {

    private var _source: WebViewSource? = null
    private var _permissions: Set<WebViewPermission> = emptySet()
    public actual val source: WebViewSource? get() = _source
    public actual val permissions: Set<WebViewPermission> get() = _permissions

    // Declared after the two fields above, because initializers run in declaration order and this
    // reads both. The policy is written even before anything is loaded, so a view that is never
    // given content still cannot do anything: no sandbox attribute grants an iframe everything, and
    // that is the state an unloaded view would otherwise sit in.
    init {
        native.tag = "iframe"
        apply(webViewAttributes(_source, _permissions))
    }

    public actual fun load(source: WebViewSource, permissions: Set<WebViewPermission>) {
        _source = source
        _permissions = permissions
        apply(webViewAttributes(source, permissions))
    }

    private fun apply(attributes: List<Pair<String, String?>>) {
        for ((name, value) in attributes) native.setAttribute(name, value)
    }
}

/**
 * The attributes a web view showing [source] under [permissions] must carry, **in the order they
 * have to be written**.
 *
 * The order is load-bearing and easy to lose. A browser reads `sandbox` and `allow` at the moment a
 * load begins, so writing `src` or `srcdoc` first starts that load under the *previous* policy. The
 * case that really bites is a [WebViewSource.Url] followed by a [WebViewSource.Html]: the inline
 * markup would load while `allow-same-origin` was still set, which is precisely what the rules for
 * Html exist to prevent. Returning an ordered list rather than writing attributes inline is what
 * makes that sequence something a test can assert instead of a comment someone has to honour.
 *
 * A null value means "remove this attribute".
 *
 * ### What goes in the sandbox
 *
 * `sandbox` is always present. Leaving it off is what grants a frame everything, top-level
 * navigation included, so content could replace the whole app with somewhere else - and no platform
 * offers that to framed content, so nothing here should either.
 *
 * `allow-same-origin` tracks the source rather than any permission. A [WebViewSource.Url] is a real
 * site and needs its own origin to work at all; [WebViewSource.Html] must not have one, because
 * `srcdoc` content inherits the *embedding page's* origin - so granting it alongside `allow-scripts`
 * would let inline HTML reach `parent.document` and rewrite this very attribute. Withholding it
 * gives that content the opaque origin Android and iOS give it for free.
 *
 * `allow-forms` is unconditional: Android cannot block form submission (`shouldOverrideUrlLoading`
 * is documented not to fire for POSTs), so blocking it here would make web the odd one out.
 *
 * Never granted on any path, because no platform offers framed content the equivalent: all four
 * `allow-top-navigation*` variants, `allow-popups-to-escape-sandbox` (so a permitted popup still
 * inherits this policy), `allow-pointer-lock`, `allow-orientation-lock`, `allow-presentation` and
 * `allow-storage-access-by-user-activation`.
 */
internal fun webViewAttributes(
    source: WebViewSource?,
    permissions: Set<WebViewPermission>,
): List<Pair<String, String?>> {
    val sandbox = buildList {
        add("allow-forms")
        if (source is WebViewSource.Url) add("allow-same-origin")
        if (WebViewPermission.Scripts in permissions) add("allow-scripts")
        if (WebViewPermission.Popups in permissions) add("allow-popups")
        if (WebViewPermission.Modals in permissions) add("allow-modals")
        if (WebViewPermission.Downloads in permissions) add("allow-downloads")
    }

    // Permissions Policy. Each feature is named explicitly rather than omitted when denied, because
    // the defaults differ per feature and per browser - autoplay in particular is allowed by
    // default, so denying it takes 'none' rather than silence. 'src' grants a feature to the frame's
    // own origin only.
    val allow = listOf(
        "camera" to (WebViewPermission.Camera in permissions),
        "microphone" to (WebViewPermission.Microphone in permissions),
        "autoplay" to (WebViewPermission.Autoplay in permissions),
    ).joinToString("; ") { (feature, granted) -> "$feature ${if (granted) "'src'" else "'none'"}" }

    return buildList {
        add("sandbox" to sandbox.joinToString(" "))
        add("allow" to allow)
        when (source) {
            // The attribute of the other kind is always cleared: `src` wins over `srcdoc` in some
            // browsers and loses in others, so a stale one is not merely untidy.
            is WebViewSource.Url -> {
                add("srcdoc" to null)
                add("src" to source.url)
            }
            // srcdoc rather than a data: URL, because a data: URL is a top-level navigation that
            // sandboxes treat inconsistently, while srcdoc is plainly framed content.
            is WebViewSource.Html -> {
                add("src" to null)
                add("srcdoc" to source.html)
            }
            null -> {}
        }
    }
}
