# Server data with ModelCache

`ModelCache` is the client-side copy of one Lightning Server collection.  Ask it for an item or a query and you get a `Reactive` that loads, stays current (by polling or the server's update socket), and is shared with every screen reading overlapping data.  Writes go through it too, so every view showing the changed record updates without a refetch.

ModelCache is not part of KiteUI.  It lives in [lightning-server-kiteui](https://github.com/lightningkite/lightning-server-kiteui), whose own docs cover it in depth:

```kotlin
commonMain.dependencies {
    implementation("com.lightningkite.lightningserver:client:<version>")
}
```

Use it for reading and writing models.  Call the API directly (`session.api.door.unlock(id)`) only for endpoints that aren't model CRUD.

## Minimal example

```kotlin
@Routable("/todos")
object TodoListPage : Page {
    override fun ElementWriter.CanAddTheme.render() {
        val todos = remember {
            currentSessionNotNull().todos.list(Query(orderBy = sort { it.createdAt.descending() }) { it.done eq false })
        }
        expanding.recyclerView {
            children(items = todos) { todo ->          // com.lightningkite.lightningserver.db.children
                card.text { ::content { todo().title } }
            }
        }
    }
}
```

`todos` is a `Reactive` of the cache's live list: the outer layer follows the session, the inner one is the list.  This `children` overload loads more rows as the user scrolls toward the end.

## Common tasks

### Set up the caches

The SDK that Lightning Server generates for your server includes a `CachedApi` class with one `ModelCache` per model.  Your session class extends it:

```kotlin
class UserSession(val api: Api, val userId: Uuid) : CachedApi(api)

// generated:
open class CachedApi(val uncached: Api) {
    open val todos = ModelCache(uncached.todo, Todo.serializer())
}
```

Keep the current session in a `Reactive<UserSession?>` so pages re-read their data when the user signs in or out.

### Read one item

```kotlin
val todo = remember { session().todos.item(todoId)() }     // Reactive<Todo?>
text { ::content { todo()?.title ?: "Deleted" } }
```

`item(id)` reads `null` when the item doesn't exist.  Lookups made at the same moment are batched into one request.

### Read a query

```kotlin
val open = remember {
    session().todos.list(Query(orderBy = sort { it.title.ascending() }, limit = 50) { it.done eq false })()
}
colOf(open, id = { it._id }) { todo ->
    text { ::content { todo().title } }
}
```

`Query(orderBy, limit) { condition }` builds the condition from a lambda; `Query(condition = Todo.done eq false)` works too, since generated fields (`Todo.done`) are paths.  Combine conditions with `and`, `or`, `!`, or `Condition.andNotNull(...)`, which ignores `null` parts:

```kotlin
Query(limit = 50) {
    Condition.andNotNull(
        tag()?.let { t -> it.tags any { x -> x eq t } },
        since()?.let { d -> it.createdAt gte d },
    )
}
```

### Search as the user types

```kotlin
val search = Signal("")
val searchDebounced = search.debounce(400.milliseconds)
val results = remember(useLastWhileLoading = true) {
    val q = searchDebounced()
    session().users.list(Query(limit = 50) {
        if (q.isBlank()) Condition.Always else it.name.contains(q, ignoreCase = true)
    })()
}
```

Filter on the server, in the query.  `useLastWhileLoading` keeps the previous results on screen while the next ones load.

### Load more

```kotlin
val todos = remember { session().todos.list(Query(limit = 50) { it.done eq false }) }.flatten()
button {
    text("Load more")
    onClick { todos.limit(todos.limit + 50) }
}
```

`limit(n)` suspends until the extra rows arrive, fetching only the missing ones.  `flatten()` turns the session-wrapped `Reactive<ModelCacheLimitReadable<T>>` into a `ModelCacheLimitReadable<T>`.  `recyclerView`'s `children(items = ...)` overload does this for you.

### Freshness

```kotlin
session.orders.list(query, maximumAge = 1.minutes, pullFrequency = 10.seconds)
session.orders.item(id, pullFrequency = 30.seconds)
```

| Parameter | Default | Meaning |
|---|---|---|
| `maximumAge` | `Duration.INFINITE` | How old cached data may be to show it at once when a screen opens. |
| `pullFrequency` | `0.seconds` (never) | How often to refetch while the result is on screen. |

If the server provides an update socket for the model and `pullFrequency` is under 30 seconds, the cache subscribes instead of polling.

### Write

```kotlin
onClick {
    val created = session().todos.add(Todo(title = title()))                     // returns the server's copy
    session().todos.item(id).modify(Modification { it.done assign true })
    session().todos.item(id).delete()
    context.pageNavigator.navigate(TodoPage(created._id))
}
```

| Call | Does |
|---|---|
| `add(item)`, `addAll(items)` | Insert.  Returns the inserted items. |
| `upsert(item)` | Insert or replace by `_id`. |
| `item(id).modify(modification)` | Partial update. |
| `item(id).set(value)` | Sends only the fields that differ; inserts if missing, deletes if `null`. |
| `item(id).delete()` | Delete. |
| `bulkModify(MassModification(condition, modification))` | Server-side bulk update.  Clears the whole cache afterward. |
| `item(id).invalidate()`, `totallyInvalidate()` | Forget and refetch. |

Every query the change affects updates at once.

### Bind a control to a server field

```kotlin
val status = remember { todo()?.status ?: TodoStatus.Open }.withWrite { newStatus ->   // com.lightningkite.reactive.extensions
    session().todos.item(todoId).modify(Modification { it.status assign newStatus })
}
select { bind(status, Constant(TodoStatus.entries)) { it.name } }
```

`withWrite` turns a `Reactive<T>` into a `MutableReactive<T>` by supplying what a write does.  For a whole edit form, edit a local copy and save the difference instead.  See [Forms](forms.md).

### Count

```kotlin
val total = rememberSuspending { session().api.todo.count(Todo.done eq false) }
```

`ModelCache` doesn't cache counts.  Call the endpoint directly from `rememberSuspending`, since it suspends.

## Gotchas

- **`skip` is rejected.**  `list()` throws if `query.skip != 0` or `query.limit <= 0`.  Page with `limit(n)`.
- **`get(id)` and `query(q)` are deprecated.**  They poll once a minute without saying so.  Use `item(id)` and `list(q)` and pass `pullFrequency` when you want polling.  `insert` is deprecated too: use `add`, which returns the item rather than a reader.
- **Don't filter on the client.**  `remember { all().filter { ... } }` downloads every row.  Put the filter in the query.
- **Read inside reactive code.**  `session()` and `item(id)()` are reactive reads: call them inside `remember { }`, `::content { }`, `reactive { }`, or an action.
- **Server errors.**  Add `context.exceptionHandlers.installLsError()` at the app root to turn `LsErrorException` into readable messages.  See [Error handling](error-handling.md).
- **Generated fields are paths.**  Since Service Abstractions 1.4, write `Todo.title`, not `Todo.path.title` (deprecated) or `Todo_title` (removed).  From another package, import them by field name: `import com.example.models.title`.

## See also

- [Reactivity](reactivity.md): `remember`, `rememberSuspending`, loading states
- [Lists](lists.md): `colOf`, `recyclerView`
- [Error handling](error-handling.md)
