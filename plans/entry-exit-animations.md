# Entry/Exit Animations via `shownWhen`

## Problem

`shownWhen` currently shows/hides elements with hardcoded platform-specific animations:
- **JS/Web:** Collapse (width/height/margin/padding to zero via Web Animations API)
- **Android:** Collapse in `SimplifiedLinearLayout` (ValueAnimator on width/height), fade in other containers
- **iOS:** Fade alpha + `extensionCollapsed` layout hint

No way for developers to choose animation style per-use. Every `shownWhen` gets the same collapse behavior.

## Goal

Elements animated by `shownWhen` can have a visual effect (fade, slide, scale) layered on top of the automatic layout animation. Layout collapse in rows/cols always happens — the `ScreenTransition` parameter controls only the visual effect.

## Current State

### `shownWhen` API

```kotlin
// commonMain/.../modifiers.kt:168
expect fun ElementWriter.CanAddShownWhen.shownWhen(
    default: Boolean = false,
    condition: ReactiveContext.() -> Boolean
): ElementWriter.CanAddSizing
```

### `ScreenTransition` — expect/actual with incompatible shapes

| Platform | Shape | File |
|----------|-------|------|
| **Common** | `expect class` with companion presets | `commonMain/.../ScreenTransitions.kt:3-14` |
| **Android** | `(name, enter: () -> Transition?, exit: () -> Transition?)` | `androidMain/.../ScreenTransitions.android.kt:12-68` |
| **iOS** | `(name, enter: UIView.()->Unit, exit: UIView.()->Unit)` | `iosMain/.../data.kt:80-143` |
| **HTML** | `(name, enter: ScreenTransitionPart, exit: ScreenTransitionPart)` | `commonHtmlMain/.../data.commonHtml.kt:121-200` |
| **Swing** | Empty private constructor (no animation) | `library-swing/.../ScreenTransitions.jvmSwing.kt:3-14` |

Custom transitions cannot be defined in common code. Each platform speaks a completely different language.

### `Transformation` — already exists in common

```kotlin
// commonMain/.../Theme.kt:1184-1193
data class Transformation(
    val translationX: Double = 0.0,  // relative units
    val translationY: Double = 0.0,
    val translationZ: Double = 0.0,
    val rotationX: Double = 0.0,
    val rotationY: Double = 0.0,
    val rotation: Double = 0.0,
    val scaleX: Double = 1.0,
    val scaleY: Double = 1.0,
)
```

Used for theme `transform` property. Platform renderers already know how to apply it.

### Animation infrastructure per platform

| Platform | Mechanism | Duration source |
|----------|-----------|-----------------|
| **JS** | Web Animations API, batched via `combinedAnimationWorker` (32ms schedule) | `theme.transitionDuration` |
| **Android** | `ValueAnimator` for collapse, `ViewPropertyAnimator` for alpha | `theme.transitionDuration.inWholeMilliseconds` |
| **iOS** | `UIView.animateWithDuration` with `CGAffineTransform` | `theme.transitionDuration.toDouble(SECONDS)` |
| **JVM SSR** | No-op | N/A |
| **Swing** | `BufferedImage` capture + `Timer` loop | `theme.transitionDuration.inWholeMilliseconds` |

### Scope: 185 occurrences of `ScreenTransition` across 33 files

---

## Design

Two coupled changes: (1) redesign `ScreenTransition` as a common data class, (2) add a `transition` parameter to `shownWhen`.

### Step 1: `ScreenTransition` as Common Data Class

Replace `expect/actual class` with a common `data class`. The `Transformation` type already exists and platforms already know how to render it.

