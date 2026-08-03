package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.*

/**
 * Manual verification for [WebView] permissions.
 *
 * Only the web implementation is testable in CI, because its whole capability model lands in two
 * iframe attributes. On Android and iOS the same capabilities are delegate callbacks - a dialog that
 * appears, a permission prompt, a browser that opens - and none of that is observable without a
 * device in hand. Every section below drives one capability from a page that exercises it, so the
 * check is "did the thing happen", not "is the flag set".
 *
 * Each row is loaded twice: once denied, once granted. A capability that behaves the same either way
 * is a capability that is not actually being enforced.
 */
@Routable("webview-permissions")
object WebViewPermissionPage : Page {
    override val title: Reactive<String> get() = Constant("WebView Permissions")

    /** Pages that make each capability observable, so the verdict is what you see rather than a flag. */
    private val probes: List<Triple<String, WebViewPermission, String>> = listOf(
        Triple(
            "Scripts",
            WebViewPermission.Scripts,
            """<body style="font:16px sans-serif;padding:1rem">
                 <p id="out">DENIED - scripts did not run</p>
                 <script>document.getElementById('out').textContent = 'GRANTED - scripts ran'</script>
               </body>""",
        ),
        Triple(
            "Modals",
            WebViewPermission.Modals,
            """<body style="font:16px sans-serif;padding:1rem">
                 <button onclick="alert('This dialog is what Modals grants')">alert()</button>
                 <button onclick="window.onbeforeunload = function(){ return 'x' };
                                  location.href = 'https://example.com'">onbeforeunload, then navigate</button>
                 <p>Granted: a native dialog for each. Denied: neither appears and the page keeps
                 running. The second one is the one that arrives <b>by default</b> on Android once a
                 WebChromeClient exists, so a "Leave this page?" prompt with Modals denied is a leak.</p>
               </body>""",
        ),
        Triple(
            "Popups",
            WebViewPermission.Popups,
            """<body style="font:16px sans-serif;padding:1rem">
                 <button onclick="window.open('https://example.com')">window.open()</button>
                 <p><a href="https://example.com" target="_blank">target=_blank link</a></p>
                 <p>Granted: both reach the system browser. Denied: window.open does nothing and the
                 link loads <b>in place</b> - if it opens the browser with Popups denied, the Android
                 navigation client is missing.</p>
               </body>""",
        ),
        Triple(
            "Autoplay",
            WebViewPermission.Autoplay,
            """<body style="font:16px sans-serif;padding:1rem">
                 <p>Granted: the clip below starts on its own. Denied: it waits for a tap.</p>
                 <video autoplay muted loop playsinline width="240"
                        src="https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"></video>
                 <p><small>Needs network. A clip that fails to load looks the same in both columns.</small></p>
               </body>""",
        ),
        Triple(
            "Downloads",
            WebViewPermission.Downloads,
            """<body style="font:16px sans-serif;padding:1rem">
                 <a href="https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf" download>Download a PDF</a>
                 <p><b>Android only.</b> Granted: the system browser takes over. Denied: nothing happens.
                 On iOS WebKit renders a PDF itself, so the download path is never entered; on web a
                 cross-origin <code>download</code> just navigates.</p>
               </body>""",
        ),
    )

    /**
     * Camera and microphone, which cannot be driven from inline HTML.
     *
     * `navigator.mediaDevices` only exists in a secure context, and [WebViewSource.Html] is given an
     * opaque origin on every platform by design - so `getUserMedia` is `undefined` there and both
     * columns would read the same no matter what the implementation did. These two therefore load a
     * real https page, which means they need network.
     */
    private val captureProbes: List<Pair<String, WebViewPermission>> = listOf(
        "Camera" to WebViewPermission.Camera,
        "Microphone" to WebViewPermission.Microphone,
    )

