# KiteUI View-Split Migration

This document describes the major architectural refactoring happening on the `view-split` branch.

**Status**: ✅ COMPLETE - Migration has landed on `version-8`; the codebase builds.

## Overview

The view-split migration is a major architectural refactoring that improves:
1. **Code clarity** - Splitting monolithic classes into focused interfaces
2. **Type safety** - Compile-time enforcement of modifier ordering
3. **Separation of concerns** - Clear boundaries between Element interface, native implementations, and DSL builders

## Major Changes

### 1. RView → Element + NativeElement Split

The old monolithic `RView` class is being split into cleaner abstractions:

#### Old System (version-7)
```kotlin
expect abstract class RView(context: RContext) : RViewHelper {
    // Mix of interface, platform-specific code, and lifecycle management
    override fun applyTheme(theme: ThemeAndBack)
    override fun internalAddChild(index: Int, view: RView)
    // ... many more mixed concerns
}
```

#### New System (view-split)
```kotlin
// Pure interface - what all UI elements must provide
interface Element : KiteUiCoroutineScopeHelpers, StatusListener {
    val context: ElementContext
    val underlyingNativeElement: NativeElement
    val parent: ContainerElement?

    fun onStartup()
    fun onShutdown()

    var opacity: Double
    var shown: Boolean
    // ... other element properties
}

// Platform-specific native view wrapper
expect abstract class NativeElement(context: ElementContext) : Element, NativeElementCommonCode {
    override var opacity: Double
    override var shown: Boolean
    override fun scrollIntoView(...)
    // ... platform-specific rendering
}

// Shared cross-platform logic
abstract class NativeElementCommonCode internal constructor(context: ElementContext) : Element {
    // Lifecycle management
    // Theme propagation
    // Reactive processes
    // Common debugging/logging
}

// Elements that can contain children
interface ContainerElement : Element, ViewWriter {
    override val underlyingNativeElement: NativeContainerElement
    val children: List<Element>
    fun addChild(index: Int, element: Element)
    fun removeChild(index: Int)
    fun clearChildren()
}

expect abstract class NativeContainerElement(context: ElementContext) :
    ContainerElement, NativeContainerElementCommonCode {
    override fun nativeAddChild(index: Int, element: Element)
    override fun nativeRemoveChild(index: Int)
    override fun nativeClearChildren()
}
```

**Benefits:**
- `Element` is a pure interface defining what all UI elements provide
- `NativeElement` handles platform-specific rendering
- `NativeElementCommonCode` contains shared logic (lifecycle, theming) to avoid duplication
- `ContainerElement` explicitly defines elements that can hold children
- Clear separation between what's common and what's platform-specific

### 2. RContext → ElementContext

Simple rename for consistency with the new Element terminology:

```kotlin
// Old
class SomeView(context: RContext) : RView(context)

// New
class SomeView(context: ElementContext) : NativeElement(context)
```

### 3. ViewWriter → ElementWriter + Type-Enforced Modifier Ordering

This is the most significant user-facing change. The old `ViewWriter` allowed modifiers in any order, which could cause subtle bugs. The new `ElementWriter` uses Kotlin's type system to enforce correct ordering at compile time.

#### The Problem (Old System)

In version-7, all modifiers were on `ViewWriter` and returned `ViewWriter`:

```kotlin
abstract class ViewWriter {
    abstract fun willAddChild(view: RView)
    abstract fun addChild(view: RView)
}

// All modifiers return ViewWriter - no ordering enforcement
fun ViewWriter.weight(amount: Float): ViewWriter
fun ViewWriter.align(horizontal: Align, vertical: Align): ViewWriter
fun ViewWriter.scrolling: ViewWriter
fun ViewWriter.card: ViewWriter
```

This allowed incorrect orderings that could cause bugs:

```kotlin
// ❌ Bad but compiles - scrolling after weight causes layout issues
weight(1f) - scrolling - col { }

// ❌ Bad but compiles - alignment after theme doesn't work as expected
card - centered - button { }
```

#### The Solution (New System)

`ElementWriter` now has sub-interfaces that form a chain, each allowing only the next valid modifiers:

```kotlin
interface ElementWriter : KiteUiCoroutineScopeHelpers {
    val context: ElementContext
    fun willAddChild(element: Element)
    fun addChild(element: Element)

    // Hierarchy of modifier interfaces (least restrictive to most restrictive)
    interface CanAddAlignment : CanAddWeight              // Most permissive
    interface CanAddWeight : CanAddListElementModifier
    interface CanAddListElementModifier : CanAddShownWhen
    interface CanAddShownWhen : CanAddSizing
    interface CanAddSizing : CanAddTheme
    interface CanAddTheme : CanAddScrolling
    interface CanAddScrolling : ElementWriter             // Most restrictive
}

// ViewWriter is the fully permissive starting point
interface ViewWriter : ElementWriter.CanAddAlignment
```

