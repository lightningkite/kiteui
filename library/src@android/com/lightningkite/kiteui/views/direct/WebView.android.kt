package com.lightningkite.kiteui.views.direct

import android.Manifest
import android.app.AlertDialog
import android.os.Message
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebViewClient
import android.widget.EditText
import com.lightningkite.kiteui.externalServices
import com.lightningkite.kiteui.utils.safeLinkUrlOrNull
import com.lightningkite.kiteui.views.*
import android.webkit.WebView as AndroidWebView

public actual class WebView actual constructor(context: ElementContext) : NativeElement(context) {
    override val native: AndroidWebView = AndroidWebView(context.activity)

    private var _source: WebViewSource? = null
    private var _permissions: Set<WebViewPermission> = emptySet()
    public actual val source: WebViewSource? get() = _source
    public actual val permissions: Set<WebViewPermission> get() = _permissions

    init {
        native.webChromeClient = ChromeClient()
        native.webViewClient = NavigationClient()
        // Local file access is off and is not offered as a permission: this is how content reads
        // arbitrary files off the device, and neither web nor iOS offers framed content the
        // equivalent. allowFileAccessFromFileURLs and allowUniversalAccessFromFileURLs are not set
        // here - both are deprecated and both already default to false, so writing them only
        // produced deprecation warnings while changing nothing.
        native.settings.allowFileAccess = false
        native.settings.allowContentAccess = false
        applyPermissions()
    }

    public actual fun load(source: WebViewSource, permissions: Set<WebViewPermission>) {
        _source = source
        _permissions = permissions
        applyPermissions()
        when (source) {
            is WebViewSource.Url -> native.loadUrl(source.url)
            // loadDataWithBaseURL with a null base, not loadData: loadData gives the content a
            // "data:" origin and mangles '#' and '%' in the markup, while a null base URL produces
            // the opaque origin WebViewSource.Html promises.
            is WebViewSource.Html -> native.loadDataWithBaseURL(null, source.html, "text/html", "utf-8", null)
        }
    }

    private fun applyPermissions() {
        val settings = native.settings
        settings.javaScriptEnabled = WebViewPermission.Scripts in permissions
        settings.javaScriptCanOpenWindowsAutomatically = WebViewPermission.Popups in permissions
        settings.setSupportMultipleWindows(WebViewPermission.Popups in permissions)
        settings.mediaPlaybackRequiresUserGesture = WebViewPermission.Autoplay !in permissions
        // DOM storage follows the origin the source gets, matching web: a real site keeps its
        // storage, inline HTML has an opaque origin and so has none.
        settings.domStorageEnabled = source is WebViewSource.Url

        // Setting a listener is what enables downloads at all - leaving it null makes the WebView
        // ignore them - so this is both the grant and the implementation. Handed to the system
        // browser, which has the progress and destination UI an embedded view does not.
        if (WebViewPermission.Downloads in permissions) {
            native.setDownloadListener { url, _, _, _, _ -> openExternally(url) }
        } else {
            native.setDownloadListener(null)
        }
    }

    /**
     * Keeps navigation inside this view.
     *
     * Without a WebViewClient installed at all, Android's default behaviour for a navigation it does
     * not handle is to fire it at the OS as an Intent - so an `intent:`, `market:` or `tel:` link
     * inside the content launches another app, with no permission involved and no equivalent on web
     * or iOS. It also means `target="_blank"` escapes to the system browser even when [WebViewPermission.Popups]
     * is denied, because `setSupportMultipleWindows(false)` routes such a link here rather than to
     * `onCreateWindow`.
     *
     * So http(s) loads in place and everything else is refused. Leaving the view is what
     * [WebViewPermission.Popups] is for, and that path validates the scheme before handing it on.
     */
    private inner class NavigationClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: AndroidWebView, request: WebResourceRequest): Boolean {
            val scheme = request.url.scheme?.lowercase()
            // "Handled" here means "refused": returning true tells the WebView to do nothing, which
            // is the only way to stop it without offering somewhere else for the navigation to go.
            return scheme != "http" && scheme != "https"
        }
    }

    /**
     * Hands a URL the displayed content chose to the OS, if its scheme is one we are willing to act on.
     *
     * The content picked this URL, so it is exactly the untrusted input [isSafeLinkUrl] exists for:
     * `openLink` resolves it through an Intent, where `intent:` reaches another installed app and
     * `file:` reaches local storage. Granting Popups or Downloads is permission to leave the view,
     * not permission to name any scheme on the device.
     */
    private fun openExternally(url: String) {
        safeLinkUrlOrNull(url)?.let { context.externalServices.openLink(it) }
    }

    /** The popup probe currently waiting for its URL, if any. See [ChromeClient.onCreateWindow]. */
    private var popupProbe: AndroidWebView? = null

    private fun discardPopupProbe() {
        popupProbe?.destroy()
        popupProbe = null
    }

    private inner class ChromeClient : WebChromeClient() {
        /**
         * Shows a dialog for `alert`/`confirm`, or dismisses the call when [WebViewPermission.Modals]
         * is not granted.
         *
         * Returning true means "handled, show nothing of your own" either way. A WebView with no
         * WebChromeClient ignores these silently, so denial needs no work - but granting does, since
         * there is no built-in dialog to fall back on.
         */
        private fun dialog(message: String, result: JsResult, cancellable: Boolean): Boolean {
            if (WebViewPermission.Modals !in permissions) {
                result.cancel()
                return true
            }
            AlertDialog.Builder(context.activity)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ -> result.confirm() }
                .apply { if (cancellable) setNegativeButton(android.R.string.cancel) { _, _ -> result.cancel() } }
                .setOnCancelListener { result.cancel() }
                .show()
            return true
        }

        override fun onJsAlert(view: AndroidWebView?, url: String?, message: String?, result: JsResult): Boolean =
            dialog(message.orEmpty(), result, cancellable = false)

        override fun onJsConfirm(view: AndroidWebView?, url: String?, message: String?, result: JsResult): Boolean =
            dialog(message.orEmpty(), result, cancellable = true)

        /**
         * The fourth dialog, and the one that arrives by default rather than on request.
         *
         * Installing any WebChromeClient makes the framework show its own "Leave this page?" prompt
         * for `onbeforeunload` unless this returns true - so unlike the other three, leaving it
         * unimplemented is what *creates* a modal rather than suppressing one.
         */
        override fun onJsBeforeUnload(view: AndroidWebView?, url: String?, message: String?, result: JsResult): Boolean =
            dialog(message.orEmpty(), result, cancellable = true)

        override fun onJsPrompt(
            view: AndroidWebView?,
            url: String?,
            message: String?,
            defaultValue: String?,
            result: JsPromptResult,
        ): Boolean {
            if (WebViewPermission.Modals !in permissions) {
                result.cancel()
                return true
            }
            val input = EditText(context.activity).apply { setText(defaultValue.orEmpty()) }
            AlertDialog.Builder(context.activity)
                .setMessage(message.orEmpty())
                .setView(input)
                .setPositiveButton(android.R.string.ok) { _, _ -> result.confirm(input.text.toString()) }
                .setNegativeButton(android.R.string.cancel) { _, _ -> result.cancel() }
                .setOnCancelListener { result.cancel() }
                .show()
            return true
        }

        /**
         * Answers the page's camera/microphone request, asking the OS for the app's own permission
         * first if it does not already hold it.
         *
         * Granting a page the camera can never mean more than the app itself has, so the page's
         * request is only ever a request to use a permission the user granted the app.
         */
        override fun onPermissionRequest(request: PermissionRequest) {
            val wanted = request.resources.filter {
                when (it) {
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE -> WebViewPermission.Camera in permissions
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE -> WebViewPermission.Microphone in permissions
                    // Protected media and MIDI SysEx are not offered as permissions, so they stay
                    // denied rather than being lumped in with camera or microphone.
                    else -> false
                }
            }
            if (wanted.isEmpty()) {
                request.deny()
                return
            }
            val osPermissions = buildList {
                if (PermissionRequest.RESOURCE_VIDEO_CAPTURE in wanted) add(Manifest.permission.CAMERA)
                if (PermissionRequest.RESOURCE_AUDIO_CAPTURE in wanted) add(Manifest.permission.RECORD_AUDIO)
            }
            AndroidAppContext.requestPermissions(permissions = osPermissions.toTypedArray()) { result ->
                if (result.accepted) request.grant(wanted.toTypedArray()) else request.deny()
            }
        }

        /**
         * Hands a popup to the system browser.
         *
         * Android reports the target URL only by loading it into a WebView the caller supplies, so a
         * throwaway one is handed over purely to learn the URL and then destroyed; there is no API
         * that simply reports it. Only reached when [WebViewPermission.Popups] is granted, because
         * `setSupportMultipleWindows(false)` otherwise makes such a link load in place instead.
         */
        override fun onCreateWindow(
            view: AndroidWebView,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message,
        ): Boolean {
            if (WebViewPermission.Popups !in permissions) return false
            // Only ever one outstanding probe: window.open() with no URL, or with about:blank or a
            // javascript: target, never navigates, so its probe would otherwise sit there forever and
            // content granted Popups could allocate them in a loop.
            discardPopupProbe()
            val probe = AndroidWebView(context.activity)
            popupProbe = probe
            probe.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(v: AndroidWebView, request: WebResourceRequest): Boolean {
                    openExternally(request.url.toString())
                    // Posted rather than called here: destroying a WebView from inside its own
                    // client callback re-enters a view that is still on the stack.
                    v.post { discardPopupProbe() }
                    return true
                }
            }
            (resultMsg.obj as AndroidWebView.WebViewTransport).webView = probe
            resultMsg.sendToTarget()
            return true
        }
    }
}