    private const val CAPTURE_PROBE_URL = "https://webrtc.github.io/samples/src/content/getusermedia/gum/"

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        scrolling.col {
            h1("WebView Permissions")
            text(
                "Each capability is loaded twice - denied, then granted - into a page that makes the " +
                        "difference visible. Same behaviour in both columns means the capability is not " +
                        "being enforced. Camera and Microphone additionally need the app's own OS " +
                        "permission, so expect a system prompt the first time."
            )

            for ((name, permission, html) in probes) {
                card.col {
                    h2(name)
                    // Scripts is granted alongside every other capability, since a page cannot
                    // demonstrate popups, modals, camera or downloads without running any code -
                    // except in the Scripts row itself, where that is the whole point.
                    val base = if (permission == WebViewPermission.Scripts) emptySet()
                    else setOf(WebViewPermission.Scripts)

                    rowCollapsingToColumn(40.rem) {
                        expanding.col {
                            subtext("Denied")
                            sizeConstraints(height = 14.rem).webView {
                                load(WebViewSource.Html(html), base)
                            }
                        }
                        expanding.col {
                            subtext("Granted")
                            sizeConstraints(height = 14.rem).webView {
                                load(WebViewSource.Html(html), base + permission)
                            }
                        }
                    }
                }
            }

            for ((name, permission) in captureProbes) {
                card.col {
                    h2(name)
                    text(
                        "Loaded from an https page rather than inline markup: getUserMedia only exists " +
                                "in a secure context, and inline HTML deliberately has none. Press the " +
                                "sample's start button in each column. Granted should also raise the OS " +
                                "permission prompt the first time - denying that must leave the page denied."
                    )
                    rowCollapsingToColumn(40.rem) {
                        expanding.col {
                            subtext("Denied")
                            sizeConstraints(height = 18.rem).webView {
                                load(WebViewSource.Url(CAPTURE_PROBE_URL), setOf(WebViewPermission.Scripts))
                            }
                        }
                        expanding.col {
                            subtext("Granted")
                            sizeConstraints(height = 18.rem).webView {
                                load(
                                    WebViewSource.Url(CAPTURE_PROBE_URL),
                                    setOf(WebViewPermission.Scripts, permission),
                                )
                            }
                        }
                    }
                }
            }

            card.col {
                h2("Navigation cannot leave the view")
                text(
                    "No permission grants this, so both columns must behave identically - that is the " +
                            "point. On Android a web view with no navigation client hands these to the OS " +
                            "as an Intent, launching another app with nothing granted at all."
                )
                sizeConstraints(height = 12.rem).webView {
                    load(
                        WebViewSource.Html(
                            """<body style="font:16px sans-serif;padding:1rem">
                                 <p>Every link below must do nothing at all - no app switch, no dialer,
                                 no store, no crash.</p>
                                 <p><a href="intent://scan/#Intent;scheme=zxing;end">intent:</a></p>
                                 <p><a href="market://details?id=com.android.chrome">market:</a></p>
                                 <p><a href="tel:+15555555555">tel:</a></p>
                                 <p><a href="file:///etc/hosts">file:</a></p>
                                 <p><a href="https://example.com">https: - this one may load in place</a></p>
                               </body>"""
                        ),
                        WebViewPermission.trusted,
                    )
                }
            }

            card.col {
                h2("Inline HTML has no origin")
                text(
                    "Web only - and it says so itself. Android and iOS give the web view the whole " +
                            "surface, so there is no embedding page to reach and the probe reports N/A " +
                            "rather than a result. On web it must say BLOCKED: inline HTML is given an " +
                            "opaque origin precisely so untrusted markup cannot read the page hosting it."
                )
                sizeConstraints(height = 10.rem).webView {
                    load(
                        WebViewSource.Html(
                            """<body style="font:16px sans-serif;padding:1rem"><p id="out">checking...</p>
                               <script>
                                 var out = document.getElementById('out');
                                 if (window.parent === window) {
                                   out.textContent = 'N/A - top level, there is no embedding page here';
                                 } else {
                                   try {
                                     out.textContent = 'REACHABLE - ' + window.parent.document.title;
                                   } catch (e) {
                                     out.textContent = 'BLOCKED - ' + e.name;
                                   }
                                 }
                               </script></body>"""
                        ),
                        setOf(WebViewPermission.Scripts),
                    )
                }
            }
        }
    }
}