```kotlin
// commonMain/.../models/Easing.kt
data class Easing(val x1: Float, val y1: Float, val x2: Float, val y2: Float) {
    companion object {
        val Linear = Easing(0f, 0f, 1f, 1f)
        val EaseOut = Easing(0f, 0f, 0.58f, 1f)         // fast start, slow end
        val EaseIn = Easing(0.42f, 0f, 1f, 1f)          // slow start, fast end
        val EaseInOut = Easing(0.42f, 0f, 0.58f, 1f)    // standard
        val Spring = Easing(0.175f, 0.885f, 0.32f, 1.275f)  // slight overshoot
    }
}

// commonMain/.../ScreenTransitions.kt
data class ScreenTransition(
    val name: String,
    val entryTransform: Transformation = Transformation(),
    val exitTransform: Transformation = Transformation(),
    val fade: Boolean = false,
    val easing: Easing = Easing.EaseInOut,
) {
    companion object {
        val None = ScreenTransition("None")
        val Fade = ScreenTransition("Fade", fade = true)
        val Push = ScreenTransition(
            "Push",
            entryTransform = Transformation(translationX = 1.0),
            exitTransform = Transformation(translationX = -1.0),
        )
        val Pop = ScreenTransition(
            "Pop",
            entryTransform = Transformation(translationX = -1.0),
            exitTransform = Transformation(translationX = 1.0),
        )
        val PullUp = ScreenTransition(
            "PullUp",
            entryTransform = Transformation(translationY = 1.0),
            exitTransform = Transformation(translationY = -1.0),
        )
        val PullDown = ScreenTransition(
            "PullDown",
            entryTransform = Transformation(translationY = -1.0),
            exitTransform = Transformation(translationY = 1.0),
        )
        val GrowFade = ScreenTransition(
            "GrowFade",
            entryTransform = Transformation(scaleX = 0.75, scaleY = 0.75),
            exitTransform = Transformation(scaleX = 1.33, scaleY = 1.33),
            fade = true,
        )
        val ShrinkFade = ScreenTransition(
            "ShrinkFade",
            entryTransform = Transformation(scaleX = 1.33, scaleY = 1.33),
            exitTransform = Transformation(scaleX = 0.75, scaleY = 0.75),
            fade = true,
        )
    }
}
```

`Easing` is a cubic bezier defined by four control points. This maps 1:1 to every platform's native API:

| Platform | Mapping |
|----------|---------|
| **Web/CSS** | `"cubic-bezier(${x1}, ${y1}, ${x2}, ${y2})"` — direct string interpolation into `animation-timing-function` |
| **Android** | `PathInterpolator(x1, y1, x2, y2)` — single constructor call (API 21+) |
| **iOS** | `CAMediaTimingFunction(controlPoints: x1, y1, x2, y2)` — single constructor call |
| **Swing** | Evaluate bezier curve at `t` per frame — ~10 lines of standard math |

Users get near-full control — any standard easing curve (CSS ease, Material curves, etc.) plus custom ones:

```kotlin
// Custom easing in common code
val SnappyBounce = Easing(0.68f, -0.55f, 0.265f, 1.55f)

val MyTransition = ScreenTransition(
    name = "MyTransition",
    entryTransform = Transformation(translationY = 1.0),
    fade = true,
    easing = SnappyBounce,
)
```

**Limitation:** Cubic bezier cannot express multi-segment curves or true spring physics with bounce. Covers ~95% of real-world easing needs.

**Semantics:**
- `entryTransform` = starting state of an appearing element (animates *from* this to identity)
- `exitTransform` = ending state of a disappearing element (animates *to* this from identity)
- Translation values are relative: 1.0 = 100% of container dimension
- No `Collapse` sentinel — layout animation is automatic based on container type (see Step 2)

**`ScreenTransitions` (plural) stays unchanged** — it's already a common data class wrapping forward/reverse/neutral.

### Step 2: Add `transition` Parameter to `shownWhen`

```kotlin
// commonMain/.../modifiers.kt
expect fun ElementWriter.CanAddShownWhen.shownWhen(
    default: Boolean = false,
    transition: ScreenTransition = ScreenTransition.None,
    condition: ReactiveContext.() -> Boolean,
): ElementWriter.CanAddSizing
```

Default `None` preserves current behavior — layout collapse with no visual effect, fully backward compatible.

**Two-layer animation model:**

