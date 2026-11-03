# KiteUI Reactivity & State Management

## ⚠️ IMPORTANT: KiteUI Execution Model (Not Like React!)

KiteUI uses **fine-grained reactivity** like Solid JS, NOT virtual DOM diffing like React.

**Key difference:** The `render()` function runs **only once** when the view is first created. It does NOT re-run when state changes. Instead, only the specific reactive bindings (like `::content { }`) re-execute.

```kotlin
// ✅ This is FINE - render() only runs once, so launch runs once
override fun ViewWriter.render() {
    launch {
        val session = currentSession.await()
        if (session != null) {
            pageNavigator.navigate(HomePage())
        }
    }

    col {
        // This text binding re-runs when count changes, but render() does not
        text { ::content { "Count: ${count()}" } }
    }
}
```

**DO NOT apply React patterns to KiteUI:**
- ❌ Don't worry about "re-renders" - they don't exist in KiteUI
- ❌ Don't avoid `launch` in render - it only fires once
- ❌ Don't think of render as a "pure function" that shouldn't have side effects

**DO understand KiteUI's model:**
- ✅ `render()` sets up the view structure once
- ✅ Reactive bindings (`::property { }`, `reactive { }`) update automatically
- ✅ `launch` in render is fine for one-time initialization

---

## Signal - Mutable Reactive State

```kotlin
val count = Signal(0)

// Read value (in reactive context)
text { ::content { "Count: ${count()}" } }

// Write value
count.value = 5
count.value++
```

## Property - Another name for Signal

```kotlin
val email = Property("")
textInput { content bind email }
```

## Constant - Immutable Reactive Value

```kotlin
val title: Reactive<String> = Constant("Welcome")
```

## Shared / Remember - Computed Reactive Value

```kotlin
val firstName = Signal("John")
val lastName = Signal("Doe")
val fullName = remember { "${firstName()} ${lastName()}" }

text { ::content { fullName() } }
```

## RememberSuspending - Async Computed Reactive Value

Use `rememberSuspending` for async data loading (preferred over `Signal + launch` pattern):

```kotlin
// ✅ CORRECT: Use rememberSuspending for async data
val booth = rememberSuspending {
    val session = currentSession() ?: return@rememberSuspending null
    val res = session.reservations.get(reservationId).await() ?: return@rememberSuspending null
    session.booths.get(res.booth).await()
}

val location = rememberSuspending {
    val b = booth() ?: return@rememberSuspending null
    val session = currentSession() ?: return@rememberSuspending null
    session.locations.get(b.location).await()
}

// UI automatically shows loading state until data is ready
text { ::content { "Booth: ${booth()?.name}" } }
text { ::content { "Location: ${location()?.name}" } }
```

**❌ AVOID: Signal + launch pattern**
```kotlin
// DON'T DO THIS - harder to manage, no automatic loading states
val booth = Signal<Booth?>(null)
launch {
    booth.value = session.booths.get(boothId).await()
}
```

**Key Benefits:**
- Automatic loading states in UI (shows spinner while loading)
- Reactive recomputation when dependencies change
- Cleaner code with less boilerplate
- Proper error handling with nullable returns

**One-shot custom API calls (non-CRUD endpoints):** Use `rememberSuspending` for custom endpoint calls that return a snapshot rather than a live-updating list. This is the right pattern when ModelCache websocket updates are not available (e.g. a server-side joined feed):

```kotlin
// One-shot fetch from a custom endpoint — not live-updating
val posts = rememberSuspending {
    session().api.post.followedFeed(Query())
}

col {
    expanding.scrolling.col {
        col {
            reactive {
                clearChildren()
                val list = posts() ?: return@reactive
                list.forEach { post -> postCardStatic(post) }
            }
        }
    }
}
```

## LazyProperty - Computed with Override

```kotlin
val calculatedValue = LazyProperty { baseValue() * 2 }

// Can override
calculatedValue.value = 100

// Reset to calculation
calculatedValue.reset()
```

## LateInitProperty - Initially Unset

```kotlin
val userData = LateInitProperty<UserData>()

// Components show loading until set
userData.value = fetchedUserData

// Can unset
userData.unset()
```

## Reactive Scope

