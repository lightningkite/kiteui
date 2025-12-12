# View Hierarchy Management

## Overview

KiteUI's view hierarchy system provides powerful tools for managing parent-child relationships, with automatic lifecycle management, theme propagation, and resource cleanup.

## Core Concepts

### Parent-Child Relationships

Every `RView` can have:
- **One parent** (or null if it's a root view)
- **Multiple children** (stored in order)

```kotlin
val parent = frameLayout {
    val child1 = text("First")
    val child2 = text("Second")
}

println(child1.parent === parent)  // true
println(parent.children.size)  // 2
```

### Automatic Management via DSL

The ViewWriter DSL automatically manages hierarchy:

```kotlin
col {
    // Children are automatically added in order
    text("Item 1")
    text("Item 2")
    text("Item 3")
}
```

Behind the scenes, this calls:
1. `willAddChild(view)` - Sets parent reference
2. `addChild(view)` - Adds to children list and native hierarchy
3. `view.postSetup()` - Finalizes initialization

## Manual Hierarchy Management

### Adding Children

```kotlin
// Add at end
parent.addChild(childView)

// Insert at specific index
parent.addChild(index = 1, view = childView)
```

**Important**: Always call `willAddChild()` before adding if you're managing manually:

```kotlin
parent.willAddChild(childView)
parent.addChild(childView)
```

### Removing Children

```kotlin
// Remove specific child (automatically shuts down)
parent.removeChild(childView)

// Remove by index
parent.removeChild(index = 0)

// Remove all children
parent.clearChildren()
```

**Critical**: Removing a child automatically calls `shutdown()` on it, which:
- Cancels all coroutines
- Removes all listeners
- Recursively shuts down all descendants
- Clears resource references

### Reading Children

```kotlin
// Access the children list (read-only)
val allChildren: List<RView> = parent.children

// Find specific children
val textViews = parent.children.filterIsInstance<TextView>()

// Check if has children
val hasChildren = parent.children.isNotEmpty()
```

## Lifecycle Integration

### Theme Propagation

Themes automatically cascade down the hierarchy:

```kotlin
parentView.theme = myTheme
// All children automatically inherit myTheme
```

When a parent's theme changes:
1. Parent applies the theme via `applyTheme()`
2. Parent calls `refreshTheming()` on all children
3. Children recompute their themes based on parent + their `themeChoice`
4. Process repeats recursively

### Startup and Shutdown

```kotlin
// Lifecycle hooks
class MyView(context: RContext) : RView(context) {

    override fun postSetup() {
        super.postSetup()
        // Called after fully initialized and added to parent
        // Safe to access parent and apply themes
    }

    override fun shutdown() {
        super.shutdown()
        // Called when removed from hierarchy
        // Clean up any remaining resources
    }
}
```

**Gotcha**: Don't override `shutdown()` without calling `super.shutdown()`, or resources will leak.

## Specialized View Types

### RViewWriter

Delegates gap (spacing) to its parent:

```kotlin
class MyWrapper(context: RContext) : RViewWriter(context) {
    // gap property returns parent's gap if not explicitly set
}
```

Use this for transparent wrapper views that shouldn't define their own spacing.

### RViewWithAction

Manages an `Action` with automatic state tracking:

```kotlin
class MyButton(context: RContext) : RViewWithAction(context) {
    init {
        action = myAction  // Automatically tracks working state
    }
}
```

The view automatically:
- Listens to the action's state
- Updates `working` signal
- Applies working theme semantic
- Cleans up listener on shutdown

### RViewWithSecondaryAction

Manages both primary and secondary actions:

```kotlin
class MyDialog(context: RContext) : RViewWithSecondaryAction(context) {
    init {
        action = confirmAction
        secondaryAction = cancelAction
        // Both actions tracked automatically
    }
}
```

## Exception Handling Hierarchy

Exceptions bubble up the view hierarchy:

```kotlin
parentView += ExceptionHandler { source, working, exception ->
    // Handle exceptions from this view and all descendants
    when (exception) {
        is NetworkException -> {
            showToast("Network error")
            {}  // Return cleanup function
        }
        else -> null  // Let it propagate to parent
    }
}
```

Exception handlers form a chain:
1. Check current view's handlers
2. If none handle it, try parent's handlers
3. Continue up to root
4. Fall back to `ExceptionHandlers.root`

## Working and Loading State Propagation

Views track aggregate state from their reactive operations:

```kotlin
val view = myView {
    // Any reactive operation in this scope contributes to working/loading
    launch {
        val data = loadData()  // Sets loading = true while running
        processData(data)  // Sets working = true while running
    }
}

// View's state reflects ongoing operations
println(view.loading.value)  // true while data loading
println(view.working.value)  // true while processing
```

This state automatically applies theme semantics (LoadingSemantic, WorkingSemantic).

## Advanced Patterns

### Dynamic Child Management

```kotlin
class DynamicList(context: RContext) : RView(context) {
    var items by Signal(emptyList<String>())

    init {
        items.addListener { newItems ->
            // Efficiently update children to match items
            clearChildren()
            newItems.forEach { item ->
                addChild(TextView(context).apply {
                    content = item
                })
            }
        }
    }
}
```

**Better**: Use KiteUI's built-in `forEach` or `Recycler2` for efficient list management.

### Conditional Views

```kotlin
col {
    if (showAdvanced) {
        // These views only exist when condition is true
        advancedSettings()
    }
}
```

The DSL handles adding/removing views as needed.

### View Pooling

For frequently created/destroyed views:

```kotlin
class ViewPool<T : RView>(val factory: (RContext) -> T) {
    private val pool = mutableListOf<T>()

    fun acquire(context: RContext): T {
        return pool.removeFirstOrNull() ?: factory(context)
    }

    fun release(view: T) {
        view.parent?.removeChild(view)  // Don't shutdown!
        pool.add(view)
    }
}
```

**Warning**: This pattern requires careful management to avoid leaks. Prefer using `Recycler2` instead.

## Performance Considerations

### Minimize Hierarchy Depth

Deeper hierarchies mean:
- More theme recalculations
- Slower layout passes
- More memory usage

**Good**:
```kotlin
row {
    text("Name")
    text("Value")
}
```

**Bad**:
```kotlin
col {
    row {
        col {
            text("Name")
        }
    }
    row {
        col {
            text("Value")
        }
    }
}
```

### Batch Child Operations

```kotlin
// Bad: Adds children one at a time
items.forEach { parent.addChild(createView(it)) }

// Better: Use DSL which can optimize
parent.apply {
    items.forEach {
        text(it)
    }
}

// Best: Use reactive lists
parent.forEach(itemsSignal) { item ->
    text(item)
}
```

### Lazy Initialization

Don't create children until needed:

```kotlin
class TabView(context: RContext) : RView(context) {
    private var tabContent: RView? = null

    fun showTab(index: Int) {
        tabContent?.let { removeChild(it) }
        tabContent = createTabContent(index)
        addChild(tabContent!!)
    }
}
```

## Common Issues

### Shutdown Warnings

If you see "WARNING!! view is shut down, but attempt to call addChild was made":

- You're operating on a view after it was shut down
- Check that you're not holding stale references
- Ensure coroutines are properly canceled

### Theme Not Applying

If themes aren't appearing:

- Check that `postSetup()` was called
- Verify parent is also fully started
- Ensure `refreshTheming()` is called when needed

### Memory Leaks

If views aren't being garbage collected:

- Enable leak detection: `RViewHelper.leakDetection = true`
- Check for strong references in closures
- Verify shutdown() was called
- Look for listeners that weren't removed

## Best Practices

1. **Use the DSL** - Let KiteUI manage hierarchy when possible
2. **Don't skip willAddChild** - Always call it before manual addChild
3. **Let removal clean up** - Don't shutdown manually before removing
4. **Watch for cycles** - Don't create circular parent-child references
5. **Respect shutdown** - Never use a view after it's shut down
6. **Batch operations** - Group multiple child changes together
7. **Keep it shallow** - Minimize unnecessary nesting

## Next Steps

- Learn about [Reactive Lists](./reactive-lists.md) for efficient dynamic content
- Explore [Custom Views](./custom-views.md) for creating reusable components
- Understand [Memory Management](./memory-management.md) for avoiding leaks