1. **Layout animation (automatic):** Determined by container type, always runs.
   - **Row/Col:** Animate width/height/margin/padding to/from zero (current collapse behavior)
   - **Frame/other:** No layout animation (overlaid elements don't affect siblings)

2. **Visual animation (from `transition` parameter):** Layered on top of layout animation.
   - `ScreenTransition.None` → no visual effect (just the layout collapse)
   - `ScreenTransition.Fade` → fade opacity in/out while space collapses
   - `ScreenTransition.Push` → slide + space collapses
   - Custom → any transform/fade combo + space collapses

This eliminates special cases. Every `shownWhen` call gets layout animation appropriate to its container, plus whatever visual effect is specified.

---

## Implementation Plan

### Phase 1: Common Data Class Conversion

**Goal:** Replace `expect/actual ScreenTransition` with common data class. All existing behavior preserved.

#### 1.1 Create `Easing` data class

**File:** `library/src/commonMain/.../models/Easing.kt` (new)

```kotlin
data class Easing(val x1: Float, val y1: Float, val x2: Float, val y2: Float) {
    companion object {
        val Linear = Easing(0f, 0f, 1f, 1f)
        val EaseOut = Easing(0f, 0f, 0.58f, 1f)
        val EaseIn = Easing(0.42f, 0f, 1f, 1f)
        val EaseInOut = Easing(0.42f, 0f, 0.58f, 1f)
        val Spring = Easing(0.175f, 0.885f, 0.32f, 1.275f)
    }
}
```

#### 1.2 Rewrite `ScreenTransitions.kt`

**File:** `library/src/commonMain/.../models/ScreenTransitions.kt`

- Remove `expect class ScreenTransition`
- Add `data class ScreenTransition(...)` with companion presets (as shown above)
- Keep `ScreenTransitions` (plural) unchanged

#### 1.3 Delete actual implementations

Remove or gut these files — they become unnecessary:

| File | Action |
|------|--------|
| `androidMain/.../ScreenTransitions.android.kt` | Delete |
| `iosMain/.../data.kt` lines 80-143 | Remove `ScreenTransition` class (keep other actuals in file) |
| `commonHtmlMain/.../data.commonHtml.kt` lines 114-200 | Remove `ScreenTransitionPart` and `ScreenTransition` class |
| `library-swing/.../ScreenTransitions.jvmSwing.kt` | Delete |

#### 1.4 Platform `animateIn`/`animateOut` — rewrite to use common data

Each platform's `animateIn`/`animateOut` must interpret `ScreenTransition` fields:

**JS/HTML** (`jsMain/.../animations.js.kt`):
- Convert `entryTransform`/`exitTransform` → CSS `transform` string
- `fade` → opacity keyframes
- `easing` → `"cubic-bezier(${x1}, ${y1}, ${x2}, ${y2})"` as `animation-timing-function`
- Generate CSS keyframes dynamically (similar to current `KiteUiCss.transition()`)

**Android** (`androidMain/.../animations.android.kt`):
- Currently no-op. Implement using `ViewPropertyAnimator`:
  - `translationX/Y` → `View.translationX/Y` (multiply by container width/height)
  - `scaleX/Y` → `View.scaleX/Y`
  - `rotation` → `View.rotation`
  - `fade` → `View.alpha`
  - `easing` → `PathInterpolator(x1, y1, x2, y2)` (API 21+)

**iOS** (`iosMain/.../animations.ios.kt`):
- Convert `entryTransform`/`exitTransform` → `CGAffineTransform`
- `fade` → alpha animation
- `easing` → `CAMediaTimingFunction(controlPoints: x1, y1, x2, y2)` (or use `UIView.animate` with `CATransaction` timing override)

**Swing** (`library-swing/.../animations.jvmSwing.kt`):
- Currently no-op. Optionally implement using existing `AnimationPanel` pattern from `SwapView.jvmSwing.kt`
- `easing` → evaluate cubic bezier at `t` per frame (replace current hardcoded `1 - pow(1-t, 3)` in SwapView). Standard De Casteljau algorithm, ~10 lines.

**JVM SSR** (`jvmSsrMain/.../animations.jvm.kt`):
- Stays no-op

#### 1.5 Update `SwapView` on all platforms

`SwapView.swap()` calls `animateIn`/`animateOut`. After 1.4, these now speak common `ScreenTransition`, so `SwapView` should work without changes beyond removing any platform-specific `ScreenTransition` field access.

Platform-specific adjustments:

**Android** (`SwapView.android.kt`):
- Currently uses `transition.enter()` and `transition.exit()` to get `Transition` objects
- Rewrite to call `animateIn`/`animateOut` (which now use `ViewPropertyAnimator`)
- Or: build `TransitionSet` from common `ScreenTransition` fields directly

**iOS** (`SwapView.ios.kt`):
- Already calls `animateIn`/`animateOut` — should work after 1.4

**HTML/JS** (`SwapView.commonHtml.js.kt`):
- Currently uses `context.kiteUiCss.transition(transition)` to get CSS keyframe names
- Rewrite `KiteUiCss.transition()` to generate keyframes from common fields
- Or: inline keyframe generation in `animateIn`/`animateOut`

**Swing** (`SwapView.jvmSwing.kt`):
- Uses custom `AnimationType` enum mapped from `ScreenTransition` identity
- Rewrite to derive animation parameters from common fields

#### 1.6 Update `KiteUiCss.transition()`

**File:** `commonHtmlMain/.../KiteUiCss.kt:777-795`

Rewrite to generate CSS keyframes from `ScreenTransition.entryTransform`/`exitTransform`/`fade` instead of from `ScreenTransitionPart`.

```kotlin
fun transition(transition: ScreenTransition): String {
    if (!transitionHandled.add(transition.name)) return "transition-${transition.name}"
    
    fun buildKeyframe(transform: Transformation, fade: Boolean, isEntry: Boolean): String {
        val parts = mutableListOf<String>()
        // Build CSS transform string from Transformation fields
        // Build opacity from fade flag
        // ...
        return parts.joinToString("; ")
    }
    
    // Generate @keyframes rules
    // ...
    return "transition-${transition.name}"
}
```

#### 1.7 Update remaining references

Other files referencing `ScreenTransition`:

| File | Usage | Impact |
|------|-------|--------|
| `ViewContextExtensions.kt` (common + actuals) | `defaultScreenTransition` property | Signature unchanged, type unchanged |
| `popoverHelpers.kt` | Uses `ScreenTransition.GrowFade` | No change needed |
| `BottomSheet.commonHtml.kt` | Uses `ScreenTransition` for overlay animation | May need minor adjustments |
| `CoordinatorFrame.commonHtml.kt` | Uses `ScreenTransition` | May need minor adjustments |
| `deprecated.kt` | References `ScreenTransition` | Update if needed |
| `example-app/App.kt` | Uses `ScreenTransitions` presets | No change needed |
| `example-app/SwapViewPage.kt` | Demonstrates transitions | Update to new API |

### Phase 2: `shownWhen` Transition Parameter

**Goal:** Add `transition` parameter, implement per-platform.

#### 2.1 Update common signature

**File:** `library/src/commonMain/.../modifiers.kt:168`

```kotlin
expect fun ElementWriter.CanAddShownWhen.shownWhen(
    default: Boolean = false,
    transition: ScreenTransition = ScreenTransition.None,
    condition: ReactiveContext.() -> Boolean,
): ElementWriter.CanAddSizing
```

#### 2.2 HTML/JS implementation

**File:** `commonHtmlMain/.../modifiers.commonHtml.kt:191-234`

Pass `transition` into the animation worker. The existing `nativeAnimateShow`/`nativeAnimateHide` path already handles layout collapse — extend it to also apply the visual transition.

```kotlin
actual fun ElementWriter.CanAddShownWhen.shownWhen(
    default: Boolean,
    transition: ScreenTransition,
    condition: ReactiveContext.() -> Boolean,
): ElementWriter.CanAddSizing {
    return lazyInjectModifierWriter {
        object : NativeContainerElement(context) {
            init {
                // ... existing wrapper setup ...
                reactive {
                    if (areAnimationsEnabled && fullyStarted) {
                        val c = condition()
                        if (c != currentState) {
                            if (c) nativeAnimateShow(transition) else nativeAnimateHide(transition)
                        }
                        currentState = c
                    } else {
                        // No animation path unchanged
                    }
                }
            }
            // ...
        }
    }
}
```

**File:** `jsMain/.../modifiers.commonHtml.js.kt`

Extend `nativeAnimateShow`/`nativeAnimateHide` signatures to accept `ScreenTransition`. In the `combinedAnimationWorker`:

- **Layout keyframes** (width/height/margin/padding collapse) — generated as before, based on container type (row vs col vs frame)
- **Visual keyframes** (transform, opacity) — generated from `transition.entryTransform`/`exitTransform`/`fade`
- Both sets of keyframes merged into the same `OngoingAnimation` and run simultaneously
- `easing` applied to the Web Animation via `"cubic-bezier(${x1}, ${y1}, ${x2}, ${y2})"`

For frame containers, layout keyframes are empty — only visual keyframes play.

#### 2.3 Android implementation

**File:** `androidMain/.../modifiers.android.kt:391-451`

Same structure as current, but layer visual animation on top:

```kotlin
actual fun ElementWriter.CanAddShownWhen.shownWhen(
    default: Boolean,
    transition: ScreenTransition,
    condition: ReactiveContext.() -> Boolean,
): ElementWriter.CanAddSizing {
    return beforeSetup {
        shown = default
        var existingAnimator: ValueAnimator? = null
        var goal = default
        reactive {
            val value = condition()
            if (goal == value) return@reactive
            goal = value
            existingAnimator?.cancel()
            existingAnimator = null
            
            if (!animationsEnabled || native.layoutParams == null) {
                shown = value
                return@reactive
            }
            
            val parent = parent
            val p = parent?.native
            shown = true

            // Layer 1: Layout animation (automatic, based on container)
            existingAnimator = if (p is SimplifiedLinearLayout) {
                // Current collapse logic (width/height animator)
                // ...
            } else {
                // Frame/other: no layout animation, just duration timer
                // ...
            }
            
            // Layer 2: Visual animation (from transition parameter)
            if (transition != ScreenTransition.None) {
                if (value) animateIn(transition) else animateOut(transition)
            }
            
            existingAnimator?.doOnEnd { shown = value }
            existingAnimator?.start()
        }
    }
}
```

Layout and visual animations run in parallel. Layout handles the space, visual handles the look.

#### 2.4 iOS implementation

**File:** `iosMain/.../modifiers.ios.kt:138-182`

Same two-layer approach:
- `extensionCollapsed` + `informParentOfSizeChange()` handles layout collapse (current behavior)
- `CGAffineTransform` from `transition.entryTransform`/`exitTransform` handles visual effect
- Both run inside the same `animateIfAllowed` block
- `easing` applied via `CATransaction.setAnimationTimingFunction(CAMediaTimingFunction(controlPoints: ...))`

#### 2.5 JVM SSR / Swing implementations

- **JVM SSR:** No animation, just set `shown` directly. Pass `transition` through signature.
- **Swing:** Layout collapse via existing size animation. Visual transition via `Transformation` fields applied to the `AnimationPanel` rendering. Easing via cubic bezier evaluation per frame.

### Phase 3: Testing & Validation

#### 3.1 Unit tests

- Verify `shownWhen` with default `None` still behaves identically to current (layout collapse only)
- Verify `shownWhen` with `Fade`, `Push`, custom `ScreenTransition` in both row/col and frame containers
- Test rapid state toggling (animation cancellation mid-flight)
- Test `default = false` + visual transition (element starts hidden)

#### 3.2 Visual testing

- Example app page demonstrating each transition type with `shownWhen`
- Side-by-side: same content with different transitions
- Test in row container (visual + space collapse), column container (visual + space collapse), and frame container (visual only)
- Verify layout collapse + visual transition run smoothly in parallel

#### 3.3 Platform-specific testing

- **JS:** Verify Web Animations API keyframes generate correctly
- **Android:** Verify `ViewPropertyAnimator` handles all transform fields
- **iOS:** Verify `CGAffineTransform` composition is correct
- **Accessibility:** Verify `prefers-reduced-motion` / `UIAccessibilityIsReduceMotionEnabled` respected

---

## Edge Cases & Decisions

### Two-layer animation model

Layout animation and visual animation are independent concerns:

- **Layout animation** is automatic. Rows/cols always collapse the space smoothly. Frames don't animate layout (overlaid elements don't affect siblings). This matches current behavior.
- **Visual animation** is the `ScreenTransition` parameter. It layers on top of whatever layout animation is happening. `None` means just the layout collapse. `Fade` means fade + collapse. `Push` means slide + collapse.