```kotlin
text {
    // Automatically re-runs when dependencies change
    ::content { "Total: ${price() * quantity()}" }
}
```

## ReactiveScope Block

**⚠️ WARNING: reactive adds duplicate views on every rerun!**

```kotlin
reactive {
    // Re-runs when signals inside change
    if (showAdvanced()) {
        advancedSettings()  // ⚠️ This adds NEW views each time, doesn't replace!
    }
}
```

**When to use:**
- Rarely! Almost always better to use `shownWhen`, `swapView`, or `::property { }` bindings
- If you must use it, ALWAYS use `clearChildren()` first

**Better alternatives:**

```kotlin
// ✅ BEST: Use shownWhen modifier for conditional visibility
shownWhen { showAdvanced() }.card.col {
    h3 { content = "Advanced Settings" }
    // ... advanced content ...
}  // Automatically shown/hidden reactively

// ✅ BEST: Use swapView for animated transitions between views
swapView(remember { if (showAdvanced()) "advanced" else "simple" }) { mode ->
    when (mode) {
        "advanced" -> advancedSettings()
        "simple" -> simpleSettings()
    }
}

// ✅ GOOD: Use reactive bindings for automatic loading states
text { ::content { userData()?.name ?: "Loading..." } }

// ❌ AVOID: Manual reactive creates duplicate views
reactive {
    val data = userData()
    if (data != null) {
        text { content = data.name }  // Adds new text view on every userData change!
    }
}

// ⚠️ LAST RESORT: If you MUST use reactive, ALWAYS clear children first
reactive {
    clearChildren()  // ⚠️ CRITICAL: Remove old views before adding new ones
    if (showAdvanced()) {
        advancedSettings()
    }
}

// Example: Dynamic lists with reactive (when forEach doesn't work)
col {
    reactive {
        clearChildren()  // ⚠️ MUST call this to prevent duplicates

        booth()?.lockIds?.forEach { lockInfo ->
            button {
                text { content = lockInfo.name }
                onClick { unlock(lockInfo.id) }
            }
        }
    }
}
```

**Recommended patterns (in order of preference):**
1. **shownWhen** - For simple show/hide of sections
2. **swapView** - For animated transitions between different views
3. **::property { }** - For automatic loading states and reactive text
4. **forEach** - For dynamic lists (when it works - may have issues with complex types)
5. **reactive + clearChildren()** - Last resort when the above don't work

**⚠️ CRITICAL: Always call `clearChildren()` as the first line in reactive blocks that add views!**

## ForEach - Reactive Lists

Use `forEach` to render dynamic lists that update when the underlying data changes:

```kotlin
val items = Signal(listOf("A", "B", "C"))

col {
    forEach(items) { item ->
        card.text(item)
    }
}
```

**Dynamic lists from async data:**
```kotlin
// Load data reactively
val booth = rememberSuspending {
    val session = currentSession() ?: return@rememberSuspending null
    val res = session.reservations.get(reservationId).await() ?: return@rememberSuspending null
    session.booths.get(res.booth).await()
}

// Render list of doors with forEach + shared
col {
    h3 { content = "Available Doors" }

    forEach(remember { booth()?.lockIds ?: emptyList() }) { lockInfo ->
        button {
            row {
                icon { source = AppIcons.lockOpen }
                text { content = lockInfo.name }
            }
            action = Action("Unlock ${lockInfo.name}") {
                unlockDoor(lockInfo.id)
                toast("Unlocked ${lockInfo.name}")
            }
        }
    }
}
```

**Key points:**
- `forEach` automatically updates when the list changes
- Use `remember { }` to transform reactive data before rendering
- Empty list handling: `booth()?.lockIds ?: emptyList()` gracefully handles null
- Each item gets its own scope for actions and state

## Two-way Binding

```kotlin
val text = Signal("")

textInput {
    content bind text  // Bidirectional binding
}
```

## Reactive Lens Extensions

KiteUI includes powerful reactive lens extensions for common transformations. These provide bidirectional bindings between different types.

### Value Comparison - `.equalTo()`

Perfect for radio buttons and conditional checks:

```kotlin
val selected = Signal(1)

// Radio button binding
radioButton { checked bind selected.equalTo(1) }
radioButton { checked bind selected.equalTo(2) }
radioButton { checked bind selected.equalTo(3) }
```

