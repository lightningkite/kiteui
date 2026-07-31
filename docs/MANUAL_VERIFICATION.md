# Manual verification checklist

Fixes from the cross-platform review that no automated test can reach, grouped by platform so each
device only has to be picked up once.

Two routes in the example app hold the test content:

- `/review-fixes` — scroll metrics, the web progress ring, drag-reorder indicators, link scheme validation
- `/platform-fixes` — audio, the iOS progress ring, Lottie teardown, camera permissions, leaks

Everything else from the review is covered by the automated suites (2106 tests). Run them with
`./gradlew :library:jvmSsrTest :library:jsBrowserTest :library:testDebugUnitTest :library:iosSimulatorArm64Test`.

Items marked **judgement** have no objectively right answer — they need an opinion, not a pass/fail.

---

## iOS

| # | Route | Do this | Expect |
|---|---|---|---|
| 1 | `/review-fixes` §1 | Scroll the box to the bottom | Both verdict lines read PASS. "distance to end" reaches ~0 — it must not stall at a full viewport height |
| 2 | `/scroll-test` | Press "Scroll to End Smooth" on the vertical scroller | Lands exactly at the bottom, no overshoot or short stop |
| 3 | `/platform-fixes` | Play a sound effect | Audible. Previously `TODO()` — silence means the fix regressed |
| 4 | `/platform-fixes` | Trigger several sound effects rapidly | They overlap rather than cutting each other off |
| 5 | `/platform-fixes` | View the progress ring | A stroked arc on a track, hollow centre. Previously a blank view |
| 6 | `/platform-fixes` | Open a Lottie animation, navigate away, return several times | Memory stays flat; animation still plays. Was leaking its render scope and frame listener |
| 7 | `/review-fixes` §4 | Tap each link | First two open. The rest do nothing — no app switch, no dialer, no crash |
| 8 | Any themed screen | Find an element with a `Theme.transform` | translate, rotate and scale all apply together. Only one used to take effect |

Item 8 is the one worth the most attention on iOS — it has an automated test
(`ThemeTransformTest`), but that test asserts the transform matrix, not what you actually see.

**Known gap:** the iOS zero-weight NaN guard (`LinearLayout.calcSizes`) has no test. Sizes are only
observable via `screenRectangle()`, which returns null in the iOS test harness — the pre-existing
`LayoutTest` in `iosTest` is `@Ignore`d for what appears to be the same reason. To check by hand,
place two children in a `row` both at `weight(0f)` and confirm the layout renders at all rather than
collapsing or blanking.

## Android

| # | Route | Do this | Expect |
|---|---|---|---|
| 1 | `/platform-fixes` | Play a sound effect (remote URL and raw resource) | Both audible. Both were `TODO()` |
| 2 | `/platform-fixes` | Open the camera screen on a fresh install | The permission dialog appears **before** the preview starts. Previously the camera was bound and started first |
| 3 | `/platform-fixes` | Deny the camera permission | Handled cleanly, no crash, no orphaned camera |
| 4 | `/review-fixes` §4 | Tap each link | `intent:` and `file:` do nothing at all — these reach other apps and local storage on Android |
| 5 | `/review-fixes` §1 | Scroll to the bottom | Both verdicts PASS, matching iOS and web |
| 6 | Any slider screen | Bind two things to one slider | Both update. Only the most recent subscriber used to receive events |

Item 6 has an automated test (`SliderListenerTest`); confirm once by eye that real UI agrees.

## Web

| # | Route | Do this | Expect |
|---|---|---|---|
| 1 | `/review-fixes` §2 | Look at the four rings | Rings with hollow centres — not bars, not filled discs. Ratios visibly 25/50/75/100% |
| 2 | `/review-fixes` §2 | Drag the slider | The arc follows continuously |
| 3 | `/review-fixes` §2 | Turn on a screen reader, focus a ring | A progress value is announced. It is a real `<progress>`, so this comes from the platform |
| 4 | **Safari** | Repeat 1–3 | **Unverified.** `mask` and `appearance` handling are the likely divergence. Automated coverage is Chrome-only |
| 5 | `/review-fixes` §4 | Click each link | Unsafe ones are not clickable destinations |
| 6 | Any deep page | Navigate several pages deep, trigger a reset, press Back | The URL is correct and Back cannot re-enter the old stack |

Item 4 is the largest open risk in the web work — the ring is now entirely CSS, and the automated
tests read computed style out of headless Chrome only.

Item 6 has no automated coverage: Karma cannot exercise browser history traversal without
disturbing its own frame.

## All platforms

| # | Route | Do this | Expect |
|---|---|---|---|
| 1 | `/review-fixes` §3 | Drag a row slowly up and down the list | Rows do **not** shift as the indicator moves. A highlight at the drop position is right; items moving before release is not |
| 2 | Any list with pull-to-refresh | Scroll to the end of a long lazy-loaded list | More items load. On iOS this never triggered, because "distance to end" was inflated by a whole viewport |

**judgement** — item 1 is about feel. The question is whether the list is stable under the pointer,
not whether any particular pixel moved.

---

## Deployment note

`Platform.isDevelopment` on SSR now defaults to **false**. Any SSR deployment that relies on the
debug exception handlers must set `KITEUI_DEVELOPMENT=true`, or it will show production error pages
instead of stack traces. There is a test for the default; the opt-in branch is untested because
`System.getenv()` is fixed at JVM start.
