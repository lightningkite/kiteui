# WindowedList — plan and research findings

Status: **Phase 0 partially run. See §0 for results — both original justifications are now
weaker than when this plan was written.** Do not start Phases 2-5 until Phase 0 completes.

---

## 0. Spike results (branch `windowed-spike`, worktree `~/Projects/kiteui-spike`)

### Spike A — ordering fix: DONE, and it needed new framework API

The proposed "~15-line fix" was **not implementable as-is**. KiteUI had no way to reorder a
container's children at all: `removeChild` calls `onShutdown` (terminal), `nativeAddChild`/
`nativeRemoveChild` are `protected`, `internalChildren` is private, and `forEachReorderable`
reorders *data*, not elements.

Added `ContainerElement.moveChild(from, to)`, implemented in `NativeContainerElementCommonCode`
via a new overridable `nativeMoveChild`. No per-platform actuals needed — it composes from the
existing native primitives. `ProgrammaticLayout` (js) overrides it to move the DOM node only,
because its own `nativeAddChild` registers a MutationObserver per call and calls
`invalidateLayout()`: a naive move would leak a listener per move and re-enter layout from inside
`measure`.

Verified in real ChromeHeadless 151 (`MoveChildSpikeTest`, 3 tests): DOM order changes, the moved
element is **not** shut down, and its reactive binding keeps updating after the move.
A deliberate negative control was run to confirm the assertions actually fire. Full JS suite after
the change: **535 tests, 0 failures.**

Recycler2 now re-sorts active cells into data order after each placer run when `recycling` is on
(`reorderCellsToDataOrder`, with a `statsReorderMoves` counter). **This path is compiled but not
yet runtime-tested** — it needs a scrolling harness.

Cost: ~25 lines of common code + 1 override. Verdict: **the accessibility fix is real and cheap,
but it is a framework change, not a Recycler2-local change.**

### Spike B — measurement cost: REFUTES the performance hypothesis

Fair comparison, both paths measuring genuinely dirty layout, real ChromeHeadless, 30-item window,
60 rounds, median (`MeasureCostSpikeTest`):

| path | 30 items | per item |
|---|---|---|
| `measureByDuplicate` (clone + body insert + reflow + remove) | 1.2ms | 0.040ms |
| direct `scrollHeight` on the real element | 0.7ms | 0.023ms |
| **speedup** | **1.7x** | |

**1.2ms per placer pass fits comfortably in a 16.6ms frame, and the best case only halves it.**
The claim that clone-measurement is *the* cause of Recycler2's web slowness is not supported.

Caveats that may understate the real cost: the test document is tiny, and `measureByDuplicate`
appends its clone to `document.body`, so its forced reflow scales with **total document size**;
clone cost also scales with item subtree size. Test items were simple (two texts in a col in a
row). A real app with a large DOM and heavy rows could look materially worse.

