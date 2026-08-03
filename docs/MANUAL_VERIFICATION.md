# Manual verification checklist

Fixes from the cross-platform review that no automated test can reach, grouped by platform so each
device only has to be picked up once.

Three routes in the example app hold the test content. **Every row below names a screen that
exists** — nothing here asks you to go and construct a test case:

- `/review-fixes` — §1 scroll metrics, §2 the web progress ring, §3 drag-reorder indicators,
  §4 link scheme validation, §5 lazy loading
- `/platform-fixes` — §1 audio (remote, raw, picked file), §2 the iOS progress ring, §3 Lottie
  teardown, §4 camera permissions, §5 overlay reentrancy, §6 leaks, §7 theme transform
- `/webview-permissions` — every `WebView` capability, loaded once denied and once granted, plus
  the two always-denied invariants (navigation, inline-HTML origin)

The only rows without a dedicated screen are web #6 (navigation reset, which any link chain
exercises) and Android #2/#3 (camera permission ordering, which needs a fresh install).

Everything else from the review is covered by the automated suites (2106 tests). Run them with
`./gradlew :library:jvmSsrTest :library:jsBrowserTest :library:testDebugUnitTest :library:iosSimulatorArm64Test`.

Items marked **judgement** have no objectively right answer — they need an opinion, not a pass/fail.

---

## iOS

| # | Route | Do this | Expect |
|---|---|---|---|
| 1 | `/review-fixes` §1 | Scroll the box to the bottom | Both verdict lines read PASS. "distance to end" reaches ~0 — it must not stall at a full viewport height |
| 2 | `/scroll-test` | Press "Scroll to End Smooth" on the vertical scroller | Lands exactly at the bottom, no overshoot or short stop |
| 3 | `/platform-fixes` §1 | Play a sound effect | Audible. Previously `TODO()` — silence means the fix regressed |
| 4 | `/platform-fixes` §1 | Trigger several sound effects rapidly | They overlap rather than cutting each other off |
| 5 | `/platform-fixes` §1 | Press all three: AudioRemote, AudioRaw, and pick a file for AudioLocal | All three audible. All three were `TODO()` or a silent no-op on iOS |
| 6 | `/platform-fixes` §2 | View the progress ring | A stroked arc on a track, hollow centre. Previously a blank view |
| 7 | `/platform-fixes` §3 | Open a Lottie animation, navigate away, return several times | Memory stays flat; animation still plays. Was leaking its render scope and frame listener |
| 8 | `/review-fixes` §4 | Tap each link | First two open. The rest do nothing — no app switch, no dialer, no crash |
| 9 | `/platform-fixes` §7 | Look at the transformed square next to the plain one | Rotated **and** larger **and** offset. Any one of the three missing is the bug - only one used to take effect |

Item 9 is the one worth the most attention on iOS — it has an automated test
(`ThemeTransformTest`), but that test asserts the transform matrix, not what you actually see.

**Known gap:** the iOS zero-weight NaN guard (`LinearLayout.calcSizes`) has no test. Sizes are only
observable via `screenRectangle()`, which returns null in the iOS test harness — the pre-existing
`LayoutTest` in `iosTest` is `@Ignore`d for what appears to be the same reason. To check by hand,
place two children in a `row` both at `weight(0f)` and confirm the layout renders at all rather than
collapsing or blanking.

## Android

| # | Route | Do this | Expect |
|---|---|---|---|
| 1 | `/platform-fixes` §1 | Play a sound effect (remote URL and raw bytes) | Both audible. Both were `TODO()` |
| 2 | `/platform-fixes` §4 | Open the camera screen on a fresh install | The permission dialog appears **before** the preview starts. Previously the camera was bound and started first |
| 3 | `/platform-fixes` §4 | Deny the camera permission | Handled cleanly, no crash, no orphaned camera |
| 4 | `/review-fixes` §4 | Tap each link | `intent:` and `file:` do nothing at all — these reach other apps and local storage on Android |
| 5 | `/review-fixes` §1 | Scroll to the bottom | Both verdicts PASS, matching iOS and web |
| 6 | `/slider-example` | Bind two things to one slider | Both update. Only the most recent subscriber used to receive events |
| 7 | `/webview-permissions` "Popups" | In the **denied** column, tap the `target=_blank` link | It loads in place inside the frame. Reaching the system browser means the navigation client is missing |
| 8 | `/webview-permissions` "Modals" | In the **denied** column, press "onbeforeunload, then navigate" | No "Leave this page?" dialog. That one arrives by default once a WebChromeClient exists |