### Collection Membership - `.contains()`

Toggle items in sets or lists:

```kotlin
val tags = Signal(setOf<String>())
checkbox { checked bind tags.contains("important") }

val items = Signal(listOf<String>())
checkbox { checked bind items.contains("featured") }
```

### Null Handling

Convert nullable signals to non-null with defaults:

```kotlin
// Nullable to non-null with default value
val name = Signal<String?>(null)
textInput { content bind name.notNull("Default") }

// String? to String (null becomes blank)
val description = Signal<String?>(null)
textInput { content bind description.nullToBlank() }
```

### String to Number Conversions

Bind number inputs directly to string signals:

```kotlin
val ageStr = Signal("")

// Decimal conversions
numberInput { content bind ageStr.asInt() }
numberInput { content bind ageStr.asDouble() }
numberInput { content bind ageStr.asFloat() }
numberInput { content bind ageStr.asLong() }

// Hexadecimal conversions
val hexStr = Signal("")
numberInput { content bind hexStr.asIntHex() }
numberInput { content bind hexStr.asUIntHex() }
```

### How Lens Extensions Work

These extensions use the reactive lens pattern to create bidirectional transformations:

```kotlin
// Example: .equalTo() implementation
infix fun <T> MutableReactive<T>.equalTo(value: T): MutableReactive<Boolean> = lens(
    get = { it == value },                    // Read: return true when equal
    modify = { o, it -> if (it) value else o } // Write: set value when true
)
```

This pattern enables:
- **Type-safe transformations** at compile time
- **Bidirectional data flow** between different types
- **Automatic updates** when either side changes

## Loading States

```kotlin
val data = LateInitProperty<MyData>()

col {
    // Automatically shows loading indicator while data is unset
    text { ::content { data().displayName } }
}

// Later, set the data
launch {
    data.value = fetchData()
}
```

## ReactiveScope Labeled Returns

Always use labeled returns in `reactive` blocks when handling nullable values:

```kotlin
// ✅ CORRECT - Labeled return
reactive {
    val item = mySignal() ?: return@reactive
    // Safe to use item here
    text { content = item.name }
}

// ✅ CORRECT - Labeled return in launch
launch {
    val session = currentSession.await() ?: return@launch
    // Safe to use session here
}

// ❌ WRONG - Bare return causes compilation error
reactive {
    val item = mySignal() ?: return  // Error: "return is prohibited here"
}
```

**Why labeled returns?**
`reactive` is a lambda, not a function body, so you must specify which lambda you're returning from.

## Common Issue: clearChildren() Clears Sibling Elements

**Symptoms:**
- Only the last section of a page renders
- Static elements (headers, buttons) disappear after reactive content loads
- Page content vanishes when signals update

**Root Cause:** When `reactive { clearChildren() }` is used directly inside a `col` or `row`, `clearChildren()` clears ALL children of the parent container, not just the content within the reactive.

**❌ WRONG - clearChildren clears siblings:**
```kotlin
expanding.scrolling.col {
    h2("Projects")  // Gets cleared!
    reactive {
        clearChildren()
        projects().forEach { /* render project */ }
    }

    h2("Tasks")  // Gets cleared!
    reactive {
        clearChildren()
        tasks().forEach { /* render task */ }
    }

    h2("Runners")  // Only this section survives (last reactive wins)
    reactive {
        clearChildren()
        runners().forEach { /* render runner */ }
    }
}
```

**✅ CORRECT - Wrap each reactive in its own container:**
```kotlin
expanding.scrolling.col {
    h2("Projects")
    col {  // Isolated container
        reactive {
            clearChildren()  // Only clears this col's children
            projects().forEach { /* render project */ }
        }
    }

    h2("Tasks")
    col {  // Isolated container
        reactive {
            clearChildren()
            tasks().forEach { /* render task */ }
        }
    }

    h2("Runners")
    col {  // Isolated container
        reactive {
            clearChildren()
            runners().forEach { /* render runner */ }
        }
    }
}
```

**Key Insight:** `clearChildren()` operates on the ViewWriter's current context. Without an isolating container, all reactive blocks share the parent's context and interfere with each other.
