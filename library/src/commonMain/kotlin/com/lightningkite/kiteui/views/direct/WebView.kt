package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement

/**
 * What a [WebView] shows.
 *
 * A web view displays one thing at a time, from one of two places. Modelling that as one value
 * rather than as separate `url` and `content` properties is what makes it answerable: with two,
 * setting either silently invalidated the other, and every platform disagreed about what the stale
 * one should then report.
 */
public sealed interface WebViewSource {
    /**
     * Fetch and display [url].
     *
     * The page gets its real origin, so its own cookies and storage work as they would in a browser.
     *
     * Which means: **a URL on your own origin is not sandboxed from you.** On web the frame is given
     * `allow-same-origin`, so a same-origin page reached this way can read `parent.document`, your
     * cookies and your local storage - correct for framing a third-party site, a trap for a relative
     * URL pointing at content someone else supplied. Use [Html] for anything untrusted; it is given
     * an opaque origin precisely so it cannot do that.
     */
    public data class Url(public val url: String) : WebViewSource

    /**
     * Display [html] directly, with no network fetch.
     *
     * The content gets an *opaque* origin on every platform, so it can reach no cookies or storage -
     * not the app's, not any site's. That matters because inline HTML is usually where markup from
     * somewhere untrusted ends up, and it is what stops such content reaching back into the page
     * embedding it on web.
     */
    public data class Html(public val html: String) : WebViewSource
}

/**
 * Something displayed content may do beyond rendering.
 *
 * Everything here is denied unless [WebView.load] is passed it. A set rather than a set of flags on
 * purpose: absent means denied, so adding a capability to this enum later cannot quietly loosen an
 * existing call site.
 *
 * Every entry is enforceable on all four platforms in both directions. Capabilities that only some
 * platforms can honour are deliberately absent rather than present-and-ignored - form submission,
 * for instance, which Android's `shouldOverrideUrlLoading` is documented not to see for POSTs, is
 * always permitted because two of three platforms cannot block it.
 */
public enum class WebViewPermission {
    /** Run scripts. Without this, the content is inert markup. */
    Scripts,

    /**
     * Open a new window (`window.open`, `target="_blank"`).
     *
     * Granted, the request is handed to the system browser rather than opening a second web view -
     * an embedded view has no window furniture to make a real popup usable.
     */
    Popups,

    /** Show `alert`, `confirm` and `prompt` dialogs. Denied, those calls return as if dismissed. */
    Modals,

    /** Start audio or video without the user first interacting with the content. */
    Autoplay,

    /** Use the camera, if the app itself holds the OS camera permission. */
    Camera,

    /** Use the microphone, if the app itself holds the OS microphone permission. */
    Microphone,

    /**
     * Download files.
     *
     * Granted, the download is handed to the system browser, which already has the UI for progress,
     * destination and completion that an embedded view does not.
     */
    Downloads;

    public companion object {
        /**
         * What a first-party page you control would expect: scripts, dialogs, popups and media.
         *
         * Camera and microphone are left out even here - those should be asked for explicitly, since
         * granting them means the app's own OS permission gets used on the content's behalf.
         */
        public val trusted: Set<WebViewPermission> = setOf(Scripts, Popups, Modals, Autoplay, Downloads)
    }
}

/**
 * An embedded browser.
 *
 * Content and the permissions it runs under are supplied together, through [load], because that is
 * the only point at which they can both be applied: web and iOS fix a page's capabilities when its
 * load begins, so a separately-mutable "allow scripts" property would appear to work while leaving
 * the page in front of the user running under whatever it was loaded with.
 *
 * These are the only members. Everything else a real browser offers - navigation callbacks, history,
 * JS bridges, cookie control - is supportable on some of the four platforms and not others, and a
 * member that silently does nothing on half of them is worse than one that is not there.
 */
public expect class WebView(context: ElementContext) : NativeElement {
    /**
     * Displays [source], allowing it exactly [permissions] and nothing else.
     *
     * Calling this again replaces what is displayed and the permissions along with it; permissions
     * never accumulate across loads.
     */
    public fun load(source: WebViewSource, permissions: Set<WebViewPermission> = emptySet())

    /** What [load] was last given, or null if it has not been called. */
    public val source: WebViewSource?

    /** The permissions [load] was last given. Empty until it is called. */
    public val permissions: Set<WebViewPermission>
}