Item 6 has an automated test (`SliderListenerTest`); confirm once by eye that real UI agrees.

## Web

| # | Route | Do this | Expect |
|---|---|---|---|
| 1 | `/review-fixes` §2 | Look at the four rings | Rings with hollow centres — not bars, not filled discs. Ratios visibly 25/50/75/100% |
| 2 | `/review-fixes` §2 | Drag the slider | The arc follows continuously |
| 3 | `/review-fixes` §2 | Turn on a screen reader, focus a ring | A progress value is announced. It is a real `<progress>`, so this comes from the platform |
| 4 | `/review-fixes` §2 in **Safari** | Repeat 1–3 | **Unverified.** `mask` and `appearance` handling are the likely divergence. Automated coverage is Chrome-only |
| 5 | `/review-fixes` §4 | Click each link | Unsafe ones are not clickable destinations |
| 6 | Any route reached via several links | Navigate several pages deep, trigger a reset, press Back | The URL is correct and Back cannot re-enter the old stack. No dedicated screen - any navigation chain does |

Item 4 is the largest open risk in the web work — the ring is now entirely CSS, and the automated
tests read computed style out of headless Chrome only.

Item 6 has no automated coverage: Karma cannot exercise browser history traversal without
disturbing its own frame.

## All platforms

| # | Route | Do this | Expect |
|---|---|---|---|
| 1 | `/review-fixes` §3 | Drag a row slowly up and down the list | Rows do **not** shift as the indicator moves. A highlight at the drop position is right; items moving before release is not |
| 2 | `/review-fixes` §5 | Scroll the list to the bottom repeatedly | It grows to 80 items over three loads, then stops. The load counter settles at 4 - climbing on its own is the runaway bug, never growing past 20 on iOS is the scroll-metrics bug |
| 3 | `/webview-permissions` | Work down every row, comparing the denied and granted columns | They differ in every row. Identical columns mean that capability is not being enforced on this platform |
| 4 | `/webview-permissions` | Grant Camera or Microphone on a fresh install | The OS permission prompt appears first; denying it must leave the page denied too. Interrupt the prompt (rotate, tap away) — that must count as denied, not granted |
| 5 | `/webview-permissions` | Read the final card | Web: BLOCKED. Android/iOS: N/A, since the view is top-level and has no embedding page. REACHABLE anywhere means inline HTML can read the app |
| 6 | `/review-fixes` §4, then `/webview-permissions` "Navigation cannot leave the view" | Tap every link in both | Nothing happens for `intent:`, `market:`, `tel:`, `file:` or `javascript:` in either place - with or without permissions |

**judgement** — item 1 is about feel. The question is whether the list is stable under the pointer,
not whether any particular pixel moved.

---

## Deployment notes

`Platform.isDevelopment` on SSR now defaults to **false**. Any SSR deployment that relies on the
debug exception handlers must set `KITEUI_DEVELOPMENT=true`, or it will show production error pages
instead of stack traces. There is a test for the default; the opt-in branch is untested because
`System.getenv()` is fixed at JVM start.

**`WebView` has a new API.** `url`, `content` and `permitJs` are gone, replaced by a single
`load(source, permissions)`:

```kotlin
webView { load(WebViewSource.Url("https://example.com"), setOf(WebViewPermission.Scripts)) }
webView { load(WebViewSource.Html(markup)) }                       // nothing permitted
webView { load(WebViewSource.Url(url), WebViewPermission.trusted) } // a page you control
```

