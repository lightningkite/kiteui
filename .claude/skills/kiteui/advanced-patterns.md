# KiteUI Advanced Patterns

## Keyboard Shortcuts

Add keyboard shortcuts to actions for power users:

```kotlin
// Global shortcut
onKeyCode(keyCode { shortcut + it.letter('n') }) {
    pageNavigator.navigate(NewItemPage())
}

// Button with shortcut
button {
    text("Save")
    action = saveAction
    onKeyCode(keyCode { shortcut + it.letter('s') }) {
        saveAction.startAction(this@button)
    }
}

// Menu item with shortcut hint
menuButton {
    icon(Icon.notifications, "Notifications")
    onKeyCode(keyCode { shortcut + it.letter('n') }) {
        launch { handleNotifications() }
    }
    opensMenu { /* ... */ }
}
```

**Keyboard modifier combinations:**
- `shortcut` - Cmd on Mac, Ctrl on Windows/Linux
- `shift` - Shift key
- `alt` - Alt/Option key
- `ctrl` - Control key (even on Mac)

## Drop Targets for Drag and Drop

Implement drag and drop for reordering or moving items:

```kotlin
card.link {
    dropTargetDelegate = object : DropTargetDelegate {
        override fun over(event: DragEvent): Boolean = true

        override fun drop(event: DragEvent): Boolean {
            val taskIds = DragConstants.decode(event.data) ?: return false
            confirmDanger("Move Tasks", "Move to this project?") {
                taskIds.forEach { taskId ->
                    launch {
                        session().task[taskId].modify(modification {
                            it.project assign targetProject._id
                        })
                    }
                }
            }
            return true
        }
    }
    // ... link content ...
}
```

## Responsive Dual-Pane Navigation

Show multiple pages side-by-side on wide screens:

```kotlin
fun ViewWriter.sidewisePageNavigator(navigator: PageNavigator) = row {
    val pages = remember {
        val newPages = generateSequence(navigator.currentPage()) {
            (it as? PagePlus)?.parentPage
        }.toMutableList()
        newPages.reverse()

        // Limit to pages that fit on screen
        while (newPages.sumOf { spaceByPageType(it::class) } > windowWidth() && newPages.size > 1) {
            newPages.removeAt(0)
        }
        newPages.takeLast(2)
    }

    forEachAnimated(pages, preHidingModifiers = { weight(spaceByPageType(it::class).toFloat()) }) {
        it.run { render() }
    }
}

// Check if using panes
val usingPanes = remember {
    activePagesAndWeights()?.invoke().let { it != null && it.size > 1 }
}
```

## Permission-Based UI Visibility

Show/hide UI elements based on user permissions:

```kotlin
shownWhen { session().myRole() != UserRole.Client }.button {
    text("Add Project")
    onClick { pageNavigator.navigate(NewProjectPage()) }
}

// Conditional action availability
link {
    ::to {
        if (session().hasPermission(Permission.Edit)) {
            { EditPage(item()._id) }
        } else null
    }
    text { ::content { item().name } }
}
```

## Conditional Rendering

```kotlin
val showAdvanced = Signal(false)

col {
    checkbox {
        checked bind showAdvanced
    }

    // Using shownWhen (preferred)
    shownWhen { showAdvanced() }.card.col {
        h3("Advanced Options")
        // ... advanced controls
    }

    // Or using reactive (clear children first!)
    col {
        reactive {
            clearChildren()
            if (showAdvanced()) {
                advancedSettings()
            }
        }
    }
}
```

## RecyclerView for Large Lists

Use `recyclerView` + `children()` for efficient rendering of large lists:

```kotlin
expanding.frame {
    // Empty state
    shownWhen { items().isEmpty() }.centered.text {
        content = "No items found."
    }

    // Efficient list rendering
    recyclerView {
        children(items, id = { it._id }) { item ->
            card.row {
                text { ::content { item().name } }
                subtext { ::content { item().description } }
            }
        }
    }
}
```

## SwapView for Animated Transitions

```kotlin
swapView(remember { if (showAdvanced()) "advanced" else "simple" }) { mode ->
    when (mode) {
        "advanced" -> advancedSettings()
        "simple" -> simpleSettings()
    }
}
```

## ForEachAnimated

For lists with enter/exit animations:

```kotlin
forEachAnimated(items, preHidingModifiers = { /* ... */ }) { item ->
    card.text { ::content { item().name } }
}
```

## Component Design Best Practices

1. **Components should take minimal parameters** unique per usage
2. **Components should load their own data** (makes them easy to debug)
3. **Components should be used more than once**; single-use components should be inlined
4. **Keep view hierarchy shallow** to minimize theme recalculations

## Performance Best Practices

1. Use `forEach` for dynamic lists instead of manual child management
2. Use `Recycler2` (or `recyclerView` + `children()`) for long lists that need virtualization
3. **Use server-side filtering for search** - NEVER use client-side `.filter()` on ModelCache results
4. Keep view hierarchy shallow
5. Batch child operations when possible
6. Use `shared` / `remember` to avoid redundant calculations
7. Debounce user input (`.debounce(500)`) before triggering expensive operations
