# ModelCache (Lightning Server data in KiteUI)

Full chapter: `docs/model-cache.md`.  ModelCache is in **lightning-server-kiteui** (`com.lightningkite.lightningserver:client`, package `com.lightningkite.lightningserver.db`), not KiteUI.  Query/Condition/Modification come from Service Abstractions (`com.lightningkite.services.database.*`).

**Use ModelCache for all model reads and writes.**  Direct API calls (`session.api.x.y()`) are only for non-CRUD endpoints, and for `count`.

## Setup

The generated SDK has `CachedApi` with one `open val todos = ModelCache(uncached.todo, Todo.serializer())` per model.  The session extends it:

```kotlin
class UserSession(val api: Api, val userId: Uuid) : CachedApi(api)
val currentSession: Reactive<UserSession?> = rememberSuspending { /* build from token */ }
```

## Read

```kotlin
val todo = remember { session().todos.item(id)() }                 // Reactive<Todo?>; null = doesn't exist
val open = remember {
    session().todos.list(Query(orderBy = sort { it.title.ascending() }, limit = 50) { it.done eq false })()
}                                                                   // Reactive<List<Todo>>
colOf(open, id = { it._id }) { t -> text { ::content { t().title } } }

// Infinite scroll: pass the un-invoked list (Reactive<Reactive<List<T>>>) to recyclerView
val all = remember { session().todos.list(Query(limit = 50) { it.done eq false }) }
expanding.recyclerView { children(items = all) { t -> text { ::content { t().title } } } }   // import com.lightningkite.lightningserver.db.children

// "Load more" button
val list = remember { session().todos.list(Query(limit = 50)) }.flatten()
button { text("More"); onClick { list.limit(list.limit + 50) } }   // suspends until loaded

// Count: not cached; suspend → rememberSuspending
val total = rememberSuspending { session().api.todo.count(Todo.done eq false) }
```

Freshness: `item(id, maximumAge = Duration.INFINITE, pullFrequency = 0.seconds)`, same for `list`.  `pullFrequency` < 30 s with an update socket on the server = live subscription instead of polling.

## Write (all suspend; call in `onClick`/`Action`)

```kotlin
val created = session().todos.add(Todo(title = "x"))                       // returns T
session().todos.item(id).modify(Modification { it.done assign true })
session().todos.item(id).set(updated)                                      // sends diff; null deletes
session().todos.item(id).delete()
session().todos.bulkModify(MassModification(condition, modification))      // clears whole cache
```

Every list and item reading the changed row updates immediately.

## Conditions and sorts (Service Abstractions 1.4+)

```kotlin
Query(limit = 50) { it.name.contains(q, ignoreCase = true) }               // lambda form
Query(condition = Todo.done eq false)                                      // fields are paths
Condition<Todo> { it.done eq false }                                       // was condition<T> { }
Modification<Todo> { it.done assign true }                                 // was modification<T> { }
sort<Todo> { it.createdAt.descending() }                                   // or listOf(SortPart(Todo.createdAt, ascending = false))
Condition.Always / Condition.Never                                         // "no filter"; not Condition<T> { true }
Condition.andNotNull(a()?.let { v -> ... }, b()?.let { v -> ... })         // optional filters
```

`Model_field` and `Model.path[...]` are gone; write `Todo.title`.  From another package, `import com.example.models.title`.

## Search

```kotlin
val search = Signal("")
val debounced = search.debounce(400.milliseconds)         // in render scope (needs a CoroutineScope)
val results = remember(useLastWhileLoading = true) {
    val q = debounced()
    session().users.list(Query(limit = 50) { if (q.isBlank()) Condition.Always else it.name.contains(q, true) })()
}
```

## Bind a select to a server field

```kotlin
val status = remember { todo()?.status ?: Status.Open }.withWrite { v ->      // com.lightningkite.reactive.extensions.withWrite
    session().todos.item(id).modify(Modification { it.status assign v })
}
select { bind(status, Constant(Status.entries)) { it.name } }
```

For edit forms: copy into a local `Signal`, bind fields with `draft.lens(User.name)` (`com.lightningkite.optics.lens`), save with `item.modify(modification(old, new) ?: return@Action)` (`com.lightningkite.lightningdb.modification`).

## Gotchas

- `list()` throws on `skip != 0` or `limit <= 0`.  Page with `limit(n)`.
- Deprecated: `cache[id]` / `query(q)` (silently poll every minute), `watch(...)`, `insert(...)` (use `add`), `limit = n` setter (use `limit(n)`).
- Never `remember { all().filter { } }`: filter in the query.
- `ModelCacheItemReadable` inside `remember { session().x.item(id) }` is double-wrapped: read with `item()()`.
- Add `context.exceptionHandlers.installLsError()` at the app root so `LsErrorException` gets a readable message (it shows the server's message, else a sentence for the status).