Each modifier is defined on the appropriate interface and returns the next interface in the chain:

```kotlin
// Alignment modifiers - available first, return CanAddWeight
fun ElementWriter.CanAddAlignment.align(h: Align, v: Align): ElementWriter.CanAddWeight
fun ElementWriter.CanAddAlignment.centered: ElementWriter.CanAddWeight

// Weight modifiers - available after alignment, return CanAddListElementModifier
fun ElementWriter.CanAddWeight.weight(amount: Float): ElementWriter.CanAddListElementModifier
fun ElementWriter.CanAddWeight.expanding: ElementWriter.CanAddListElementModifier

// Visibility modifiers - available after list-element modifiers, return CanAddSizing
fun ElementWriter.CanAddShownWhen.shownWhen(condition: () -> Boolean): ElementWriter.CanAddSizing

// Sizing modifiers - available after visibility, return CanAddTheme
fun ElementWriter.CanAddSizing.sizedBox(constraints: SizeConstraints): ElementWriter.CanAddTheme
fun ElementWriter.CanAddSizing.sizeConstraints(...): ElementWriter.CanAddTheme

// Theme modifiers - available after sizing, return CanAddTheme (repeatable!)
fun ElementWriter.CanAddTheme.themed(theme: ThemeDerivation): ElementWriter.CanAddTheme
fun ElementWriter.CanAddTheme.card: ElementWriter.CanAddTheme
fun ElementWriter.CanAddTheme.important: ElementWriter.CanAddTheme

// Scrolling modifiers - available after theme, return ElementWriter
fun ElementWriter.CanAddScrolling.scrolling: ElementWriter
fun ElementWriter.CanAddScrolling.scrollingHorizontally: ElementWriter
```

#### Canonical Modifier Order

The type system enforces this order:

```
alignment → weight → shownWhen → sizing → theme → scrolling → element
```

**Mnemonic**: Position > Visibility > Scroll > Theme (then the element)

#### Examples

```kotlin
// ✅ Correct - enforced by type system
centered.weight(1f).shownWhen { isVisible() }.sizedBox(...).card.scrolling.col { }

// ✅ Correct - can skip stages
centered.card.button { }

// ✅ Correct - theme is repeatable
card.important.bold.button { }

// ❌ Won't compile - weight comes before alignment
weight(1f).centered.col { }
//         ^^^^^^^^ ERROR: centered not available on CanAddListElementModifier

// ❌ Won't compile - scrolling must come after theme
scrolling.card.col { }
//        ^^^^ ERROR: card not available on ElementWriter

// ❌ Won't compile - wrong order
sizedBox(...).weight(1f).col { }
//            ^^^^^^^^^^ ERROR: weight not available on CanAddTheme
```

#### Escape Hatch

In rare cases where you need to break the rules (and accept the risk of bugs), use:

```kotlin
@OptIn(UnsafeModifier::class)
someWriter.withUnsafeModifiers().card.col { }
```

This should be avoided in normal code.

### 4. Element Creation Flow

The element creation process is explicit and ordered:

```kotlin
@OptIn(ExperimentalContracts::class, InternalKiteUi::class, OverrideOnly::class)
inline fun <T : Element> ElementWriter.write(element: T, setup: T.() -> Unit = {}): T {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }

    willAddChild(element)  // 1. Modifiers configure the element
    setup(element)          // 2. User setup block runs
    element.onStartup()     // 3. Lifecycle starts
    addChild(element)       // 4. Added to native view hierarchy

    return element
}
```

All view DSL functions (like `col`, `button`, `text`) use `write()` internally.

## Migration Checklist

When migrating code from version-7 to view-split:

### For Library Code

- [ ] Rename `RView` → `NativeElement` (or `Element` for interfaces)
- [ ] Rename `RContext` → `ElementContext`
- [ ] Rename `ViewWriter` → `ElementWriter` in modifier implementations
- [ ] Update modifier function signatures to use correct `CanAdd*` interfaces
- [ ] Ensure modifiers return the correct next interface in the chain
- [ ] Update platform implementations (`NativeElement.android.kt`, etc.)
- [ ] Split container views to use `NativeContainerElement`
- [ ] Update `willAddChild` / `addChild` implementations

### For Application Code

