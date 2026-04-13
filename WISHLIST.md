# KiteUI Wishlist

- [ ] Create a property to set the default align/gravity in containers (col, row, frame, all of the views using acting like a frame like button...)

---

## Nested Corner Radius Calculation

**Problem:** The design rule is: inner radius = outer radius − padding between them. KiteUI has no parent→child radius information flow. `RatioOfSpacing` and `AdaptiveToSpacing` approximate this incidentally but don't compute the geometric relationship.

**Goal:** If a card has `cornerRadii = 12px` and `padding = 8px`, its children should automatically get `cornerRadii = 4px`.

### Implementation Ideas

**Recommended: Add `CornerRadii.Nested` variant** — means "compute my radius as parent's resolved radius minus the padding between us."

```kotlin
data class Nested(val fallback: Dimension = 0.px) : CornerRadii()
```

Resolution: `max(0, parentResolvedRadius - parentPadding)`. Clamping to 0 gives square inner corners when padding exceeds outer radius — correct behavior.

**Existing infrastructure:** Each platform already has `spacingForChildCornerRadii` (deprecated) which computes `min(padding, gap)` and passes it to children. This proves the parent→child propagation pattern works:

- **Web:** `--parentSpacing` CSS variable is already set on children during `nativeApplyTheme`. Add a `--parentCornerRadius` CSS variable alongside it. Resolution becomes `calc(max(0px, var(--parentCornerRadius, 0px) - var(--parentSpacing, 0px)))`. Works cleanly for `Fixed` and `AdaptiveToSpacing` parents. `RatioOfSize` parents (rare) would need JS fallback.
- **Android/iOS:** Add `resolvedCornerRadius: Double` property to `NativeElement`, set during `updateCorners()` / `refreshCorners()`. Children read `parent?.resolvedCornerRadius` and subtract parent's `appliedPadding`.

**Edge cases:**
- *Multiple nesting levels:* Each level computes from its direct parent — works recursively since each parent stores its own resolved radius.
- *Non-uniform corners (PerCorner):* Simplest approach: `Nested` resolves from the parent's minimum corner radius. A `PerCornerNested` variant could follow later.
- *`spacingForChildCornerRadii` deprecation:* `Nested` replaces its use case more correctly. The existing `RatioOfSpacing`/`AdaptiveToSpacing` adapt to spacing but don't actually implement the geometric "inner = outer - padding" rule.

**Why not compute at theme derivation time?** Parent's radius may itself be spacing-dependent or size-dependent and not yet resolved. Runtime resolution is necessary.

---

## Corner Shape / Squircle Support

**Problem:** `CornerRadii` only controls radius magnitude. There's no way to express corner curvature — circular arc vs iOS-style continuous curve (squircle/superellipse). iOS gets this for free with `CALayer.cornerCurve = .continuous`, but KiteUI can't express it.

**Goal:** Theme authors can specify corner smoothing. Platform renderers apply where possible, degrade gracefully elsewhere.

### Implementation Ideas

**Recommended: Add `cornerShape: CornerShape` enum to `Theme`** — separate from `cornerRadii` because shape is orthogonal to radius size/strategy. Default `Circular` (no behavior change).

```kotlin
enum class CornerShape {
    Circular,    // Standard circular arc (CSS border-radius, default everywhere)
    Continuous   // iOS-style superellipse/squircle
}
```

**Per-platform rendering:**