Both run in parallel with the same duration and easing. The visual effect doesn't change when the layout does — a fade looks the same whether the element is in a row (space collapses) or a frame (space doesn't change).

### Translation units

`Transformation.translationX/Y` values are relative (1.0 = 100% of container). This matches iOS (already uses `superview.bounds`) and web (already uses `translateX(100%)`). Android will need to multiply by container dimensions.

### Custom transitions

Users can create arbitrary transitions in common code:

```kotlin
val SlideFromLeft = ScreenTransition(
    name = "SlideFromLeft",
    entryTransform = Transformation(translationX = -1.0),
    exitTransform = Transformation(translationX = 1.0),
    fade = true,
    easing = Easing.EaseOut,
)

// Usage — in a col, this fades + slides while the space collapses
// In a frame, this fades + slides with no layout change
shownWhen(transition = SlideFromLeft) { someCondition() }
```

### `ScreenTransition.name` requirement

Names must be unique per distinct transition (used as CSS keyframe identifiers on web). Companion presets have fixed names. User-created transitions need unique names — collisions silently reuse cached keyframes (which is fine if the transitions are identical, wrong if not).

**Consider:** Generate name from field hash to avoid collision risk. Trade-off: less readable in dev tools.

### Accessibility / reduced motion

All platforms already gate animations on `animationsEnabled` / `UIAccessibilityIsReduceMotionEnabled`. Non-collapse transitions should respect the same gates — when animations disabled, immediately show/hide without any visual transition.

