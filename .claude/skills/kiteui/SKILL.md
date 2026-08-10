---
name: kiteui
description: This skill should be used when you are using KiteUI.
version: 3.0.0
---

# KiteUI Development Skill

KiteUI is a Kotlin Multiplatform UI framework using native view components and fine-grained reactivity (inspired by Solid.js).

**🔑 KEY CONCEPT: `render()` runs ONCE, not on every state change (unlike React).** Using `launch` in render is fine for one-time setup. Only reactive bindings (`::content { }`, `reactive { }`) re-execute when signals change.

## Agent Instructions

**Before answering complex questions, READ the relevant sub-file:**
- Layout/sizing/scrolling → [layout.md](layout.md)
- Signals/reactive/forEach → [reactivity.md](reactivity.md)
- ModelCache/server queries → [model-cache.md](model-cache.md)
- Text/buttons/inputs/icons → [components.md](components.md)
- Form validation patterns → [forms.md](forms.md)
- Pages/@Routable/navigation → [navigation.md](navigation.md)
- Toast/dialog/bottomSheet → [dialogs.md](dialogs.md)
- Semantic themes/dynamicTheme → [theming.md](theming.md)
- ExceptionHandler/Actions → [error-handling.md](error-handling.md)
- Setup/SDK regen/browser testing → [testing.md](testing.md)
- Keyboard shortcuts/drag-drop/recyclerView → [advanced-patterns.md](advanced-patterns.md)

---

## General Tips

- `scrolling` modifier makes views scroll vertically; will clip content otherwise
- `onClick {}`'s lambda is already suspending; no need to launch
- Avoid creating components unless significantly useful; layers of abstraction have cost
- Avoid custom spacing with `gap = x` - default spacing almost always looks good already
- 

## Using KiteUI with Lightning Server

- `./gradlew :server:generateSdk` will regenerate the SDK from the server definition
- Use ModelCache for CRUD, direct API for actions
```kotlin
// ❌ WRONG for data display - bypasses caching, no reactivity
val users = session.api.user.query(Query(...))

// ✅ CORRECT for CRUD - cached, reactive, auto-updates across all views
val users = remember { session.users.list(Query(...))() }

// ✅ CORRECT for non-CRUD actions (no caching needed)
session.api.door.unlock(doorId)
session.api.email.send(emailRequest)
```

- Prefer using server-side filtering; use `Condition.andNotNull()` with `?.let {}` for multiple optional filters

```kotlin
// ❌ WRONG - loads ALL data then filters client-side
val filtered = remember { allUsers().filter { it.name.contains(query()) } }

// ✅ CORRECT - server filters, only matching records sent
val filtered = remember {
    session.users.list(Query(condition {
        it.name.contains(query(), ignoreCase = true)
    }))()
}

// ✅ Multiple optional filters - Condition.andNotNull ignores nulls
val items = remember {
    session.items.list(Query(condition {
        Condition.andNotNull(
            tagFilter()?.let { tags -> it.tags any { t -> t inside tags } },
            dateFrom()?.let { d -> it.date gte d },
        )
    }))()
}
```

### ModelCache (Lightning Server)

```kotlin
// Setup: wrap endpoints in CachedApi
class UserSession(val api: Api) : CachedApi(api) { }

// Individual item (reactive)
val user = remember { session.users[userId]() }
text { ::content { user()?.name ?: "Loading..." } }

// Query (reactive list)
val activeUsers = remember {
    session.users.list(Query(condition { it.active eq true }))()
}
forEach(activeUsers) { user -> text { ::content { user().name } } }

// Modify (updates all views automatically)
launch {
    session.users[userId].modify(modification { it.name assign "New Name" })
}

// Insert new item — .add() returns the inserted T
launch { session.tasks.add(Task(_id = Task.ID(Uuid.random()), ...)) }

// Search with debounce
val searchDebounced = searchQuery.debounce(500)
val results = remember {
    session.users.list(Query(condition {
        if (searchDebounced().isEmpty()) Condition.Always
        else it.name.contains(searchDebounced(), ignoreCase = true)
    }))()
}
```

## ⚠️ CRITICAL PITFALLS (Read First!)

### P1. KSP `path` extension collisions — use subscript form `Model.path[Model_field]`

KSP generates both `Model_field` (`SerializableProperty`) and `Model.path.field` (`DataClassPath`). Two collisions to watch for:

1. **`name` field** — `Model.path.name` resolves to Kotlin's `KProperty.name` (wrong), not the generated path. Use `Model.path[Model_name]`.
2. **Field whose name matches a class in scope** — e.g. `Message.path.channel` when a `Channel` class is imported resolves to the class, not the path. Use `Message.path[Message_channel]`.

