package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.externalServices
import com.lightningkite.kiteui.utils.safeLinkUrlOrNull
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleCancel
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UITextField
import platform.WebKit.*
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
public actual class WebView actual constructor(context: ElementContext) : NativeElement(context) {
    /**
     * A container, not the `WKWebView` itself.
     *
     * A web view's capabilities come from the `WKWebViewConfiguration` it was built with, and
     * `WKWebView.configuration` hands back a *copy* - mutating it afterwards changes nothing. So each
     * [load] builds a fresh web view configured for the permissions that call was given and swaps it
     * in here, which is also what makes the per-load contract literally true on iOS: a page can never
     * end up running under permissions from some later call.
     *
     * [FrameLayout] rather than a bare UIView so the container participates in this codebase's sizing
     * contract like every other container does. Note that it does not give the web view an intrinsic
     * size: WKWebView's own `sizeThatFits` reports its current bounds, which start at zero, so a
     * `webView { }` with no `sizeConstraints` or `expanding` measures 0x0 - the same as before this
     * change, and the same as it would with any container.
     */
    override val native: FrameLayout = FrameLayout()

    private var _source: WebViewSource? = null
    private var _permissions: Set<WebViewPermission> = emptySet()
    public actual val source: WebViewSource? get() = _source
    public actual val permissions: Set<WebViewPermission> get() = _permissions

    // Both held strongly: WKWebView keeps only weak references to its delegates, and the web view
    // itself has to be reachable to be swapped out on the next load.
    private var delegate: Delegate? = null
    private var webView: WKWebView? = null

    public actual fun load(source: WebViewSource, permissions: Set<WebViewPermission>) {
        _source = source
        _permissions = permissions

        val configuration = WKWebViewConfiguration().apply {
            defaultWebpagePreferences = WKWebpagePreferences().apply {
                allowsContentJavaScript = WebViewPermission.Scripts in permissions
            }
            preferences = WKPreferences().apply {
                javaScriptCanOpenWindowsAutomatically = WebViewPermission.Popups in permissions
            }
            allowsInlineMediaPlayback = true
            mediaTypesRequiringUserActionForPlayback =
                if (WebViewPermission.Autoplay in permissions) WKAudiovisualMediaTypeNone
                else WKAudiovisualMediaTypeAll
            // Inline HTML gets an ephemeral store, so the opaque origin WebViewSource.Html promises
            // has nowhere to persist anything even if it finds a way to ask.
            if (source is WebViewSource.Html) websiteDataStore = WKWebsiteDataStore.nonPersistentDataStore()
        }

        val web = WKWebView(frame = native.bounds, configuration = configuration)
        val delegate = Delegate()
        web.UIDelegate = delegate
        web.navigationDelegate = delegate

        // Torn down before the new delegate replaces the old one: an in-flight load or playing media
        // on the outgoing page would otherwise run on until it happened to be collected, and its
        // delegate would be swapped out from under any dialog still waiting on the user.
        webView?.let {
            it.stopLoading()
            it.UIDelegate = null
            it.navigationDelegate = null
            it.removeFromSuperview()
        }
        this.delegate = delegate
        webView = web
        native.addSubview(web)

        when (source) {
            is WebViewSource.Url -> NSURL.URLWithString(source.url)?.let {
                web.loadRequest(NSURLRequest.requestWithURL(it))
            }
            // A null baseURL is what gives this content its opaque origin.
            is WebViewSource.Html -> web.loadHTMLString(source.html, baseURL = null)
        }
    }

    /**
     * Hands a URL the displayed content chose to the OS, if its scheme is one we are willing to act on.
     *
     * The content picked this URL, so it is exactly the untrusted input [isSafeLinkUrl] exists for:
     * `openURL` will launch whichever installed app registered the scheme. Granting Popups or
     * Downloads is permission to leave the view, not permission to name any scheme on the device.
     */
    private fun openExternally(url: String) {
        safeLinkUrlOrNull(url)?.let { context.externalServices.openLink(it) }
    }

    private inner class Delegate : NSObject(), WKUIDelegateProtocol, WKNavigationDelegateProtocol {

        /**
         * Keeps navigation inside this view, matching the Android navigation client.
         *
         * WebKit does not open custom schemes on its own, but being explicit means the rule is stated
         * in one place per platform rather than resting on that. `about:` is allowed because
         * `loadHTMLString` navigates to `about:blank`.
         */
        override fun webView(
            webView: WKWebView,
            decidePolicyForNavigationAction: WKNavigationAction,
            decisionHandler: (WKNavigationActionPolicy) -> Unit,
        ) {
            val scheme = decidePolicyForNavigationAction.request.URL?.scheme?.lowercase()
            decisionHandler(
                if (scheme == "http" || scheme == "https" || scheme == "about") WKNavigationActionPolicy.WKNavigationActionPolicyAllow
                else WKNavigationActionPolicy.WKNavigationActionPolicyCancel
            )
        }

        // Goes through ElementContext.present, which walks the context chain for a controller that
        // is actually on screen rather than assuming this element's own is.
        private fun present(alert: UIAlertController) = context.present(alert)

        /**
         * A `WKWebView` with no UI delegate ignores `alert`/`confirm`/`prompt` entirely, so denial is
         * simply invoking the completion handler as though the dialog had been dismissed. Granting
         * them means building the dialog, since there is nothing built in to fall back on.
         */
        override fun webView(
            webView: WKWebView,
            runJavaScriptAlertPanelWithMessage: String,
            initiatedByFrame: WKFrameInfo,
            completionHandler: () -> Unit,
        ) {
            if (WebViewPermission.Modals !in permissions) return completionHandler()
            val alert = UIAlertController.alertControllerWithTitle(null, runJavaScriptAlertPanelWithMessage, UIAlertControllerStyleAlert)
            alert.addAction(UIAlertAction.actionWithTitle("OK", UIAlertActionStyleDefault) { completionHandler() })
            present(alert)
        }

        override fun webView(
            webView: WKWebView,
            runJavaScriptConfirmPanelWithMessage: String,
            initiatedByFrame: WKFrameInfo,
            completionHandler: (Boolean) -> Unit,
        ) {
            if (WebViewPermission.Modals !in permissions) return completionHandler(false)
            val alert = UIAlertController.alertControllerWithTitle(null, runJavaScriptConfirmPanelWithMessage, UIAlertControllerStyleAlert)
            alert.addAction(UIAlertAction.actionWithTitle("Cancel", UIAlertActionStyleCancel) { completionHandler(false) })
            alert.addAction(UIAlertAction.actionWithTitle("OK", UIAlertActionStyleDefault) { completionHandler(true) })
            present(alert)
        }

        override fun webView(
            webView: WKWebView,
            runJavaScriptTextInputPanelWithPrompt: String,
            defaultText: String?,
            initiatedByFrame: WKFrameInfo,
            completionHandler: (String?) -> Unit,
        ) {
            if (WebViewPermission.Modals !in permissions) return completionHandler(null)
            val alert = UIAlertController.alertControllerWithTitle(null, runJavaScriptTextInputPanelWithPrompt, UIAlertControllerStyleAlert)
            var field: UITextField? = null
            alert.addTextFieldWithConfigurationHandler { field = it.also { f -> f?.text = defaultText } }
            alert.addAction(UIAlertAction.actionWithTitle("Cancel", UIAlertActionStyleCancel) { completionHandler(null) })
            alert.addAction(UIAlertAction.actionWithTitle("OK", UIAlertActionStyleDefault) { completionHandler(field?.text) })
            present(alert)
        }

        /**
         * Hands a popup to the system browser and returns null, which tells WebKit not to open a
         * second web view. An embedded view has no window furniture to make a real popup usable.
         */
        override fun webView(
            webView: WKWebView,
            createWebViewWithConfiguration: WKWebViewConfiguration,
            forNavigationAction: WKNavigationAction,
            windowFeatures: WKWindowFeatures,
        ): WKWebView? {
            if (WebViewPermission.Popups in permissions) {
                forNavigationAction.request.URL?.absoluteString?.let { openExternally(it) }
            }
            return null
        }

        /**
         * Answers the page's camera/microphone request, asking the OS for the app's own permission
         * first when it does not already hold it - a page can never be granted more than the app has.
         */
        override fun webView(
            webView: WKWebView,
            requestMediaCapturePermissionForOrigin: WKSecurityOrigin,
            initiatedByFrame: WKFrameInfo,
            type: WKMediaCaptureType,
            decisionHandler: (WKPermissionDecision) -> Unit,
        ) {
            val mediaTypes = buildList {
                if (type == WKMediaCaptureType.WKMediaCaptureTypeCamera || type == WKMediaCaptureType.WKMediaCaptureTypeCameraAndMicrophone) {
                    if (WebViewPermission.Camera !in permissions) return decisionHandler(WKPermissionDecision.WKPermissionDecisionDeny)
                    add(AVMediaTypeVideo)
                }
                if (type == WKMediaCaptureType.WKMediaCaptureTypeMicrophone || type == WKMediaCaptureType.WKMediaCaptureTypeCameraAndMicrophone) {
                    if (WebViewPermission.Microphone !in permissions) return decisionHandler(WKPermissionDecision.WKPermissionDecisionDeny)
                    add(AVMediaTypeAudio)
                }
            }
            requestEach(mediaTypes) { granted ->
                decisionHandler(if (granted) WKPermissionDecision.WKPermissionDecisionGrant else WKPermissionDecision.WKPermissionDecisionDeny)
            }
        }

        /** Asks for each capture permission in turn, stopping at the first refusal. */
        private fun requestEach(mediaTypes: List<String?>, onResult: (Boolean) -> Unit) {
            // The AVMediaType constants come through the bindings as nullable. They are never
            // actually null, but if one were, denying is the only safe reading of it.
            if (mediaTypes.any { it == null }) return onResult(false)
            val remaining = mediaTypes.filterNotNull().toMutableList()
            fun next() {
                val mediaType = remaining.removeFirstOrNull() ?: return onResult(true)
                if (AVCaptureDevice.authorizationStatusForMediaType(mediaType) == AVAuthorizationStatusAuthorized) next()
                else AVCaptureDevice.requestAccessForMediaType(mediaType) { granted ->
                    if (granted) next() else onResult(false)
                }
            }
            next()
        }

        /**
         * Sends a download to the system browser, matching Android.
         *
         * WebKit reports a response it cannot display as one this delegate must decide on; allowing
         * it without being able to render it would leave a blank view, so it is cancelled either way
         * and only handed onward when downloads are permitted. Going through the browser rather than
         * WKDownload also keeps this working on iOS 14, below WKDownload's 14.5 requirement.
         */
        override fun webView(
            webView: WKWebView,
            decidePolicyForNavigationResponse: WKNavigationResponse,
            decisionHandler: (WKNavigationResponsePolicy) -> Unit,
        ) {
            if (decidePolicyForNavigationResponse.canShowMIMEType) {
                decisionHandler(WKNavigationResponsePolicy.WKNavigationResponsePolicyAllow)
                return
            }
            if (WebViewPermission.Downloads in permissions) {
                decidePolicyForNavigationResponse.response.URL?.absoluteString?.let { openExternally(it) }
            }
            decisionHandler(WKNavigationResponsePolicy.WKNavigationResponsePolicyCancel)
        }
    }
}