---

## Risk Assessment

| Risk | Severity | Mitigation |
|------|----------|------------|
| Breaking `SwapView` on some platform during ScreenTransition rewrite | High | Phase 1 is the riskiest. Test SwapView on all platforms before moving to Phase 2. |
| CSS keyframe name collisions for custom transitions | Low | Hash-based names, or document uniqueness requirement |
| Visual + layout animation timing mismatch | Medium | Both layers use same `theme.transitionDuration` and `easing`. If visual transition finishes before layout collapse (or vice versa), one layer snaps. Using identical timing avoids this. |
| Android `animateIn`/`animateOut` currently no-op; implementing is net-new work | Medium | Straightforward with `ViewPropertyAnimator`. Can ship iOS/JS first. |
| `ScreenTransitionPart` used in non-`ScreenTransition` contexts | Low | Check all usages during 1.3 — only found in `ScreenTransition` companion and `KiteUiCss.transition()` |

## Effort Estimate

| Phase | Scope | Dependencies |
|-------|-------|-------------|
| **Phase 1** | ~15 files touched. Biggest change is Android `SwapView` + JS `KiteUiCss.transition()`. | None |
| **Phase 2** | ~5 files touched (one per platform). Extend existing animation paths to layer visual transition on top of layout collapse. | Phase 1 |
| **Phase 3** | Example app page + manual testing on each platform. | Phase 2 |

Phase 1 is the heavy lift. Phase 2 is small once Phase 1 is solid.