- [ ] Rename `RContext` → `ElementContext` in custom view constructors
- [ ] Update custom modifiers to use new `ElementWriter` interfaces
- [ ] Fix any modifier ordering issues (compiler will catch these!)
- [ ] Update references to `RView` → `Element`

### Common Patterns

#### Custom View Migration

```kotlin
// Old
class CustomView(context: RContext) : RView(context) {
    override val native: View = ...
}

// New
class CustomView(context: ElementContext) : NativeElement(context) {
    // Platform native property name varies by platform
}
```

#### Custom Container Migration

```kotlin
// Old
class CustomContainer(context: RContext) : RView(context) {
    override fun internalAddChild(index: Int, view: RView) { ... }
}

// New
class CustomContainer(context: ElementContext) : NativeContainerElement(context) {
    override fun nativeAddChild(index: Int, element: Element) { ... }
    override fun nativeRemoveChild(index: Int) { ... }
    override fun nativeClearChildren() { ... }
}
```

#### Modifier Implementation Migration

```kotlin
// Old
fun ViewWriter.myModifier(): ViewWriter = beforeNextElementSetup {
    // configure element
}

// New - determine correct interface based on when it should be applied
fun ElementWriter.CanAddTheme.myModifier(): ElementWriter.CanAddTheme =
    themed(this, myThemeDerivation)

// Or for sizing modifiers:
fun ElementWriter.CanAddSizing.myModifier(): ElementWriter.CanAddTheme =
    beforeSetup {
        // configure element
    }
```

## Why This Migration?

### Before: Problems with Old System

1. **Modifier ordering bugs**: Applying modifiers in wrong order caused subtle layout/rendering issues
2. **Monolithic classes**: `RView` mixed interface, platform code, and common code
3. **Unclear boundaries**: What's platform-specific vs common wasn't always clear
4. **No compiler help**: Easy to write buggy modifier chains that compiled fine

### After: Benefits of New System

1. **Compile-time safety**: Type system prevents incorrect modifier ordering
2. **Clear architecture**: Element (interface) → NativeElement (platform) → NativeElementCommonCode (shared)
3. **Better separation**: Platform-specific code clearly separated from common logic
4. **Explicit contracts**: `ContainerElement` explicitly defines what can hold children
5. **Easier to understand**: Each class has a single, clear responsibility

## Implementation Status

**Branch**: `version-8` (migration is complete and merged)

**📊 See [MIGRATION_STATUS_ANDROID_HTML.md](MIGRATION_STATUS_ANDROID_HTML.md) for detailed Android and HTML migration status**

All work completed:
- ✅ Android native modifiers
- ✅ Android views migrated to NativeElement
- ✅ JS/HTML modifiers migrated
- ✅ Context addon migration
- ✅ Modifier DSL migration
- ✅ Core Element/ElementWriter/NativeElement interfaces
- ✅ iOS native elements
  - ✅ Core NativeElement.ios.kt refactor
  - ✅ Interactive elements (Button, Checkbox, RadioButton, Switch, etc.)
  - ✅ Text input elements (TextField, TextArea, AutoComplete, etc.)
  - ✅ Container elements (LinearLayouts, Frame, ScrollView, DismissBackground, CoordinatorFrame, ProgrammaticLayout, SwapView)
  - ✅ Link components (Link, ExternalLink)
  - ✅ Selection elements (Select, MenuButton, Slider)
  - ✅ Date/Time inputs (LocalDateTimeField, NumberField, FormattedTextInput)
  - ✅ Icon and toggle components (IconView, ToggleButton, RadioToggleButton)
  - ✅ Display elements (ActivityIndicator, TextView, Space, Separator, ProgressBar, CircularProgress)
  - ✅ Media elements (Canvas, RawImageView, Video, WebView)
  - ⏭️ ImageCrop (commented out - not implemented on iOS)
- ✅ Platform-specific modifier implementations
- ✅ Example app updates

## Testing Strategy

Migration is complete. For ongoing verification:

1. Run full test suite on all platforms
2. Manual testing of example-app on all platforms
3. Modifier ordering enforcement is validated at compile time
4. Performance testing (ensure no regressions)
5. See application code migration patterns above for external users

## Questions?

See also:
- `GoodKiteuiCode.md` - Component and page creation patterns
- `ThemeRules.md` - Theme system behavior
- `library/src/commonMain/kotlin/com/lightningkite/kiteui/views/ElementWriter.kt` - Core interfaces
- `library/src/commonMain/kotlin/com/lightningkite/kiteui/views/Element.kt` - Element interface
