# KiteUI ModelCache (Lightning Server Integration)

**CRITICAL: ALWAYS use ModelCache for Lightning Server APIs.** This is not a tradeoff - it's a straight improvement in every way. ModelCache handles request optimization, caching, consistency, and WebSocket integration automatically.

## Why ModelCache is Mandatory

ModelCache provides:
- **Zero extra network requests**: Caches prevent redundant fetches
- **Instant UI consistency**: A modification is immediately visible to all views
- **Automatic WebSocket integration**: Real-time updates when available, seamless fallback to polling
- **Request batching**: Multiple concurrent requests are automatically batched
- **Query caching with smart invalidation**: Query results update automatically when individual items change
- **Background polling with staleness tracking**: Fresh data without manual refresh logic

## Setup: Wrap All Endpoints

```kotlin
class UserSession(
    val api: Api  // Generated API from Lightning Server
) : CachedApi(api) {
    // CachedApi base class automatically wraps all endpoints with ModelCache
    // Each endpoint like api.user becomes a ModelCache<User, Uuid>
}

// Manual setup (if not using CachedApi base class):
class UserSession(
    val uncached: Api,
    val userId: Uuid
) {
    // Wrap each endpoint with ModelCache for client-side caching
    val users: ModelCache<User, Uuid> =
        ModelCache(uncached.user, User.serializer(), log = StoredLog())

    val projects: ModelCache<Project, Uuid> =
        ModelCache(uncached.project, Project.serializer())

    val tasks: ModelCache<Task, Uuid> =
        ModelCache(uncached.task, Task.serializer())
}
```

## How ModelCache Works

### Individual Item Caching (`cache[id]`)
- Returns a `ModelCacheItemReadable<T>` with reactive `.state` property
- First access triggers background fetch (waits for WebSocket connection if available)
- Subsequent accesses return cached value instantly (if fresh)
- Automatically polls for updates at configurable `pullFrequency` (default: 60 seconds)
- If WebSocket is active, skips polling and relies on real-time updates
- All modifications via `cache[id].modify()` or `cache[id].set()` update cache instantly

### Query Caching (`cache.list()`)
- Returns a `ModelCacheLimitReadable<T>` with reactive `.state` property
- Query results are cached with timestamp and requested limit
- Intelligently reconstructs query results when individual items are modified
- Example: If you query "all active users" and one user becomes inactive, the cache automatically removes them from the result
- WebSocket updates automatically invalidate/update matching queries
- Dynamic limit support: changing `.limit` property automatically fetches more items if needed

### Update Pipeline
1. Call `cache[id].modify(modification)` or `cache.add(item)`
2. API request sent to server, returns updated item
3. Updated item flows through `newData` signal (central update pipeline)
4. `newData` triggers:
   - Individual item cache update (`lastIndividualValues`)
   - Query cache reconstruction (`ListReconstructionCalculator`)
   - All reactive listeners fire (UI updates automatically)
5. Result: Every view watching that item or any matching query sees the change instantly