`url` and `content` were mutually exclusive modes pretending to be independent properties: setting
either invalidated the other, and each platform improvised a different answer for the stale one (web
threw `TODO()` for `content`; Android stashed it in `View.tag`; `url` meant "what I set" on web and
"where the user has since navigated" on Android and iOS). Permissions are part of the same call
because web and iOS fix a page's capabilities *when its load begins* — a separately-mutable flag
appeared to work while leaving the page in front of the user running under whatever it was loaded
with.

**Everything is denied unless asked for.** The seven capabilities — `Scripts`, `Popups`, `Modals`,
`Autoplay`, `Camera`, `Microphone`, `Downloads` — are each enforced on all four platforms in both
directions. A capability only *some* platforms could honour is deliberately absent rather than
present-and-ignored: form submission, for instance, which Android's `shouldOverrideUrlLoading` is
documented not to see for POSTs, so it stays permitted everywhere.

Behaviour worth knowing:

- **Popups and downloads go to the system browser** on Android and iOS. An embedded view has no
  window furniture for a real popup and no progress/destination UI for a download. The URL the
  content chose is scheme-checked (`isSafeLinkUrl`) before it reaches the OS: granting Popups or
  Downloads is permission to leave the view, not permission to name `intent:` or `file:`.
- **Camera and microphone can never exceed what the app itself holds.** The page's request triggers
  the OS runtime permission prompt if the app does not already have it.
- **Modals show a native dialog** when granted; denied, `alert`/`confirm`/`prompt` return as if
  dismissed rather than hanging.
- **`WebViewSource.Html` always gets an opaque origin** — no cookies, no storage, and on web no
  `allow-same-origin`, so inline HTML cannot reach the page embedding it even with `Scripts`. This is
  what makes it safe to render untrusted markup.
- **Navigation cannot leave the view.** Only `http(s)` (plus `about:` on iOS, which is what
  `loadHTMLString` navigates to) is allowed; anything else is refused. This matters most on Android,
  where a WebView with no `WebViewClient` hands unhandled navigations to the OS as an Intent — so an
  `intent:` or `market:` link inside the content would launch another app with **no permission
  involved at all**, and `target="_blank"` would reach the system browser even with Popups denied.
- **Never granted on any path**, since no platform offers framed content the equivalent: all four
  `allow-top-navigation*` variants, `allow-popups-to-escape-sandbox`, pointer lock, orientation lock,
  presentation, `allow-storage-access-by-user-activation`, and every form of Android local file
  access.
- **A `Url` on your own origin is not sandboxed from you.** It gets `allow-same-origin`, so a
  *relative* URL pointing at content someone else supplied can read `parent.document`, your cookies
  and your local storage. Use `Html` for anything untrusted — that is what the opaque origin is for.

Route `/webview-permissions` in the example app loads each capability twice, denied and granted, into
a page that makes the difference visible. Same behaviour in both columns means it is not being
enforced. Only the web side has automated coverage (the whole model is two iframe attributes there);
on Android and iOS these are delegate callbacks that need a device.

Android views with corner radii now use `Outline.setRoundRect` rather than a path whenever all four
corners share a radius, which is what makes `clipToOutline` actually clip below API 33. Rounded
containers that previously did not clip their children will start doing so; that is the documented
intent, but it is a visible change on API 21–32.

`DependentAction` no longer clears its state when a dependency changes **while the action is still
running** — clearing now applies only once the run has finished. Previously, editing a form while its
save was in flight wrote "ready, no error" over the in-progress state, stopping the button's spinner
and re-enabling it mid-save. If any screen relied on that to interrupt a long action, it will now stay
in its loading state until the action actually returns.

`RecyclerViewPlacerHorizontalGrid(rows)` and `RecyclerViewPlacerVerticalGrid(columns)` now `require`
their count to be at least 1. Every construction in this repo already passes a literal or a
`coerceAtLeast(1)`, but downstream code computing a count from a measured width (`(width /
cellWidth).toInt()`) will now throw at small widths instead of laying out degenerately — clamp at
the call site.
