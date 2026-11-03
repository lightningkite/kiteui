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

    /**
     * ~62ms of silent 8kHz mono PCM, inline.
     *
     * The autoplay probe used to point a `<video>` at a third-party URL, which fails on every
     * platform for a different reason: inline HTML is given an opaque origin by design, and an
     * opaque-origin document cannot fetch cross-origin media on Android, while on web the same
     * request is refused whenever the page is cross-origin isolated. Embedding the clip sidesteps
     * all of it - and the WAV's sample data is entirely zeroes, so all but the 45-byte header
     * base64-encodes to a run of 'A', which is why this is spelt as a header plus a repeat rather
     * than 1,392 opaque characters.
     */
    private val SILENT_WAV_DATA_URI: String =
        "data:audio/wav;base64,UklGRgwEAABXQVZFZm10IBAAAAABAAEAQB8AAIA+AAACABAAZGF0YegDAAAA" +
                "A".repeat(1332)

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
                 <p id="out">waiting...</p>
                 <audio id="clip" src="$SILENT_WAV_DATA_URI"></audio>
                 <script>
                   var out = document.getElementById('out'), clip = document.getElementById('clip');
                   function verdict(text) { out.textContent = text }
                   var started = clip.play();
                   if (started && started.then) {
                     started.then(function () { verdict('GRANTED - playback started with no gesture') })
                            .catch(function (e) { verdict('DENIED - ' + (e && e.name ? e.name : e)) });
                   } else {
                     setTimeout(function () {
                       verdict(clip.paused ? 'DENIED - still paused' : 'GRANTED - playback started with no gesture');
                     }, 500);
                   }
                 </script>
                 <p><small>The clip is silent by design - the verdict above is the result, not what you
                 hear. It is embedded rather than fetched, so this works offline and needs no
                 third-party site to stay up.</small></p>
               </body>""",
        ),
        Triple(
            "Downloads",
            WebViewPermission.Downloads,
            // Two links, because no single one can prove the rule on every platform.
            //
            // The data: link is the one web can actually judge. The `download` attribute is only
            // honoured for same-origin, blob: and data: URLs, and inline HTML is given an opaque
            // origin by design - so a cross-origin target is ignored and the link merely navigates
            // in *both* columns, which is what the https link below used to do on its own. With a
            // data: URL the attribute takes effect, so allow-downloads is what decides.
            //
            // The https link is the one Android and iOS judge: there a permitted download leaves
            // the view for the system browser, and the URL is scheme-checked on the way out.
            """<body style="font:16px sans-serif;padding:1rem">
                 <p><a href="data:text/plain;charset=utf-8,KiteUI%20download%20test" download="kiteui-download-test.txt">Download a text file (data:)</a><br>
                 <small><b>Web.</b> Granted: the file downloads. Denied: the browser blocks it and logs to the console.</small></p>
                 <p><a href="https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf" download>Download a PDF (https)</a><br>
                 <small><b>Android.</b> Granted: the system browser takes over. Denied: nothing happens.
                 On iOS WebKit renders a PDF itself, so the download path is never entered. On web this
                 one is cross-origin, so <code>download</code> is ignored and it just navigates - use the
                 data: link above there.</small></p>
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

                    rowCollapsingToColumn(45.rem) {
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
                            sizeConstraints(height = 32.rem).webView {
                                load(WebViewSource.Url(CAPTURE_PROBE_URL), setOf(WebViewPermission.Scripts))
                            }
                        }
                        expanding.col {
                            subtext("Granted")
                            sizeConstraints(height = 32.rem).webView {
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