### WebSocket Integration
- When `pullFrequency < 30 seconds`, ModelCache prefers WebSocket over polling
- Cache tracks which conditions are actively being watched
- Server sends change notifications: `{updates: [items], remove: [ids]}`
- Changes flow through same `newData` pipeline as manual modifications
- On socket overload (server can't keep up), cache clears everything and refetches (safety mechanism)
- Fallback: if socket fails to connect within 5 seconds, falls back to immediate fetch + polling

### Request Batching
- Multiple concurrent `cache[id1]`, `cache[id2]`, `cache[id3]` calls batch into single query: `_id inside [id1, id2, id3]`
- Reduces network round trips from N to 1
- Queries execute in parallel but don't merge (each query is separate, but concurrent)

## Usage Patterns

### Individual Item (reactive access)
```kotlin
val userId = Uuid.parse("...")
val user = remember { session.users[userId]() }  // Reactive<User?>

text { ::content { user()?.name ?: "Loading..." } }
```

### Individual Item (with polling configuration)
```kotlin
// Custom staleness and polling
val user = session.users.item(
    id = userId,
    maximumAge = 30.seconds,     // Data valid for 30 seconds
    pullFrequency = 60.seconds   // Poll every minute
)

text { ::content { user()?.name ?: "Loading..." } }
```

### Query (reactive list)
```kotlin
val activeUsers = remember {
    session.users.list(
        query = Query(condition { it.active eq true }),
        maximumAge = 10.seconds,
        pullFrequency = 30.seconds
    )()  // Call operator returns Reactive<List<User>>
}

forEach(activeUsers) { user ->
    text { ::content { user().name } }
}
```

### Modifying cached items
```kotlin
// Modify via cache - updates all views automatically
launch {
    session.users[userId].modify(
        Modification.assign(User::name, "New Name")
    )
    // All views watching this user see the change instantly
}

// Alternative: set entire value (uses diffing to send only changed fields)
launch {
    val user = session.users[userId].awaitOnce()
    session.users[userId].set(user.copy(name = "New Name"))
}

// Insert new item — .add() returns the inserted T directly
launch {
    val newUser = User(_id = Uuid.random(), name = "Alice", ...)
    session.users.add(newUser)
    // Cache updated, all matching queries update automatically
}
```

### Complex reactive composition
```kotlin
// Self-reference (current user)
val self = remember { session.users[session.userId]()!! }

// Derived reactive values
val currentOrgId = Signal<Uuid?>(null)

// Query based on reactive signal
val myOrganizations = remember {
    val orgIds = self().organizationIds
    session.organizations.list(
        query = Query(condition { it._id inside orgIds })
    )()
}

// Nested reactive (flattened)
val currentOrg = remember {
    currentOrgId()?.let { session.organizations[it]() }
}.flatten()  // Reactive<Reactive<Org?>> -> Reactive<Org?>
```

## Anti-Pattern: Direct API Calls for Data Display

```kotlin
// ❌ WRONG for data display: Bypasses cache, no reactivity
launch {
    val users = session.api.user.query(Query(...))  // Direct API call
    // Problem: Not cached, not reactive, every call fetches from server
}

// ❌ WRONG: Manual cache invalidation needed, UI doesn't update automatically
val users = Signal<List<User>>(emptyList())
launch {
    users.value = session.api.user.query(Query(...))
    // Problem: If another screen modifies a user, this list is stale
}

// ✅ RIGHT for CRUD: Use ModelCache
val users = remember {
    session.users.list(Query(...))()  // Cached, reactive, auto-updates
}

// ✅ RIGHT for non-CRUD actions: Direct API is fine
launch {
    session.api.door.unlock(doorId)  // Action, not data query
    session.api.notification.sendPush(message)  // Fire-and-forget
}
```

## Loading Data Pattern

Use `Signal<T?>(null)` + `.get(id).await()` for loading individual items:

```kotlin
@Routable("detail/{itemId}")
class DetailScreen(val itemId: Uuid) : Page {
    override fun ViewWriter.render() {
        val item = Signal<Item?>(null)

        // Load data on screen creation
        launch {
            val session = currentSession.await() ?: return@launch
            val loaded = session.items.get(itemId).await()
            item.value = loaded
        }

        // UI automatically shows loading state until item is set
        reactive {
            val i = item() ?: return@reactive  // Labeled return
            h1 { content = i.name }
            text { content = i.description }
        }
    }
}
```

## Modifying Data Pattern

Use bracket notation `cache[id].modify()` for modifications:

```kotlin
// Modify a single field
session.users[userId].modify(modification {
    it.name assign "New Name"
})

// Modify multiple fields
session.reservations[reservationId].modify(modification {
    it.checkedInAt assign kotlin.time.Clock.System.now()
    it.beforeImages assign imageSet
})

// Modify based on current value
session.counters[counterId].modify(modification {
    it.count.increment(1)
})
```

**Common modification operators:**
- `assign` - Set field to value
- `increment` / `decrement` - Math operations
- `ListAppend` / `ListRemove` - List operations
- `SetAppend` / `SetRemove` - Set operations

## Inserting items: use `.add()`, not `.insert()`

`.add(item): T` is the current insert method on `ModelCacheLike` — it returns the **inserted item** (`T`) directly, with any server-generated fields populated.

`.insert(item)` is `@Deprecated("Use add instead")` and returns a `ModelCacheItemReadable<T>` wrapper — **not** the item. Do not use `.insert()` in new code.

Because you must supply `_id` yourself, the simplest pattern is to construct the model with its `_id` before calling `.add()`, then reuse that id for navigation:

```kotlin
// ❌ WRONG — .insert() is deprecated; its return value is a readable wrapper with no ._id
val result = session.listings.insert(listing)
pageNavigator.replace(DetailPage(result._id.toString()))  // Compile error

// ✅ CORRECT — .add() returns the item; construct _id before inserting
val listing = Listing(
    _id = Listing.ID(Uuid.random()),
    title = titleVal,
    // ...
)
session.listings.add(listing)
pageNavigator.replace(DetailPage(listing._id.toString()))  // works
```

Verified in: `ModelCacheLike.kt` in lightning-server-kiteui (`.add()` declared; `.insert()` has `@Deprecated`); iter4 TaskBoardScreen.kt uses `.add()`.

## Server-Side Search Pattern

**⚠️ CRITICAL: Always use server-side filtering for search, NOT client-side `.filter()`**

```kotlin
@Routable("members")
object MembersScreen : Page {
    override val title: Reactive<String> = Constant("Members")

    // URL-persisted search state
    @QueryParameter
    val searchQuery = Signal("")

    // Debounce to prevent excessive API calls while typing
    val searchQueryDebounced = searchQuery.debounce(500)

    // Server-side filtered query
    val searchedUsersMeta = remember {
        val q = searchQueryDebounced()
        currentSessionNotNull().users.list(
            Query(condition {
                if(q.isEmpty()) Condition.Always
                else Condition.And(q.split(' ').map { p ->
                    it.name.contains(p, ignoreCase = true)
                })
            })
        )
    }
    val searchedUsers = remember { searchedUsersMeta()() }

    override fun ViewWriter.render() {
        col {
            field("Search") {
                textInput { content bind searchQuery }
            }

            expanding.frame {
               shownWhen { searchedUsers().isEmpty() }.centered.text {
                    content = "No matching users found."
                }

                recyclerView {
                    children(searchedUsers, id = { it._id }) { user ->
                        card.row {
                            text { ::content { user().name } }
                            subtext { ::content { user().email } }
                        }
                    }
                }
            }
        }
    }
}
```

**Why server-side filtering?**
- **Performance**: Only loads matching records, not entire dataset
- **Scalability**: Works with thousands/millions of records
- **Network efficiency**: Minimal data transfer
- **Database optimization**: Uses database indexes for fast searches

## Multi-Filter Pattern with `Condition.andNotNull()`

<!-- by Claude -->
When filtering list data based on multiple optional UI filters, use `Condition.andNotNull()` with `?.let {}` for clean server-side filtering:

```kotlin
val selectedTags = Signal<Set<String>>(emptySet())
val projectFilter = Signal<Project?>(null)
val dateFrom = Signal<LocalDate?>(null)

val items = remember {
    val s = currentSession() ?: throw PlainTextException("Not logged in")
    val selected = selectedTags()
    val project = projectFilter()
    val from = dateFrom()

    s.items.list(
        query = Query(
            condition = condition<MyItem> {
                Condition.andNotNull(
                    // Only add each filter when its Signal has a value
                    selected.takeIf { it.isNotEmpty() }?.let { tags ->
                        it.tags any { tag -> tag inside tags }
                    },
                    project?.let { p -> it.project eq p._id },
                    from?.let { d -> it.date gte d },
                )
            },
            limit = 50
        ),
        maximumAge = 5.minutes,
        pullFrequency = 5.minutes
    )
}
```

**Key points:**
- `Condition.andNotNull()` combines non-null conditions with AND, ignoring nulls
- `filterValue()?.let { condition }` — only adds the condition when the filter is active
- Reading Signals inside `remember { }` creates reactive dependencies — when a filter changes, the query re-executes
- Server returns only matching results (efficient, scalable)
- Common operators: `eq`, `inside`, `gte`/`lte`, `contains`, `any { }`

## Required Imports

```kotlin
import com.lightningkite.reactive.context.*      // Provides .await() extension
import com.lightningkite.reactive.core.*          // Signal, Reactive, remember
import com.lightningkite.reactive.extensions.*    // Reactive utilities
import kotlinx.coroutines.launch                  // For async operations

// For search patterns
import com.lightningkite.kiteui.QueryParameter
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.reactive.extensions.debounce
import com.lightningkite.services.database.Condition
import com.lightningkite.services.database.Query
import com.lightningkite.services.database.condition
import com.lightningkite.services.database.contains
```

## Troubleshooting

**"Unresolved reference: await"**
→ Missing `import com.lightningkite.reactive.context.*`

**"Return is prohibited here"**
→ Use labeled return: `return@reactive` or `return@launch`

**"Unresolved reference: name" (in modification block)**
→ Don't import field paths at top level - they're auto-provided in `modification {}`

**"Type mismatch: required Modification<T>, found Unit"**
→ You're in a `modification {}` block - use `assign` not `=`
```kotlin
// ❌ Wrong
modification { it.name = "value" }

// ✅ Correct
modification { it.name assign "value" }
```

## Common Gotchas

**Socket Activation Timing:**
If WebSocket activates AFTER data is cached, you might miss changes. ModelCache checks activation timestamps to avoid applying stale socket updates.

**Limited Query Deletions:**
When items are deleted from a limited query (e.g., `limit = 10`), the cache doesn't automatically fetch more items to fill the gap. You may end up with 9 items instead of 10 until next poll.

**Bulk Modifications:**
`cache.bulkModify()` clears ALL caches because it can't know which items were affected. This is intentionally aggressive to avoid serving stale data.

**Denormalized fields look stale on detail pages after cross-table mutations:**
When a server-side `registerChangeListener` mutates a denormalized field on model A in response to a change on model B (e.g., `Event.goingCount` incremented when an `EventRsvp` is created), the individual item read `session().events[eventId]()` does **not** receive a WebSocket push for that update. The ModelCache item for the event is not notified because the event itself was not the target of any client-visible write — the RSVP endpoint triggered it server-side.

The result: the detail page shows a stale `goingCount` until the cache's next poll (default 60 seconds) or a page reload.

A **list query subscription** (`session().events.list(Query(...))()` on an endpoint with `ModelRestUpdatesWebsocket`) **does** receive the updated event and shows the correct count.

**Workaround options:**
1. On the list page: use `ModelRestUpdatesWebsocket` — the list subscription refreshes automatically and the correct value appears there.
2. On the detail page: add a manual refresh button, or reduce `pullFrequency` on that specific item (e.g., `session().events.item(id, pullFrequency = 10.seconds)()`).
3. Architectural: avoid denormalized counts on detail pages if freshness is critical; re-query the source table instead (e.g., count RSVPs client-side from the RSVP list).

Verified: observed in iter-5 events demo (struggle-log.md issue #6) — RSVP creation incremented `goingCount` server-side via `registerChangeListener`; list page updated correctly, detail page was stale.

## Summary

**Use ModelCache for CRUD operations** (queries, gets, modifications) - it provides caching, reactivity, and automatic UI updates.

**Direct API calls are appropriate for:**
- Non-CRUD actions (e.g., `api.door.unlock()`, `api.email.send()`)
- One-off operations where result is never displayed (e.g., logging events)
- Operations that don't return data models (e.g., file uploads, health checks)
- Server-side code

For data display and CRUD, ModelCache is the correct, optimized, production-ready solution.