Also note `measureByDuplicate` answers a **counterfactual** ("what size would this be at
constraint W×H"), which `getBoundingClientRect` cannot answer for an element not laid out at that
width. It is not a drop-in replacement.

**Implication:** Recycler2's slowness is more likely driven by the *number* of layout passes —
MutationObserver → `invalidateLayout` storms, and the double placer run ("dang it, we have to
rerun the layout", `Recycler2.kt:775,783`) — than by per-measure cost. **Next measurement should
count `invalidateLayout` calls and placer runs during a real scroll**, not micro-benchmark the
measure.

### Spike C — Android max scroll height: NOT RUN (needs a physical 4x-density device)

### Incidental findings

- `Recycler2Test.kt`'s `assertManualReview` tripwires on `Recycler2.kt` and
  `ScrollView.commonHtml.js.kt` are **inert**: `currentHash` and `reviewedHash` are equal
  literals, so the test always passes, and neither value matches the current file under SHA-1 or
  git-blob hashing. They did not fire for this edit. The *intent* — manual retest of Recycler
  View and View Pager on Chrome/Safari/Firefox on any edit to either file — still stands and is a
  real cost on any Recycler2 change.
- `jsBrowserTest` runs real ChromeHeadless via Karma and is a good harness for this work:
  full suite ~20s.

### Revised read

Both original justifications are weaker than assumed. Accessibility is fixable in Recycler2 for
~25 lines of new framework API. Performance is not primarily the measure path. Real scroll extent
is capped by §3. **The strongest remaining justification is SSR — see §0.1.**

---

## 0.1 SSR — the justification that survives everything above

**This is the one advantage Recycler2 cannot be retrofitted to have.**

`SsrContext.kt:39-55` does not merely lack a viewport; it **throws** on access, by design:

> The viewport (window width/height) is unavailable during SSR: the server never learns the
> client's actual screen size, so any value here would be a guess that can silently mismatch real
> devices. These accessors throw instead of returning a guessed value, so that any code that needs
> viewport-dependent DOM *structure* fails loudly during SSR rather than shipping a layout that's
> subtly wrong on hydration.

Recycler2's DOM structure **is** the output of a measure pass against `scroll.viewport`. The one
input SSR structurally cannot supply is the one its structure derives from, so it must be skipped
on the server and ships as empty content. That is architectural, not a bug.

WindowedList inverts this. Its structure is `[leading spacer][N items][trailing spacer]`, and
**N can be a chosen constant rather than a viewport-derived value.** So:

- Server emits a fixed first window (`ssrItemCount`) plus spacers sized from estimates.
- Client hydrates the identical structure — satisfying the hydration contract in `root.kt`
  ("components must produce the same DOM structure on both SSR and client... when mismatches
  occur, the SSR element is replaced with a freshly created client element").
- Viewport only influences what happens *after* first client layout, via the normal
  estimate/correct path.

### Why this is decisive

It survives every refutation above: it does not depend on the performance claim Spike B refuted,
it is not capped by the §3 scroll-height limits, and fixing Recycler2 cannot deliver it.

Concrete wins:
- **SEO** — crawlers see real list content instead of an empty container.
- **FCP/LCP** — first window painted before hydration.
- **No-JS / progressive enhancement** — first window readable with scripting off.
- **Accessibility, compounded** — a screen reader gets real content *before JS runs*, not merely
  correctly-ordered content afterwards.
- **Scrollbar is roughly right immediately**, from the estimated trailing spacer.

### API addition

```kotlin
    /**
     * Items rendered during SSR, before any viewport is known.
     *
     * Deliberately a constant, not derived from any viewport guess: the hydration contract
     * requires server and client to produce identical DOM structure, and SsrContext throws on
     * viewport access precisely to prevent structure that depends on a guessed size. The client
     * adjusts to the real window after its first layout pass.
     */
    public var ssrItemCount: Int = 20
```

### Decisions (confirmed by maintainer, 2026-08-26)

- **Hydration reuse is workable**, and there are existing tools for transferring SSR'd data
  (see `SsrResourceRegistry` / `SsrResource` in `SsrContext.kt`). Use them for the first window's
  data rather than refetching on the client.
- **The server does not measure at all.** It emits exactly `ssrItemCount` items and stops. It does
  not attempt an estimated trailing-spacer height. The client computes all spacer sizes after its
  first layout pass.
  - Consequence: **the §3 max-scroll-height cap does not apply to server-emitted HTML at all**,
    because the server never emits a large spacer.
  - Design note: still emit the spacer *elements* (at zero size, `shown = false`) on the server so
    server and client tree shape match exactly; the client only sets their sizes afterwards.
- **`ssrItemCount` defaults above a typical first window**, so hydration never inserts items above
  the fold (adding below is invisible; adding above shifts content).
- **`virtualizeThreshold`** already renders short lists fully — SSR-correct today, no extra work.

### Scroll extent: real first, fake as an acceptable fallback

Real scrollbar is the target. If the §3 caps or platform behaviour make it unworkable at scale, a
fake scrollbar (as Recycler2 uses) is an acceptable fallback.

**This is not a fallback to Recycler2's architecture.** The SSR, hydration, accessibility, and
focus-order wins all come from *a real container with real children in document order*, not from
real scroll extent. A fake scrollbar degrades exactly one feature and leaves every other
justification intact.

Therefore the extent strategy must be **behind an interface from day one**, so the choice is
swappable per-platform and per-list-size rather than assumed throughout the implementation:

```kotlin
/**
 * How the list presents scrollable range. Real extent gives a native, accurate scrollbar but is
 * capped by platform limits (see §3); fake extent lifts the cap at the cost of scrollbar fidelity.
 */
public interface ScrollExtentStrategy {
    /** Content extent to present, given the model's estimate of total size. */
    public fun presentedExtent(trueExtent: Double, itemCount: Int): Double
    /** Maps a scroll offset in presented space to one in true content space. */
    public fun toContentOffset(presented: Double): Double
    /** Maps a true content offset to presented space. */
    public fun toPresentedOffset(content: Double): Double

    /** 1:1. Native scrollbar, exact for the measured fraction. Capped by platform max extent. */
    public object Real : ScrollExtentStrategy
    /** Compresses content space into a bounded presented space. Uncapped; scrollbar approximate. */
    public class Compressed(public val maxPresentedExtent: Double) : ScrollExtentStrategy
}
```

`Real` is the default; the list falls back to `Compressed` when
`trueExtent > platformMaxScrollExtent`. Because the container and child order are unchanged
either way, the fallback is invisible to SSR, accessibility, and focus.

### Grid — not now, but do not preclude it

Grid support is wanted eventually, via a **new separate grid container** rather than by
generalising `RowOrCol`. Implication for this work: keep the window computation
(offset → index range → spacer sizes) **independent of the container type**, so a grid container
can reuse it later. Do not hard-couple windowing logic to linear layout.

### Remaining unknowns on this path

- Which `SsrResource` mechanism best carries the first window's data, and how it interacts with a
  `Reactive<List<T>>` source that would otherwise refetch on the client.
- Whether hydration's deferred matcher reuses item nodes cleanly when children arrive from a
  reactive source — expected to work, but confirm on the first real render rather than assuming.

---

A virtualized list that uses native linear layout (`RowOrCol`) with children in data order,
rather than `Recycler2`'s `ProgrammaticLayout` absolute placement. Motivation was accessibility
(element order) and performance.

Research: six parallel agents (web/Android+iOS platform validation, prior art, KiteUI internals,
adversarial review). Claims marked VERIFIED were checked directly against source by the author.

---

## 1. Recommendation

**Do not build WindowedList yet.** Two spikes (~2 days) may cancel it.

The original proposal rested on two justifications. One is gone; the other is scoped to one
platform of four.

### Accessibility — justification gone (VERIFIED)

`Recycler2.kt:247-262` already contains ordered insertion, gated on `recycling`:

```kotlin
// Accessibility: we're going to ensure this is inserted in the correct order.
this@MyCell.view = element
if (this@Recycler2.recycling) {
    this@Recycler2.cells.addChild(element)          // append — order arbitrary
} else {
    /* binarySearchBy(index) { it.index } … ordered insert */
    this@Recycler2.cells.addChild(index, element)
}
```

`recycling` defaults to `true` (`Recycler2.kt:57`). So `recycling = false` yields correct order
today, at the cost of never reusing views.

Absolute positioning was never the cause — DOM order and visual position are independent.
`ProgrammaticLayout` sets `position:absolute` (`ProgrammaticLayout.js.kt:40`), which has no
bearing on what a screen reader reads. The real cause: `onPullForPlacing` (`:272-284`), the
reuse path, never repositions the pooled node.

### Performance — real, but web-only (VERIFIED)

`ProgrammaticLayout.js.kt:103` measures via `measureByDuplicate`
(`helpers.commonHtml.js.kt:84-99`): deep-clone the item, append to `document.body`, read
`scrollWidth`/`scrollHeight` (forced full-document reflow), remove.

There is a cache on the DOM node, but `nativeAddChild` (`ProgrammaticLayout.js.kt:38-44`)
attaches a **recursive MutationObserver per child** that nulls the cache and calls
`invalidateLayout()` on any content mutation. Updating a recycled cell's content therefore
triggers invalidate → relayout → re-clone-measure every cell.

Android uses `View.measure()`; iOS uses `sizeThatFits2`. No equivalent pathology on either.

### The architectural premise does not hold (VERIFIED)

`NativeContainerElement.kt:477-492` — both `removeChild` overloads call `onShutdown()`
unconditionally. `NativeElement.kt:500-511`:

```kotlin
override fun onShutdown() {
    if (isShutdown) return
    job.cancel()
    isShutdown = true
    ...
    parent = null
}
```

`isShutdown` is set true at `:503` and **never reset anywhere in the codebase**. `job.cancel()`
kills the coroutine scope, and a cancelled `Job` cannot be restarted — so even adding a reset
would not revive the element's reactive bindings.

**Detach-and-reattach is structurally impossible** without changing `NativeElement`'s lifecycle
contract for every element type. "Only the window lives in the tree" is not buildable. The
achievable design is a bounded pool of hidden-but-mounted elements — which is what Recycler2 and
`foreach.kt` already do.

Every existing recycler avoids `removeChild` on anything it wants to reuse:
- `foreach.kt:268-337` — slot pool, toggles `shown`, only removes past `poolCap`.
- `Recycler2.MyCell.animatedDismiss()` — sets `shown = false`, parks in `reuseableCells`, kept in
  the tree. Physically removes only when `recycling = false`. (No cap on `reuseableCells`.)

### Remaining delta

Between "fixed Recycler2" and "WindowedList": real linear layout instead of `ProgrammaticLayout`,
and real scroll extent instead of the 50,000px sentinel + fake scrollbar. Both real wins. Neither
is the original pitch.

---

## 2. Phase 0 — gating spikes

**Spike A** (~½ day): reposition the reused node in `onPullForPlacing`. Does order come out
correct with `recycling = true`? What does repositioning ~30 nodes per scroll cost?

**Spike B** (~1 day): replace `measureByDuplicate` with `getBoundingClientRect` on the real
element (or an RO-backed cache); benchmark Recycler2 on web against the current baseline.

**Spike C** (~½ day): measure real max scroll extent on a 4x-density Android device.

Decision: if A and B land, fix Recycler2 and stop. If B cannot be fixed without the layout
rewrite, build WindowedList per below.

---

## 3. Scale ceiling — decide before writing code

All platforms **silently clamp**; content past the cap becomes unreachable with no error.

| Platform | Cap | Source |
|---|---|---|
| Android | **16,777,215px** | `View.java:2111`, `MEASURED_SIZE_MASK = 0x00ffffff` (24-bit) |
| Firefox | 17,187,496px | Mozilla bug 1527883 (open) |
| Chrome | 33,554,432px | 2^25 LayoutUnit |
| Safari | unverified | no primary source found |

Android is binding because the cap is in **physical** pixels, so density multiplies it away.
At 48dp rows: ~349k items @1x, ~175k @2x, ~116k @3x, **~87k @4x**.

A 100,000-item list **exceeds the cap on xxxhdpi devices.**

This is the price of real scroll extent. RecyclerView and `LazyColumn` never hit it because they
never build a real content height — the strongest argument for the model being abandoned.
TanStack (#460) and react-window (#823) both carry unaddressed issues from users hitting this.

**Choose:** cap supported N (~80k) and fail fast, or build a compression mode above the cap
(which is Recycler2's fake scrollbar, reinvented).

---

## 4. Design corrections from research

### Fenwick tree → block list

No surveyed implementation uses a Fenwick tree. `insert`/`remove` are O(N) rebuilds, so
prepend-to-chat (the most common live mutation) is worst-case and paged sources rebuild per page.

Use blocks of ~1024 with cached sums; untouched spans stored run-length as `{count, estimate}`.
Memory tracks *measured* items, not N. O(1) amortized prepend/append, O(block) mid-list insert.
This is a shallow B+-tree/rope — established outside virtualizers (CodeMirror 6 `Text`).

Prior art for comparison: TanStack uses a flat cumulative `Float64Array` rebuilt forward from the
earliest dirtied index (O(N-i) update); react-window uses an index-keyed array with a high-water
mark; lit-virtualizer and Compose `LazyColumn` give up on exactness entirely
(`count × running average`, recomputed live, forgetting sizes once off-screen).

### Compensation has four triggers, not one

The original rule matches TanStack's `defaultShouldAdjust` (`index.ts:1589-1603`), but they
needed more:

1. **First measurement above viewport** — the original rule.
2. **Re-measurement** of an already-measured item (late images, font swap, streaming text). Gate
   on *fully* above the fold, not spanning it — TanStack #1218 was a chat bubble dragging the
   viewport down every tick.
3. **Structural change** (prepend/remove/reorder) — fires no measurement event at all, so a
   50-message prepend jumps with zero correction. Needs a separate key-based anchor
   snapshot/restore path (TanStack PR #1176).
4. **Viewport resize** — rotation, keyboard, window. Changes what "above" means with nothing
   measured.

Batch all deltas and apply once per frame, atomically. react-virtualized #853 is the
visible-wrong-frame bug from splitting invalidate and compensate across dispatch cycles.

### Size-neutrality is a rule to enforce, not a free property

The spacer must grow by **the value already recorded in the extent structure**, never a fresh DOM
read — otherwise an in-flight transition or undelivered RO callback silently corrupts total
height with no visible symptom until an unrelated query goes wrong.

Also: forbid vertical margins on item roots (margin collapsing makes per-item extent undefined —
adjacent margins collapse to the max, not the sum), define extent as flow contribution *including*
allocated gap, and keep exact fractional px throughout (rounding compounds over thousands of
boundary crossings into visible drift).

### Zero spacers use `shown = false`, not `size = 0.0` (VERIFIED per platform)

Gap is keyed off hidden state on all three platforms:
- iOS `LinearLayout.kt:112,181,266` — skips gap for `view.hidden`
- Android `SimplifiedLinearLayout.kt:93-98` — guards on `!= GONE`
- Web `rerunOptimizedBottomMarginCalc` — checks the `hidden` attribute

And `shown = false` maps to exactly those (`GONE` / `hidden = true` /
`.kui[hidden]{display:none !important}`). `visible = false` maps to `INVISIBLE` and **still
consumes a gap** — do not use it.

Note: web's `RowOrCol` defaults to **non-flex** mode (`display:block` faking gap via
`margin-bottom` on each child), entering real flexbox only when a child has `flexGrow > 0` or the
row is horizontal. Spacer math must be correct in both modes.

Dead-but-read plumbing exists for per-boundary gap overrides: Android
`LayoutParams.gapBeforeOverride` (`SimplifiedLinearLayout.kt:984`, read at 4 sites, never
written) and iOS `extensionSpacingBeforeOverride`. Reviving these is an alternative to relying on
hidden-child gap-skip. Web has no equivalent.

### Accessibility — two corrections

Not a role property on `Element` (the nesting objection was correct — `asList`/`asListItem`/
`setupAsListContainer` already exist, implemented via `ApplyTag` wrapper insertion on web, which
is what composes under nesting where a single `role` attribute cannot).

But **not a no-op on native either**:
- **Android**: `AccessibilityNodeInfo.CollectionInfo`/`CollectionItemInfo`.
  `LayoutManager.getRowCountForAccessibility` returns the adapter's **full** `getItemCount()` —
  that is how TalkBack announces "item 4231 of 100,000" with 30 views alive.
- **iOS**: `accessibilityContainerType = .list` + `accessibilityElementCount` +
  `accessibilityIndexInContainer`.
- **Web**: use the WAI-ARIA APG **feed pattern** (`role="feed"` / `role="article"` +
  `aria-posinset`/`aria-setsize`), not bare `ul`/`li`. The feed contract makes eviction
  **focus-driven** — never evict the item holding focus.

### `batchChildChanges` — dropped

`ViewGroup.addView` (AOSP, line 5102) only calls `requestLayout()`, and `View.requestLayout()`
short-circuits on `!mParent.isLayoutRequested()` — five insertions cost exactly one layout pass.
iOS coalesces identically; web only forces layout when geometry is *read* between writes.
Unnecessary on all four platforms. The real rule is a discipline one: never interleave geometry
reads with child mutations.

---

## 5. API

### Layer 0 — platform primitives

```kotlin
@ExperimentalKiteUi
public expect class ElementSizeWatcher(context: ElementContext) {
    public var onChanged: (changed: List<Element>) -> Unit   // batched, one call per layout pass
    public fun watch(element: Element)
    public fun unwatch(element: Element)
    public fun sizeOf(element: Element): Size
    public fun close()
}
// web: one ResizeObserver, many targets, borderBoxSize
//      (getBoundingClientRect fallback below Safari 16.4)
// android: per-child addOnLayoutChangeListener, coalesced via dirty flag
// ios: follow ProgrammaticLayout.ios.kt:108-146 — push-based subviewDidChangeSizing
//      + myInvalidated dirty flag. NOTE: the commented-out ResizeObserver in
//      directViewActuals.kt:213-240 is dead Kotlin/JS copy-paste, not an iOS attempt.

@ExperimentalKiteUi public expect val Element.currentSize: Size
@ExperimentalKiteUi public expect fun Element.boundsWithin(ancestor: Element): Rect

/** After layout, before paint. web: rAF; android: OnPreDrawListener; ios: layoutSubviews. */
@ExperimentalKiteUi public expect fun ElementContext.beforeNextPaint(block: () -> Unit): () -> Unit

public expect class LayoutSpacer(context: ElementContext) : NativeElement {
    public var vertical: Boolean
    public var size: Double          // set shown=false at zero, or you eat a phantom gap
}
```

`ScrollingBehaviors` additions:

```kotlin
    /**
     * Shift by an exact delta without emitting a scroll event, cancelling momentum, or animating.
     * NOT scrollToKeepAnimations — that is an absolute position setter with anti-jank timing
     * (web re-forces scrollLeft/Top for 32 frames while pinning what viewport reports;
     * android queues queuedJumpX/Y for the pre-draw listener). The delta math is unwritten.
     */
    public fun compensateScroll(dx: Double, dy: Double)
    public val currentViewport: Rect
    /** True while touch-momentum/deceleration is active; compensation must queue, not apply. */
    public val momentumActive: Reactive<Boolean>
```

Accessibility — modifier, reactive, composes under nesting:

```kotlin
public expect fun ElementWriter.CanAddListElementModifier.asListItemAt(
    index: Reactive<Int>, setSize: Reactive<Int>,
): ElementWriter.CanAddListElementModifier

@InternalKiteUi
internal expect fun ContainerElement.setupAsVirtualListContainer(totalItems: Reactive<Int>)
```

### Layer 1 — extent model

```kotlin
/**
 * Ordered extents with prefix sums. Blocks of ~1024 with cached sums; untouched spans held
 * run-length so memory tracks measured items, not N. O(1) amortized at the ends.
 */
public class ExtentBlockList {
    public val count: Int
    public val total: Double
    public fun reset(count: Int, defaultExtent: Double)
    public operator fun get(index: Int): Double
    public operator fun set(index: Int, extent: Double)
    public fun offsetOf(index: Int): Double      // includes gap contribution
    public fun indexAt(offset: Double): Int
    public fun insert(at: Int, count: Int, extent: Double)
    public fun remove(at: Int, count: Int)
    public fun prepend(count: Int, extent: Double)
    public fun append(count: Int, extent: Double)
}

public interface WindowedSizeModel<ID> {
    public fun extentOf(id: ID, type: Any?): Double
    public fun isMeasured(id: ID): Boolean
    public fun record(id: ID, type: Any?, extent: Double): Double   // returns delta
    public fun forget(id: ID)
    public fun clear()
}

/** Per-type average, frozen at insertion — do not let it float live (Compose's jitter source). */
public class RunningAverageSizeModel<ID>(
    seed: Double = 48.0,
    cacheCap: Int = 10_000,
) : WindowedSizeModel<ID>
```

### Layer 2 — the view

```kotlin
public class WindowedList<T, ID : Any>(
    context: ElementContext,
    public val vertical: Boolean = true,
    public val refreshAction: Action? = null,
) : ElementWithChildren, Element {

    public var data: RecyclerViewData<T, ID>
    public var rendererSet: RecyclerViewRendererSet<T, ID>
    public var sizeModel: WindowedSizeModel<ID>

    public var overdraw: Double = 0.5
    public var estimatedItemExtent: Double = 48.0
    /** Hidden-but-mounted reuse pool. Tree size is bounded by this, NOT by the window. */
    public var poolCap: Int = 32
    public var virtualizeThreshold: Int = 40
    public var gap: Dimension?
    override var paddingByEdge: Edges?

    /** Never evict the item holding focus. Required by the ARIA feed contract. */
    public var keepFocusedMaterialized: Boolean = true
    /** Fail fast rather than silently clamp past the platform's max scroll extent. */
    public var maxItemsBeforeFailure: Int = 80_000

    public val firstVisibleIndex: MutableReactive<Int>
    public val lastVisibleIndex: MutableReactive<Int>
    public val centerIndex: MutableReactive<Int>
    public fun scrollToIndex(index: Int, align: Align = Align.Start, animate: Boolean = false)

    public var snapToElements: Align?
    public var scrollSnapStop: Boolean

    public val stats: Stats
    public class Stats {
        public var materializedCount: Int
        public var pooledCount: Int
        public var firstMaterializedIndex: Int
        public var lastMaterializedIndex: Int
        public var leadingExtent: Double
        public var trailingExtent: Double
        public var viewsCreated: Int
        public var viewsReused: Int
        public var windowRecomputes: Int
        public var scrollCompensations: Int
        public var compensationsDeferredForMomentum: Int
        public var measuredItemCount: Int
        public fun reset()
    }

    override val driverActions: AiDriver.Actions   // + "windowStats"
}
```

### Layer 3 — DSL

```kotlin
public fun <T, ID : Any> ElementWriter.windowedCol(
    items: Reactive<List<T>>,
    id: (T) -> ID,
    setup: WindowedList<T, ID>.() -> Unit = {},
    render: ViewWriter.(item: Reactive<T>, index: Reactive<Int>) -> Unit,
): WindowedList<T, ID>
// + windowedRow, + RecyclerViewRendererSet overload, + RecyclerViewData overload
```

### Implementation constraints

**Theme cascade.** `Recycler2` sets `fromParentNonCascading` on its inner `ProgrammaticLayout`
(`Recycler2.kt:110`) because that layer is invisible plumbing the app author never wrote —
cascading normally would apply an unintended extra reversion. A plain `col` as WindowedList's
**sole** public boundary needs no such handling. But any extra internal wrapper between the caller
and the `RowOrCol` needs the same treatment, or themes double-revert.

**`asListItem` wrapper insertion (web).** Retags a `div`/`span` root in place, but inserts a
`PassthroughContainer` (`display:flex`) wrapper for any other root — e.g. a `button`. That wrapper
becomes what gap/offset math operates on. Either constrain item roots or make offset math robust
to the extra layer.

**Padding.** `Recycler2`'s `lastRecordedPadding*` cache exists only because its delegate invents
absolute coordinates and `scrollToIndex` can run outside a measure pass. Native layout applies
padding to the `RowOrCol` automatically, so WindowedList does not need the cache — but windowing
math computed against `scroll.viewport` still needs padding awareness.

---

## 6. Testing

**`docs/TESTING_GUIDE.md` is stale.** `kiteUiTest`, `findByTestId`, `KiteUiTestHarness`, and
`awaitStability` appear nowhere in source. The real surface is the string-command driver protocol:
`UiTestBackend.command(String)` → `AiDriver.handleCommand` (`AiDriver.kt:155+`). Both
`LocalUiTestBackend` (in-process) and `RemoteUiTestBackend` (HTTP → daemon → WebSocket) funnel
through it, so one new command lands in both. The doc needs fixing independently.

**`jsBrowserTest` runs real headless Chrome** via Karma (`library/build.gradle.kts:51-57`) — real
CSS layout, real ResizeObserver, real scroll events.

### Recommended strategy

1. Pure tests for `ExtentBlockList` + window math — no UI, no harness, property-based against a
   naive O(n) reference.
2. Driver additions: `bounds`, `scrollTo`, `scrollBy`, `scrollInfo`, `windowStats`, `awaitLayout`.
3. Scripted-scroll geometry tests in headless Chrome.
4. Android/iOS integration tests for compensation and accessibility only.

### On the deterministic JVM layout backend

Recommended **against**, pending the user's call. Every hard bug here is a timing bug —
rAF-vs-scroll ordering, async RO delivery one frame late, RO loop errors, compensation landing
after paint, iOS momentum deferral. A synchronous deterministic engine has no frames and no async
and structurally cannot express any of them. To be faithful it must model flex gap, padding,
weight, and text wrapping at a given width (item height depends on width) — approximate those and
you get green tests with red browsers.

Note `jvmSsr` has **no layout engine at all** — `ProgrammaticLayout.jvm.kt:31-33` has an empty
`invalidateLayout()` body; SSR emits static HTML and defers all layout to the eventual browser. So
this would be built from nothing.

Test plan (applies whichever backend decision is made):

```kotlin
// accessibility
treeOrderMatchesDataOrder / visualOrderMatchesTreeOrderAfterHeavyScrolling
tabOrderFollowsDataOrder / posInSetAndSetSizeReflectDataIndex
focusedItemStaysMaterializedWhenScrolledOutOfWindow

// estimate/correct correctness
materializesOnlyWindowPlusOverdraw
correctingEstimateAboveViewportDoesNotMoveContent
growingAnItemInViewportDoesNotMoveItemsAbove
scrollDownThenBackUpLandsOnSameOffset
variableHeightItemsConvergeToExactTotalExtent
insertAboveViewportKeepsVisibleItemsStill / removeAboveViewportKeepsVisibleItemsStill
prependFiftyItemsKeepsVisibleItemsStill        // structural trigger, no measurement event
reorderPreservesMeasuredSizes
viewportResizeReAnchorsRatherThanCompensates

// performance claims
viewsCreatedStaysBoundedOverFullScrollOfHundredThousand
oneWindowRecomputePerScrollFrame

// edges
emptyListAndSingleItem / belowThresholdSkipsVirtualizationEntirely
itemsTallerThanViewport / zeroSpacerConsumesNoGap
```

---

## 7. Out of scope

WindowedList as designed is linear-only. Recycler2 has `RecyclerViewPlacerVerticalGrid` (154
lines), `Horizontal` (120), `RecyclerViewPagingPlacer` (80).

Also unhandled: reverse/chat layout (starting at bottom means `offsetOf(N)`, an estimate — you
land wrong and visibly correct on every chat open), drag-to-reorder (`forEachReorderable.kt`),
insert/remove animations, keyboard nav (Page Up/Down; and tabbing off the last item onto a spacer
is a **new** a11y bug this design creates), print (`showOnPrint`), nested scrolling, RTL.

**Recycler2 may be deprecated for linear lists only.** It stays supported for grid and paging
until equivalents exist.

---

## 8. Sequencing

**Phase 0 — gates (2 days).** Spikes A, B, C above. Decision point.

**Phase 1 — no-regrets (1 week).** Useful whichever way Phase 0 goes:
- `ExtentBlockList` + pure tests
- Driver `bounds` / `scrollTo` / `scrollBy`
- Fix `TwoWayNestedScrollView.scrollToIgnoringClamp` (`:2259-2262`) — currently reads
  `mScroller?.finalX` and discards it, moving the view but leaving the OverScroller's trajectory
  stale, so any compensation during a fling snaps back on the next `computeScroll()`. Needs
  `forceFinished(true)` first.
- Wire `CollectionInfo` (Android) / `accessibilityContainerType` (iOS)
- Fix `docs/TESTING_GUIDE.md`

**Phase 2 — primitives.** `ElementSizeWatcher`, `currentSize`, `boundsWithin`, `LayoutSpacer`,
`beforeNextPaint`, per platform, each tested alone.

**Phase 3 — WindowedList, fixed-height only.** No compensation.

**Phase 4 — variable heights + compensation.** All four triggers, frame-batched, plus the iOS
momentum deferral queue. Budget most of the project here — TanStack needed the queue plus two
follow-up fixes to the queue itself (#1229, #1233).

**Phase 5 — `asListItemAt`, feed pattern, deprecate Recycler2 for linear.**

---

## 9. Known-hard problems to accept, not solve

- **The scrollbar will not be exact** while most of a large list is unmeasured, and will visibly
  resize as clusters get measured. Information-theoretically unavoidable without measuring
  everything. ag-grid documents this as permanent; RecyclerView and Compose decline real extent
  entirely. Mitigate with damped/animated corrections (lit-virtualizer's model); do not promise
  pixel-perfect.
- **iOS Safari momentum**: WebKit defers dynamic-measurement compensation while `isScrolling`
  (TanStack #1250, WebKit #884), producing a ~150ms sag-then-snap. No clean fix; defer-and-batch
  until scroll settles.
- **Safari has no `overflow-anchor`** at any version, so native scroll anchoring cannot be the
  mechanism anywhere. Manual compensation is mandatory.
- **Never set `scroll-behavior: smooth`** on the virtualized scroller, and never toggle
  `overflow:hidden` on it (WebKit bug 238497).
- **Find-in-page cannot reach unmaterialized items.** No browser primitive solves this. Offer
  in-app search.
- **Scrollbar show/hide changes item width**, reflowing wrapping text and invalidating every
  cached height at once. `scrollbar-gutter: stable` mitigates.

---

## 10. Unverified / open

- Safari max scrollable height (no primary source found).
- Exact visual failure mode past Android's 24-bit height cap.
- iOS `contentOffset`-during-deceleration specifics and rubber-band interaction (agent hit search
  quota; mechanism inferred from `contentOffsetAdjustment` existing).
- iOS practical max content size (no documented ceiling; `CGFloat` is `Double`, so likely safer
  than Android).
- Current NVDA/JAWS/VoiceOver virtual-buffer recovery behaviour on DOM churn.
- Whether a spacer's own height change suppresses native scroll anchoring for that change.

## 11. Mobile findings (measured, not predicted)

Established by running a shared test page through each platform's real layout engine —
Robolectric on Android, the simulator on iOS. All three suites green: Android 521 tests,
iOS 504, js unchanged.

### Fixed

**Spacers were sized in `.px`, which is not the unit a view reports back.** Web and Android
agree; iOS does not, because a frame is in points while `.px` converts through the screen
scale. Every spacer would have been short by 2–3× on retina while looking perfect elsewhere.
Added `Double.viewUnits` as the inverse of `Dimension.viewUnits` — the one conversion that
lines up with measurement everywhere — and dropped the rounding, which was adding error
rather than preventing drift (spacer sizes are recomputed from the prefix sum, never
accumulated).

**Android's `viewport` was only invalidated by scrolling, never by layout.** A list learned
its size as zero and stayed on the fixed `ssrItemCount` window until the user scrolled. Web
gets the size half of the rectangle from a ResizeObserver; Android now gets it from a global
layout listener filtered to actual size changes. This affects everything reading `viewport`,
not just WindowedList.

**The correction pass did not run on mobile — now fixed by measuring on demand.** The list read
a cell's size in the same pass that materialised it, which only works where reading a size
forces a layout. Android reported zero for a view added since the last layout pass; iOS landed
on timing, passing alone and failing in the full suite.

`measureExtent(vertical, crossAxis)` asks an element how much main-axis extent it wants,
without laying it out. All three implementations already existed inside `ProgrammaticLayout`,
where Recycler2 has been using them: `View.measure` on Android, `sizeThatFits2` on iOS,
`measureByDuplicate` on the web.

The pass prefers the **real laid-out size** and falls back to a measure only for a cell not yet
laid out. The two answer different questions — how big it wants to be, versus how big its
parent made it — and diverge wherever a parent stretches or weights a child. So the browser is
unchanged, and a cell whose parent overrode its preference reconciles on a later pass instead
of staying wrong.

Rejected alternative: an `afterNextLayout` hook running the correction asynchronously. More
accurate in principle, but it costs a frame — the frame where cells appear shows estimate-based
spacers, and scroll compensation lands late, arriving while the user may have scrolled further.
It also needs re-entrancy bounding, since correcting resizes spacers and triggers another
layout.

### Open

**Mobile measurement cost is unbenchmarked.** Each cell now takes a real measure on the pass it
appears. Recycler2 already pays this on every platform, and the web path is unchanged, but no
mobile equivalent of the js pooling benchmark exists yet.

### Test-harness limits worth knowing

**iOS unit tests cannot see a scale bug.** `UIScreen` in the Kotlin/Native test runner is a
stub: bounds 0×0, `nativeScale` NaN, `scale` 1.0. Points and pixels coincide, so a `.px`
regression passes. This is why the bug above survived. `makeKeyAndVisible()` also crashes the
runner (SIGTRAP).

**Android density is a suite-wide setting, not a per-test one.** `AndroidAppContext.density`
is a `lazy` on an object, so the first test to read it fixes it for the whole JVM and a
per-class `@Config(qualifiers = ...)` silently poisons every later test. The suite moved from
mdpi to xxhdpi: at density 1 `dp` and device pixels coincide and unit mistakes measure
correct.

**Every assertion here needs a mutation check.** The first version of the WindowedList test
passed while asserting nothing — 20 fallback items sat inside a tolerance meant to allow a
different overdraw. Counts are now checked against the fallback explicitly and first.

### Unrelated inconsistency found

`frame` stretches its children over an explicit height on iOS but honours it on Android
(`sizedBox(height = 400).frame {}` inside a `frame` measures 1000 on iOS, 400 on Android).
Not a WindowedList problem, but a real cross-platform difference; the test page uses `col`.
