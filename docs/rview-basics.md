# RView Basics (Historical Reference)

> **This document describes the old `RView`-based API, which no longer exists.**
> The RView → Element split has landed on `version-8`. The current model is:
> - `Element` (interface) + `NativeElement` (platform implementation) replace `RView`
> - `ElementContext` replaces `RContext`
> - `ElementWriter` replaces `ViewWriter`, with type-enforced modifier ordering
>
> See [MIGRATION.md](../MIGRATION.md) for the migration rationale and current patterns.

---

## Overview (Historical)

`RView` was the foundational class for all views in the KiteUI framework before the version-8 refactor. This document is retained as a historical reference for understanding the pre-migration architecture. All new code should use `Element`/`NativeElement` and `ElementWriter` instead.

## Key Concepts

### Platform Independence

`RView` is defined as an `expect` class in common code, with platform-specific `actual` implementations for each target:

- **Android**: Wraps Android's `View` class
- **iOS**: Wraps UIKit's `UIView` class
- **Web (JS)**: Wraps HTML elements
- **JVM**: Wraps Swing/JavaFX components

This architecture allows you to write your UI logic once while leveraging native platform capabilities.

### View Hierarchy

Views form a tree structure with parent-child relationships:

```kotlin
val parentView = someContainer {
    // Children are automatically added to the parent
    text("Hello")
    button("Click me")
}
```

The hierarchy is managed automatically through the `ViewWriter` DSL, but you can also manage children manually:

```kotlin
parentView.addChild(childView)
parentView.removeChild(childView)
parentView.clearChildren()
```

**Important**: When you remove a child, it is automatically shut down, canceling all its coroutines and cleaning up resources.

## View Lifecycle

Every RView goes through these lifecycle stages:

1. **Construction** - View is created with an `RContext`
2. **Setup** - Properties are configured, children may be added
3. **postSetup()** - Called when fully configured and added to parent
4. **Active** - View is part of the hierarchy and responds to changes
5. **shutdown()** - View is removed, all resources cleaned up

### Creating Views

Views should be created using the ViewWriter DSL:

```kotlin
class MyScreen : Screen {
    override fun ViewWriter.render() {
        col {
            h1("Welcome")
            text("This is my screen")
        }
    }
}
```

**Threading**: Views must be created on the main/UI thread. Platform implementations enforce this requirement.

### Cleanup

When a view is no longer needed, call `shutdown()` or remove it from its parent:

```kotlin
// Removing automatically shuts down the child
parentView.removeChild(childView)

// Or explicitly
childView.shutdown()
```

After shutdown, the view:
- Cancels all coroutines
- Removes all reactive listeners
- Recursively shuts down all children
- Clears parent references

## Common Properties

### Visibility

Control whether a view is displayed:

```kotlin
view.shown = false  // Removes from layout (like CSS display: none)
view.visible = false  // Invisible but takes space (like CSS visibility: hidden)
view.opacity = 0.5  // Semi-transparent
```

### Layout

Control spacing and alignment:

```kotlin
view.gap = 8.dp  // Space between children
view.padding = 16.dp  // Uniform padding on all sides
view.paddingByEdge = Edges(top = 8.dp, bottom = 16.dp)  // Per-edge padding
```

### Interaction

```kotlin
view.ignoreInteraction = true  // Disable all user interaction
view.requestFocus()  // Request keyboard focus
view.scrollIntoView()  // Scroll to make this view visible
```

### Theme and Styling

Views automatically inherit themes from their parents:

```kotlin
// Apply a specific theme to this view and children
view.themeChoice = ThemeDerivation { it.copy(background = Color.blue) }

// Take non-cascading properties from parent
view.themeTakeNonCascadingFromParent = true
```

See [Theming Guide](./theming.md) for more details.

## Reactive State

Views integrate with KiteUI's reactive system through their built-in coroutine scope:

```kotlin
class MyView(context: RContext) : RView(context) {
    val counter = Signal(0)

    init {
        // Automatically cleaned up on shutdown
        counter.addListener {
            println("Counter is now: ${counter.value}")
        }
    }
}
```

Views also track working and loading states:

```kotlin
view.working.value  // true when actions are running
view.loading.value  // true when data is loading
```

These states automatically apply theme variations (e.g., showing spinners or disabled states).

## Common Patterns

### Type-Safe Child Access

Access children of specific types:

```kotlin
val textViews = parentView.children.filterIsInstance<TextView>()
```

### Relative Positioning

Calculate positions relative to another view:

```kotlin
val rect = view.rectangleRelativeTo(otherView)
// Returns view's rectangle in otherView's coordinate space
```

### Animation Control

Temporarily disable animations:

```kotlin
view.withoutAnimation {
    // Changes here happen instantly without transitions
    view.opacity = 0.5
    view.shown = false
}
```

### Debug Names

Assign debug names for easier troubleshooting:

```kotlin
view.debugName = "UserProfileCard"
// Now logs and toString() will show this name
```

## Best Practices

1. **Always use the DSL** - Prefer ViewWriter DSL over manual child management
2. **Respect lifecycle** - Don't use views after shutdown
3. **Mind the thread** - Create and modify views only on the main thread
4. **Let cleanup happen** - Don't manually manage coroutines; let shutdown() handle it
5. **Use reactive state** - Leverage the built-in coroutine scope for state management
6. **Theme consistently** - Use ThemeDerivation for customization rather than direct property changes

## Common Gotchas

- **Theme refresh timing**: Themes aren't applied until `postSetup()` is called
- **Parent theme inheritance**: Changing a parent's theme automatically updates all children
- **Shutdown cascades**: Removing a view shuts down all its descendants
- **Main thread requirement**: View operations must happen on the UI thread
- **Property observers**: Some properties (like `padding`) trigger layout recalculations

## Next Steps

- Learn about [Theming](./theming.md) to customize appearance
- Explore [View Wrappers](./view-wrappers.md) for specialized containers
- Understand [Actions and State](./actions-state.md) for interactive views
- See [Platform-Specific Features](./platform-features.md) for advanced usage