- **iOS:** `Circular` → default. `Continuous` → `layer.cornerCurve = kCACornerCurveContinuous` (one line, native API since iOS 13, below KiteUI's 14.0 target, zero performance cost). For `PerCorner` paths using `CAShapeLayer`, generate a superellipse `UIBezierPath` instead.
- **Android:** No native API. `Continuous` → generate a `Path` using superellipse formula `|x/a|^n + |y/b|^n = 1` with `n ≈ 5`. Use in a custom `Drawable` with `canvas.drawPath()` + `ViewOutlineProvider.setPath()`. Path computed only on layout, not per-frame.
- **Web:** `Continuous` → use SVG `clip-path` with a generated superellipse polygon (~64 points). Apply via `clip-path: url(#id)`. The emerging CSS `corner-shape: squircle` could be progressive enhancement later. SVG clip-paths are GPU-composited.

**Why an enum, not a Float:** iOS only supports binary `.circular` / `.continuous` — no intermediate values. A float implies a smooth slider but in practice you'd get the native fast path at 0 and 1, with expensive custom path rendering for everything in between for minimal visual difference. An enum is honest about what the platforms support and avoids the false precision.

**Why on Theme, not CornerRadii:** Keeps the sealed class clean — avoids duplicating the parameter across all five variants. Shape is typically set once globally and inherited, matching how designers apply it.

**Graceful degradation:** When `Continuous` is unsupported (e.g., older Android custom views), fall back to `Circular`. The visual difference is subtle.

---

## Multi-Layer / Composite Backgrounds

**Problem:** `background: Paint` is a single value. You can't composite a gradient over a color, or layer decorative elements (artistic backgrounds — circles, arcs). CSS supports multiple `background-image` layers natively; Android/iOS can layer drawables/sublayers.

**Goal:** Theme authors can express layered backgrounds (e.g., radial accent blob + solid base color) without custom rendering.

### Implementation Ideas

**Recommended: Add `Composite` Paint variant** — a new case in the sealed hierarchy, not a change to Theme's API.

```kotlin
data class Composite(val layers: List<Paint>) : Paint
```

**Why this over alternatives:**
- **`List<Paint>` on Theme** (Option A): Massively breaking — 41+ `theme.background` accesses, 68 `when` branches, every theme factory. Not worth it.
- **Separate `backgroundLayers` property** (Option C): Adds ambiguity about which field to use, bloats Theme's already-26-parameter constructor.
- **`Composite` variant** (Option B): Near-zero breaking changes. Only exhaustive `when` expressions on `Paint` break — which is *desirable* since it forces platform renderers to handle the new case. Theme API surface unchanged.

**Paint utilities compose naturally:**
- `closestColor()`: Return the top (last) opaque-ish layer's `closestColor()`, or blend.
- `map()`: `Composite(layers.map { it.map(mapper) })`.
- Theme derivation: `copy(background = Composite(...))` just works.

**Platform rendering:**
- **Web:** Emit multiple comma-separated `background-image` values (CSS supports this natively).
- **Android:** Wrap in `LayerDrawable` for stacking multiple drawables.
- **iOS:** Add multiple `CAGradientLayer` sublayers.

**Backward compatible:** Existing single-paint themes are unaffected. Composite backgrounds are opt-in.

---

## Entry/Exit Animations via shownWhen

**Problem:** `shownWhen` currently shows/hides elements with hardcoded platform-specific animations (collapse in rows/columns, fade in stacks). There's no way for developers to choose the animation style.

**Goal:** Elements animated by `shownWhen` can fade/slide/scale in and out, with the animation style chosen per-use.

### Implementation Ideas

Two coupled changes: redesign `ScreenTransition` to be a common data class, then add it as a parameter to `shownWhen`.

#### Step 1: Redesign `ScreenTransition` as a Common Data Class

Currently `ScreenTransition` is `expect/actual` with completely different shapes per platform:
- **Android:** `enter: () -> Transition?, exit: () -> Transition?` (AndroidX Transition objects)
- **iOS:** `enter: UIView.()->Unit, exit: UIView.()->Unit` (CGAffineTransform lambdas)
- **Web:** `enter: ScreenTransitionPart, exit: ScreenTransitionPart` (CSS keyframe maps)

This makes it impossible to define custom transitions in common code. Replace with a common data class:

```kotlin
data class ScreenTransition(
    val entryTransform: Transformation = Transformation(),  // start state for entering element
    val exitTransform: Transformation = Transformation(),   // end state for exiting element
    val fade: Boolean = false,                              // whether opacity animates 0↔1
    val easing: Easing = Easing.DecelerateIn,               // easing function
) {
    companion object {
        val None = ScreenTransition()
        val Fade = ScreenTransition(fade = true)
        val Push = ScreenTransition(
            entryTransform = Transformation(translationX = 1.0),  // 100% from right
            exitTransform = Transformation(translationX = -1.0),  // 100% to left
        )
        val GrowFade = ScreenTransition(
            entryTransform = Transformation(scaleX = 0.75, scaleY = 0.75),
            exitTransform = Transformation(scaleX = 1.33, scaleY = 1.33),
            fade = true,
        )
        val Collapse = ScreenTransition()  // special-cased by shownWhen for layout collapse
        // ... etc
    }
}

enum class Easing {
    Linear,
    DecelerateIn,   // fast start, slow end (ease-out)
    AccelerateOut,   // slow start, fast end (ease-in)
    Standard,        // ease-in-out
}
```

`Transformation` already exists in KiteUI with `translationX/Y/Z`, `rotationX/Y`, `rotation`, `scaleX/Y`. The `entryTransform` is the starting state of a newly-appearing element (it animates *from* this to identity). The `exitTransform` is the ending state of a disappearing element (it animates *to* this from identity).

**Platform mapping is mechanical:**
- **Web:** `entryTransform`/`exitTransform` → CSS `transform` keyframes. `fade` → opacity keyframes. `easing` → CSS `animation-timing-function`.
- **Android:** `entryTransform`/`exitTransform` → `ViewPropertyAnimator` calls (translationX, scaleX, alpha, etc.). `easing` → `Interpolator`.
- **iOS:** `entryTransform`/`exitTransform` → `CGAffineTransform`. `fade` → alpha. `easing` → `UIView.AnimationOptions`.

Translation values would be relative (1.0 = 100% of container width/height), matching how iOS and web already handle Push/Pop.

#### Step 2: Add `transition` Parameter to `shownWhen`

```kotlin
fun ElementWriter.CanAddShownWhen.shownWhen(
    default: Boolean = false,
    transition: ScreenTransition = ScreenTransition.Collapse,
    condition: ReactiveContext.() -> Boolean
): ElementWriter.CanAddSizing
```

**Why a parameter, not a theme property or separate modifier:**
- Theme property is too global — different elements in the same page need different animations.
- Separate modifier breaks the ordering chain with no benefit.
- A parameter on `shownWhen` keeps the API local and explicit.

**`Collapse` is special-cased:** It triggers the current behavior (animate height/width to 0, shrink out of layout flow). All other transitions keep the element in layout flow and only animate visual properties, setting `hidden` after the exit animation completes.

**Exit handling already solved:** Each platform already keeps the element alive during exit animation and sets `hidden` on completion. This stays the same.

**Current platform animation infrastructure:**
- **Web:** Web Animations API with batching system (`combinedAnimationWorker`). Already supports arbitrary CSS keyframe animations.
- **Android:** `ValueAnimator` in `SimplifiedLinearLayout`. Other containers fade alpha.
- **iOS:** `UIView.animateWithDuration` with transform + alpha.

**Effort:** The `ScreenTransition` redesign is the bigger change — it touches SwapView on all platforms plus overlays/dialogs. But it makes the whole transition system more expressive and composable, benefiting more than just `shownWhen`.