Escape hatch: subscript form always works:
```kotlin
// ❌ Ambiguous when field name collides with stdlib or imported class
SortPart(Channel.path.name, ascending = true)     // resolves to KProperty.name!
condition<Message> { it.channel eq channelId }    // 'channel' resolves to Channel class!

// ✅ Subscript form bypasses all collisions
SortPart(Channel.path[Channel_name], ascending = true)
condition<Message> { it[Message_channel] eq channelId }
```

Imports needed (KSP-generated, in model's package):
```kotlin
import com.lightningkite.lskiteuistarter.Channel_name
import com.lightningkite.lskiteuistarter.Message_channel
```

Verified: ChannelListScreen.kt, ChannelChatScreen.kt (demo/iter3-chat).

### P2. `SortPart` takes `DataClassPath`, NOT `SerializableProperty`

`Model_field` is a `SerializableProperty<Model, V>` — it won't compile in `SortPart`. Use `Model.path.field`, or the subscript form when the field name collides (see P1 above).

```kotlin
// ❌ WRONG — type mismatch: SerializableProperty vs DataClassPath
orderBy = listOf(SortPart(Message_createdAt, ascending = true))

// ✅ CORRECT — DataClassPath form
orderBy = listOf(SortPart(Message.path.createdAt, ascending = true))

// ✅ CORRECT — subscript form when name collides
orderBy = listOf(SortPart(Channel.path[Channel_name], ascending = true))
```

Verified: ChannelChatScreen.kt, ChannelListScreen.kt (demo/iter3-chat).

### P3. Reading a reactive (e.g. `session()`) is suspend — can't call inside render-time lambdas

`session()` and other `Reactive<T>` reads are suspend operations. Calling them inside `forEachById` or other render-time lambdas (which are not suspend) causes a compiler error. Call reactives inside a reactive context: `::content { }` or `reactive { }`.

```kotlin
// ❌ WRONG — can't call session() in forEachById's render lambda
forEachById(messages, id = { it._id }) { messageReactive ->
    val userId = session().userId  // compile error: suspension point in non-suspend context
    text { content = "..." }
}

// ✅ CORRECT — read session() inside a reactive binding
forEachById(messages, id = { it._id }) { messageReactive ->
    text { ::content {
        val userId = session().userId  // fine: ::content lambda IS reactive
        "${messageReactive().senderName}: ${messageReactive().content}"
    } }
}

// ✅ CORRECT — or read outside forEachById in the render scope (if session is already reactive)
val session = currentSessionNotNull
forEachById(messages, id = { it._id }) { messageReactive ->
    text { ::content {
        val userId = session().userId
        "..."
    } }
}
```

Verified: ChannelChatScreen.kt (demo/iter3-chat).

### P4. Labeled-field pattern — use `subtext` + input, or `field()` with explicit import

There are two verified patterns for a labeled input field:

**Pattern A — simple (used in ChannelListScreen dialogs):**
```kotlin
col {
    subtext("Channel name")
    textInput {
        hint = "e.g. general"
        keyboardHints = KeyboardHints.title
        content bind channelName
    }
}
```

**Pattern B — `field()` from `l2` (used in LoginScreen):**
```kotlin
import com.lightningkite.kiteui.views.l2.field  // explicit import required

field("Server") {
    select {
        bind(selectedValue, options) { it.label }
    }
}
```

`field()` requires an explicit import (`l2.*` star import does NOT resolve it reliably). It applies `fieldTheme` automatically and wires accessibility `labelFor`. Pattern A is simpler and always works.

Verified: ChannelListScreen.kt (Pattern A), LoginScreen.kt (Pattern B) on master.

### P5. `expanded.centered.text { }` fails — `expanding` returns `CanAddWeight`, not `CanAddAlignment`

`expanding` is defined on `ElementWriter.CanAddWeight` and returns `CanAddWeight`. But `centered` is defined on `ElementWriter.CanAddAlignment` (a supertype of `CanAddWeight`). Result: you cannot chain `.centered` directly after `.expanding`.

Wrap the expanding view in a `col {}` first, then use `centered` inside it:

```kotlin
// ❌ WRONG — compile error: centered not available on CanAddWeight
expanding.centered.text { ::content { org()?.name ?: "…" } }

// ✅ CORRECT — col provides the CanAddAlignment context for centered
expanding.col {
    centered.text { ::content { org()?.name ?: "…" } }
}
```

Verified: `ElementWriter.kt` in KiteUI source (`expanding` is `val CanAddWeight.expanding get() = weight(1f)`; `centered` is `val CanAddAlignment.centered`); iter4 OrgListScreen and TaskBoardScreen both use `expanding.col { centered.text { } }`.

### P6. Use `.add()` to insert items into ModelCache — `.insert()` is deprecated

`.add(item): T` is the current insert method on `ModelCacheLike`. It returns the inserted `T` directly.

`.insert(item)` is `@Deprecated("Use add instead")` and returns a `ModelCacheItemReadable<T>` wrapper — not the item.

```kotlin
// ❌ OLD / DEPRECATED
session.listings.insert(listing)

// ✅ CORRECT — returns the inserted item as T
session.listings.add(listing)
```

When you need the item's id for navigation after insert, construct the model with `_id` before calling `.add()` and reuse that id:

```kotlin
val task = Task(_id = Task.ID(Uuid.random()), organization = orgId, title = title, ...)
session.tasks.add(task)
pageNavigator.replace(TaskDetailPage(task._id))  // use the id you assigned
```

Verified: `ModelCacheLike.kt` in lightning-server-kiteui; iter4 TaskBoardScreen.kt uses `.add()`.

### P7. `select` + async server write — use `.withWrite { }` to adapt a `Reactive<T>` into `MutableReactive<T>`

`select.bind()` requires a `MutableReactive<T>`. A server-backed value (e.g. `session.tasks[id]()`) is a `Reactive<T>`, not `MutableReactive<T>`, so you cannot bind it directly to a `select`.

The `Reactive<T>.withWrite { value -> ... }` extension (from `com.lightningkite.reactive.extensions`) wraps any `Reactive<T>` into a `MutableReactive<T>` by supplying the write action:

```kotlin
import com.lightningkite.reactive.extensions.withWrite

// Bind a select to a server-backed field, writing back via modify()
val statusBinding = taskReactive.withWrite { newStatus ->
    session().tasks[taskReactive()._id].modify(
        modification<Task> { it.status assign newStatus }
    )
}
select {
    bind(statusBinding, Constant(TaskStatus.entries)) { it.name }
}
```

**Note on loading states:** `taskReactive` must resolve (not be in a loading state) when the user interacts with the select. Wrap in `shownWhen { taskReactive.state.success }` if needed.

If `withWrite` feels too complex for the use case (e.g. a status transition grid), per-value buttons with `Action` lambdas calling `.modify()` directly are the simpler and equally idiomatic fallback — see iter4 TaskBoardScreen.kt for a working example.

Verified: `Select.kt` (`bind` requires `MutableReactive<T>`); `helpers.kt` in reactive library (`withWrite` definition).

### P8. `condition<T> { }` builds a condition from a LAMBDA — use `Condition.Always` for "no filter"

`condition<T> { it.field eq x }` takes a lambda that builds a `Condition<T>`. For the unconstrained
case (match everything), use the constant `Condition.Always` — NOT `condition<T> { true }`.

```kotlin
val cond = if (query.isBlank()) Condition.Always
           else condition<Contact> { it[Contact_name].contains(query, true) }
```

Aggregate calls like `count()` are also `suspend` — call them in `rememberSuspending { }`, not
`remember { }`. Verified: demo/iter6-pagination ContactBrowseScreen.kt.

### P9. `PersistentProperty<T>` stores JSON-encoded values (token-injection / test gotcha)

A `PersistentProperty<String?>` (and friends) serialize their value as **JSON** in localStorage — a
`String?` is stored as `"\"the-token\""`, not the raw string. To inject a session token from outside
the app (e.g. a test harness setting `localStorage`), write the JSON-encoded form, not the bare value.
Verified: `PersistentProperty` source; observed when injecting a session token during iter-6 testing.

### 0. Action lambda has NO element/context receiver — capture context before defining it

`Action("label") { ... }` is `suspend CoroutineScope.() -> Unit`. Unlike `onClick {}`, it does NOT have `ElementWriter` or `ElementContext` as a receiver, so `context`, `toast(...)`, and `pageNavigator` are out of scope inside it.

**Pattern:** capture `val elementContext = this.context` (inside the `ElementWriter` scope) **before** the `Action`, then use `elementContext` inside the lambda.

```kotlin
override fun ElementWriter.CanAddTheme.render() {
    // Capture context for Action lambdas — must be done OUTSIDE the Action
    val elementContext = this.context

    button {
        text("Save")
        action = Action("Save") {
            // ✅ Use captured context
            elementContext.toast("Saved!")
            elementContext.pageNavigator.goBack()
            // ❌ 'context' and 'toast(...)' are NOT in scope here
        }
    }
}
```

Contrast: `onClick {}` DOES have `ElementContext` as receiver, so `context` and `pageNavigator` work directly there.

Verified in: CreateListingScreen.kt, ListingDetailScreen.kt, EditListingScreen.kt (ls-kiteui-starter marketplace demo, iter 2).

### 1. V7 uses DOT notation, NOT dash
```kotlin
// ✅ V7 CORRECT
expanding.scrolling.card.col { }

// ❌ V6 WRONG - dash doesn't exist in v7
expanding - scrolling - card - col { }
```

### 3. reactive DUPLICATES views on rerun
```kotlin
// ❌ WRONG - adds new text view on every signal change!
reactive {
    text { content = userData()?.name ?: "Loading" }
}

// ✅ CORRECT - use shownWhen for conditional visibility
shownWhen { showAdvanced() }.card.col { advancedSettings() }

// ✅ CORRECT - use reactive binding for dynamic content
text { ::content { userData()?.name ?: "Loading..." } }

// ✅ CORRECT - if you MUST use reactive, ALWAYS clearChildren first
col {
    reactive {
        clearChildren()  // ⚠️ CRITICAL - must be first line
        items().forEach { item -> text(item.name) }
    }
}
```

### 4. clearChildren() clears SIBLINGS if not isolated
```kotlin
// ❌ WRONG - each reactive clears the parent col's children
col {
    h2("Projects")
    reactive { clearChildren(); /* ... */ }  // Clears h2 above!
    h2("Tasks")
    reactive { clearChildren(); /* ... */ }  // Clears everything above!
}

// ✅ CORRECT - wrap each reactive in its own container
col {
    h2("Projects")
    col { reactive { clearChildren(); /* ... */ } }  // Isolated
    h2("Tasks")
    col { reactive { clearChildren(); /* ... */ } }  // Isolated
}
```

### 7. Use labeled returns in reactive/launch
```kotlin
// ❌ WRONG - "return is prohibited here"
reactive { val item = signal() ?: return }

// ✅ CORRECT
reactive { val item = signal() ?: return@reactive }
launch { val session = currentSession.await() ?: return@launch }
```

### 8. Calculate derived values with `remember`, don't store them
```kotlin
// ❌ WRONG - storing derived value in Signal + manual sync
val firstName = Signal("John")
val lastName = Signal("Doe")
val fullName = Signal("")  // Unnecessary Signal

reactive {
    fullName.value = "${firstName()} ${lastName()}"  // Manual update
}

// ✅ CORRECT - calculate with remember (auto-recomputes, shows loading, propagates errors)
val firstName = Signal("John")
val lastName = Signal("Doe")
val fullName = remember { "${firstName()} ${lastName()}" }

// ✅ CORRECT - async derived values with rememberSuspending
val userId = Signal<String?>(null)
val userDetails = rememberSuspending {
    val id = userId() ?: return@rememberSuspending null
    session.users.get(id).await()  // Loading state shown automatically
}
```

### 9. Modifier order matters with `shownWhen`
```kotlin
// ❌ BROKEN - expanding applied to child inside wrapper; wrapper has flex-grow:0 → height:0!
shownWhen { !isLoading() }.expanding.recyclerView { ... }

// ✅ CORRECT - expanding applied to the shownWhen wrapper itself
expanding.shownWhen { !isLoading() }.recyclerView { ... }
```
Layout modifiers (`expanding`, `weight()`, `sizeConstraints()`) must go **before** `shownWhen`, not after.

### 9b. Don't build tabs from parallel `shownWhen` branches — use pages or `swapView`

Four sibling `shownWhen` branches keeps every tab's views alive in the tree at once, and it
silently breaks height propagation: `shownWhen` inserts an intermediate column that only becomes
a flex container once one of *its* direct children carries weight. Your content column usually
doesn't, so it stays `display: block` and any `expanding` child inside collapses to content
height — while reporting `flex-grow: 1` on the element you're inspecting. The symptom points at
the wrong node.

```kotlin
// ❌ WRONG - all four tabs live in the tree; the expanding output pane collapses to one line
col {
    expanding.shownWhen { tab() == Tab.Console }.col { consoleTab() }
    expanding.shownWhen { tab() == Tab.Snapshot }.col { snapshotTab() }
    // ...
}

// ✅ BEST - each tab is its own @Routable page; only the active one exists, and it's URL-addressable
@Routable("/")         object ConsolePage : Page { /* ... */ }
@Routable("/snapshot") object SnapshotPage : Page { /* ... */ }
// then wire them up as navItems on appNav(navigator) { ... }

// ✅ OK - swapView when the tabs genuinely shouldn't be routes
swapView(tab) { t -> when (t) { Tab.Console -> consoleTab(); Tab.Snapshot -> snapshotTab() } }
```

State on a `@Routable object` page survives navigating away and back, because the page object is
a singleton — so captured output isn't lost when switching tabs.

If you are stuck with `shownWhen` for this, the content column inside it must itself be
`expanding`, or nothing below it will stretch.

### 10. Buttons have no `text` property — nest a `text {}` call

```kotlin
// ❌ WRONG - button has no .text property
button { text = "Click me" }

// ✅ CORRECT - nest text() inside button
button {
    text("Click me")
    onClick { /* ... */ }
}

// ✅ CORRECT - reactive button label
button {
    text { ::content { postReactive().authorName } }
    onClick { context.pageNavigator.navigate(UserProfilePage(postReactive().author)) }
}
```

### 11. Private helper functions need `ElementWriter.CanAddTheme` receiver

Helper functions that call layout/theme elements (`card`, `col`, `row`, `text`, `subtext`, etc.) need `ElementWriter.CanAddTheme` as receiver, not plain `ElementWriter`. Using `ElementWriter` causes "unresolved reference" errors for theme modifiers.

```kotlin
// ❌ WRONG - plain ElementWriter has no theme modifiers
private fun ElementWriter.postCard(postReactive: Reactive<Post>) {
    card.col { /* ... */ }  // 'card' unresolved!
}

// ✅ CORRECT
private fun ElementWriter.CanAddTheme.postCard(session: Reactive<UserSession?>, postReactive: Reactive<Post>) {
    card.col {
        text { ::content { postReactive().content } }
    }
}
```

### 12. `expanding` + `scrolling.col {}` in render() — wrap in a `col {}` first

When `expanding.scrolling.col {}` appears as a top-level statement in `render()`, the `expanding` modifier needs a parent flex container to work correctly. Wrap the whole page in a `col {}` first:

```kotlin
// ❌ Can fail — expanding has nowhere to expand into
override fun ElementWriter.CanAddTheme.render() {
    expanding.scrolling.col {
        forEachById(posts, id = { it._id }) { /* ... */ }
    }
}

// ✅ CORRECT — outer col provides the flex context
override fun ElementWriter.CanAddTheme.render() {
    col {
        card.col { h2("Feed") }
        expanding.scrolling.col {
            forEachById(posts, id = { it._id }) { /* ... */ }
        }
    }
}
```

### 13. Theme switches create backgrounds
```kotlin
// Creates background (switches from default to important)
important.button { text("Save") }

// ✅ Apply themes to containers, not individual elements
card.col {
    text("Title")
    text("Content")
}

// ❌ Avoid multiple theme switches
card.text("Title")
card.text("Content")  // Two separate cards!
```

---

## Recommended Imports - Use * imports (add to all KiteUI files)

```kotlin
import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.*      // Icon, KeyboardHints, rem, Align, etc.
import com.lightningkite.kiteui.navigation.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.reactive.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.reactive.lensing.validation.*
import com.lightningkite.serialization.*
import kotlinx.coroutines.*
```

`com.lightningkite.kiteui.models.*` is required for `Icon`, `KeyboardHints`, `rem`, and alignment constants — these are NOT in `kiteui.*` or `views.*`. Verified: both `Icon` and `KeyboardHints` are `data class` in `com.lightningkite.kiteui.models` (source: `library/src/commonMain/kotlin/com/lightningkite/kiteui/models/data.kt`).

### ⚠️ Imports the `*` star-imports do NOT pull in — add these explicitly

These must be added manually even when using the star-imports above. Missing any one of them produces confusing, unrelated-looking errors.

| Import | Why you need it |
|--------|----------------|
| `import com.lightningkite.reactive.context.invoke` | Read a `Reactive<T>` as a function (e.g. `session()`) inside `onClick {}` or any suspend lambda. Without it you get a cascade of ~20 unresolved-reference errors that don't mention this import. |
| `import com.lightningkite.reactive.context.reactive` | The `reactive {}` builder. `context.*` does NOT cover it. |
| `import com.lightningkite.kiteui.reactive.Action` | The `Action("label") { ... }` class for button `.action` assignments. |
| `import com.lightningkite.kiteui.views.l2.toast` | `context.toast("...")` helper. Lives in `l2`, not covered by `kiteui.*`. |
| `import com.lightningkite.kiteui.requestFile` | `context.requestFile(...)` for file pickers. |
| `import com.lightningkite.kiteui.exceptions.PlainTextException` | User-facing validation errors thrown in `Action` lambdas. Not in any star import. `throw PlainTextException("message", "Title")`. |
| `import com.lightningkite.kiteui.locale.renderToString` | `Instant.renderToString()` and `LocalDateTime.renderToString()` for human-readable date display. Lives in `kiteui.locale`, not covered by `kiteui.*`. |
| `import com.lightningkite.<yourpkg>.path` (+ individual path members) | KSP-generated path extensions live in the **model's package**. View files in a subpackage don't inherit them. Import both the `path` object and individual field accessors you use (e.g. `import com.lightningkite.lskiteuistarter.createdAt`). |

Quick recipe — add these to every view file that uses actions, toasts, file picking, or sorts:

```kotlin
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.l2.toast
import com.lightningkite.kiteui.requestFile
// Plus model path imports for your package:
import com.lightningkite.lskiteuistarter.path
import com.lightningkite.lskiteuistarter.createdAt  // (and other fields you use)
```

**Troubleshooting:** Many unresolved references inside `onClick {}` or `Action {}` that look like valid symbols → add `import com.lightningkite.reactive.context.invoke` first.

---

## Layout

```kotlin
// Containers
col { }                    // Vertical stack (flex-direction: column)
row { }                    // Horizontal (flex-direction: row)
frame { }                  // Z-stack (position: relative with absolute children)
rowCollapsingToColumn(70.rem) { }  // Horizontal if screen larger than input, vertical otherwise.  Vertical mode ignores weights.

// Modifiers chain BEFORE container
expanding.scrolling.card.col { }

// Sizing
expanding.someView                  // flex: 1 (takes available space)
weight(2f).someView                 // flex: 2
sizeConstraints(width = 20.rem, height = 10.rem).someView
sizeConstraints(minWidth = 10.rem, maxWidth = 30.rem).someView

// Frame positioning
frame {
    centered.text("Center")
    atTop.text("Top edge")
    atBottomEnd.text("Bottom right corner")
}

// Responsive
rowCollapsingToColumn(70.rem) { /* row on wide, col on narrow */ }
scrollingHorizontally.row { /* horizontal scroll */ }
```

---

## Reactivity

- `Reactive<T>` is a watchable, changing value
- `MutableReactive<T>` is one that can be changed with '.set(x)' (suspending)
- `Signal<T>(startingValue)` is a concrete implementation that can be changed with `.value = x`
- `reactive {}` runs the given block as often as dependencies change
- `someReactive()` will retrieve the value of a reactive AND set the dependency
- Reactives already hold their own loading / error states; using `reactive {}` in a view will automatically show loading states in UI if it's still being calculated
- `remember {}` is a reactive value that is calculated from the given reactive block, automatically recalculated when a dependency changes

```kotlin
// Mutable state
val count = Signal(0)
count.value = 5
count.value++

// Computed (auto-updates when dependencies change)
val doubled = remember { count() * 2 }

// Async data loading (preferred over Signal + launch)
val user = rememberSuspending {
    val session = currentSession() ?: return@rememberSuspending null
    session.users.get(userId).await()
}

// Reactive text binding
text { ::content { "Count: ${count()}" } }

// Two-way binding - binds two MutableReactive<T>
textInput { content bind email }

// Conditional visibility (PREFERRED over reactive creation / destruction of views)
shownWhen { showAdvanced() }.card.col { advancedSettings() }

// Reactive list rendering
val items = Signal(listOf("A", "B", "C"))
col {
    forEachById(items, id = { it } /*used to distinguish rows*/) { item: Reactive<String> ->
        card.text { ::content { item() } }
    }
}
recyclerView {  // handles its own scrolling, needs a size controlled by the outside.  Similar to Android's RecyclerView.
    children(items, id = { it } /*used to distinguish rows*/) { item: Reactive<String> ->
        card.text { ::content { item() } }
    }
}

// Reactive lens extensions
radioButton { checked bind selected.equalTo(1) }  // Radio groups
checkbox { checked bind tags.contains("featured") }  // Set membership
textInput { content bind nullableString.nullToBlank() }  // Null handling
numberInput { content bind ageStr.asInt() }  // String↔Number
```

---

## Components

```kotlin
// Text
h1("Heading"); h2("Subheading"); text("Body"); subtext("Muted")

// Buttons
button {
    text("Click")
    onClick { doSomething() }
}

// Button with async action (shows loading, handles errors automatically)
button {
    text("Save")
    action = Action("Save") {
        api.save(data())
        toast("Saved!")
    }
}

// Themed buttons
important.button { text("Primary") }
danger.button { text("Delete") }

// Inputs
textInput {
    hint = "Email"
    keyboardHints = KeyboardHints.email  // Don't forget this!  You need to tell it what kind of keyboard to use.
    content bind email
}
textArea {
    keyboardHints = KeyboardHints.email  // Don't forget this!  You need to tell it what kind of keyboard to use.
    content bind notes 
}

// Keyboard hint options
KeyboardHints.paragraph
KeyboardHints.title
KeyboardHints.id
KeyboardHints.integer
KeyboardHints.integerWithNegative
KeyboardHints.decimal
KeyboardHints.decimalWithNegative
KeyboardHints.phone
KeyboardHints.email
KeyboardHints.password
KeyboardHints.newPassword
KeyboardHints.oneTimeCode

// Selection
checkbox { checked bind isEnabled }
switch { checked bind isEnabled }
radioToggleButton { checked bind selected.equalTo(0) }
select { bind(selectedValue, options) { it } }

// Icons (Material Design paths, but a LIMITED subset — check source before assuming one exists)
// Available: home, search, add, delete, settings, done, arrowBack, chevronRight, chevronLeft,
//   menu, close, logout, login, moreHoriz, moreVert, deleteForever, remove, download, sync,
//   block, sort, filterList, star, starFilled, person, group, warning, send, chat, list,
//   notification, notificationFilled, email, certification, copy, lightMode, darkMode,
//   info, externalLink, expand, collapse, passkey, upload, dot, help
// ❌ No calendar/event icon (Icon.event does not exist) — use Icon.star or Icon.list as fallback
// Source: library/src/commonMain/kotlin/com/lightningkite/kiteui/models/data.kt
icon(Icon.home, "Home")
icon { source = AppIcons.custom; description = "Custom" }
icon(Icon.home.copy(width = 3.rem, height = 3.rem), "Home")  // Huge icon
```

### Date & time inputs

**Widget:** `localDateTimeField` — binds to `Signal<LocalDateTime?>` (from `kotlinx.datetime`).

```kotlin
import kotlinx.datetime.*
import com.lightningkite.kiteui.locale.renderToString  // for display

// 1. Declare local state as LocalDateTime?
val startsAt = Signal<LocalDateTime?>(null)

// 2. Render the picker (wrap in fieldTheme for consistent styling)
col {
    subtext("Date & Time")
    fieldTheme.localDateTimeField {
        content bind startsAt
    }
}

// 3. Convert to Instant for storage (models store Instant, not LocalDateTime)
val instant = startsAt() ?: throw PlainTextException("Pick a date and time", "Missing")
val stored: Instant = instant.toInstant(TimeZone.currentSystemDefault())

// 4. Convert back to LocalDateTime for editing
val forEdit: LocalDateTime = storedInstant.toLocalDateTime(TimeZone.currentSystemDefault())

// 5. Display Instant as human-readable string
subtext { ::content { event().startsAt.renderToString() } }
// renderToString() is an extension on Instant in com.lightningkite.kiteui.locale
// Signature: fun Instant.renderToString(size: RenderSize = RenderSize.Full, zone: TimeZone = TimeZone.currentSystemDefault(), ...): String
```

**Filtering/sorting by date:**
```kotlin
import kotlin.time.Clock  // for now()

val now = Clock.System.now()
val upcomingEvents = remember {
    session().events.list(
        Query(
            condition<Event> { it.startsAt gte now },
            orderBy = listOf(SortPart(Event.path.startsAt, ascending = true))
        )
    )()
}
```

Verified: CreateEventScreen.kt, EventsScreen.kt (demo/iter5-events); `LocalDateTimeField.kt` in KiteUI source (`content: MutableReactiveValue<LocalDateTime?>`); `LocalTimeRender.kt` (renderToString on Instant/LocalDateTime in `com.lightningkite.kiteui.locale`).

---

## Pagination & large lists (virtualized + grow-the-window)

Key facts (verified against KiteUI/Lightning Server source + demo/iter6-pagination):
- `Query<T>` is **offset paging only**: `skip` + `limit`. There is **no cursor/keyset paging**.
  A stable order is required for correct paging — sort by an `@Index`-ed field in every query.
- **`ModelCache.list(Query)` is NOT lazy** — it fetches *all* rows up to `limit` in one response.
  So the KiteUI idiom is **"grow the window"**: a `Signal<Int>` limit that increases by a page size.
- `recyclerView { children(reactive, id) { } }` **virtualizes** — only visible rows are in the view
  tree even with hundreds loaded. `lastIndex()` (reactive) gives the last visible index → trigger the
  next window when it nears the end (infinite scroll). A "Load more" button is the simple alternative.
- **`count()` is a `suspend` fn** on `ClientModelRestEndpoints` (`session.api.<model>.count(cond)`);
  there is **no ModelCache wrapper for count** → call it in `rememberSuspending { }`.

```kotlin
private const val PAGE_SIZE = 50

val limit = Signal(PAGE_SIZE)                 // grows as you scroll / press "Load more"
val search = Signal("")
val searchDebounced = search.debounce(400)

val contacts = remember {
    val session = currentSession() ?: return@remember emptyList<Contact>()
    val cond = if (searchDebounced().isBlank()) Condition.Always
               else condition<Contact> { it[Contact_name].contains(searchDebounced(), true) }
    session.contacts.list(Query(
        condition = cond,
        orderBy = listOf(SortPart(Contact.path[Contact_name], ascending = true)), // P2 + subscript (P1)
        skip = 0, limit = limit(),            // re-queries whenever limit() or search changes
    ))()
}

val total = rememberSuspending {              // count() is suspend → rememberSuspending, not remember
    val session = currentSession() ?: return@rememberSuspending 0
    session.api.contact.count(Condition.Always)
}

expanding.recyclerView {
    children(contacts, id = { it._id }) { row -> contactRow(row) }   // virtualized
    reactive {                                                       // infinite scroll
        val items = contacts(); val last = lastIndex()
        if (items.isNotEmpty() && last >= items.size - 3 && items.size < (total() ?: 0)) {
            launch { limit.value += PAGE_SIZE }
        }
    }
}
```

**Caveat (real framework limit):** grow-the-window loads everything up to the current `limit` in one
response, so it does NOT scale to very large windows (tens of thousands). For truly large data you'd
need cursor/keyset paging, which isn't built in today. Good for hundreds–low thousands on screen.

Verified: ContactBrowseScreen.kt, ContactEndpoints.kt (demo/iter6-pagination).

---

## Navigation

```kotlin
// Page definition
@Routable("users/{userId}")
class UserPage(val userId: String) : Page {
    override fun ViewWriter.render() {
        col { h1("User: $userId") }
    }
}

// Navigation
pageNavigator.navigate(UserPage("123"))
pageNavigator.goBack()
pageNavigator.replace(LoginPage)

// Links
link { text("Settings"); to = { SettingsPage } }
externalLink { text("Docs"); to = "https://example.com" }

// URL query parameters (persisted in URL)
@QueryParameter val searchQuery = Signal("")
```

---

## Dialogs

```kotlin
// Toast
toast("Success!")

// Confirm
val confirmed = confirm("Delete item?")
if (confirmed) deleteItem()

// Custom dialog
dialog { close ->
    card.col {
    card.col {
        h2("Title")
        text("Content")
        button { text("OK"); onClick { close() } }
    }
}

// Bottom sheet
openBottomSheet {
    col {
        h3 { content = "Options" }
        button { text("Option 1"); onClick { dismissBackground() } }
    }
}

// Destructive confirmation
confirmDanger("Delete", "Cannot be undone") { api.delete(id) }
```

---

## Theming

```kotlin
// Semantic modifiers
important.button { }  // Primary action
danger.button { }     // Destructive
warning.text("!")     // Warning
card.col { }          // Card background

// Make a labeled field — two options (see P4 in Critical Pitfalls)
// Option A: simple, no special import
col {
    subtext("Label")
    textInput { hint = "…"; content bind value }
}

// Option B: field() — requires explicit import com.lightningkite.kiteui.views.l2.field
// (fieldTheme + accessibility labelFor wired automatically)
field("Label") {
    textInput { hint = "…"; content bind value }
}

// Dynamic theme based on state
link {
    dynamicTheme {
        if (isSelected()) SelectedSemantic else null
    }
    text("Item")
}
```

---

## Error Handling

Actions automatically catch and display errors:

```kotlin
button {
    action = Action("Save") {
        // Errors thrown here show in dialog automatically
        api.save(data())
        // Custom error
        throw PlainTextException("Custom message", "Title")
    }
}

// Custom exception handler for a view tree
col {
    this += ExceptionToMessage<NetworkException>(priority = 5f) {
        ExceptionMessage(
            title = "Connection Error",
            body = "Check your internet connection."
        )
    }
    // Children inherit this handler
}
```

---

## Advanced Patterns

```kotlin
// Keyboard shortcuts
onKeyCode(keyCode { shortcut + it.letter('s') }) { saveAction.startAction(this) }

// Permission-based visibility
shownWhen { session().role != UserRole.Guest }.button { text("Admin") }

// Animated transitions
swapView(remember { if (advanced()) "adv" else "simple" }) { mode ->
    when (mode) { "adv" -> advancedUI(); "simple" -> simpleUI() }
}
```

---

## Resources

- **Live Example**: https://kiteui.cs.lightningkite.com/
- **Maven**: https://lightningkite-maven.s3.us-west-2.amazonaws.com
