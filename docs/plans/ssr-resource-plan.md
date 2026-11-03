# SsrResource Implementation Plan

**Status:** Approved
**Created:** 2025-12-22
**Replaces:** `SsrPreloadable` interface

## Overview

`SsrResource<T>` is a reactive primitive for SSR data loading that:
1. Integrates with KiteUI's reactive system (`Reactive<T>`)
2. Automatically serializes data into HTML for hydration
3. Auto-detects completion (no explicit `preload()` function needed)
4. Uses non-nullable `T` (loading state handled by `ReactiveState`)

## Design Decisions

| Decision | Choice |
|----------|--------|
| Base interface | `Reactive<T>` (from `com.lightningkite.reactive.core`) |
| Await method | Use built-in `suspend fun Reactive<T>.await()` |
| Render strategy | Single pass - structure self-updates when resources load |
| Key parameter | Explicit `String` only (user's responsibility for uniqueness) |
| Serialization | `@Serializable` required on resource types |
| Error handling | Fail entire SSR render if any resource fails |
| Caching | None - fresh fetch on each navigation |
| Nullability | Non-nullable `T` - use `ReactiveState` for loading/error states |

## `SsrResource<T>` Class

```kotlin
// In commonMain
package com.lightningkite.kiteui.ssr

import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.ReactiveState
import com.lightningkite.reactive.core.Property
import kotlinx.coroutines.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

class SsrResource<T : Any>(
    val key: String,
    private val serializer: KSerializer<T>,
    private val loader: suspend () -> T
) : Reactive<T> {

    private val _state = Signal<ReactiveState<T>>(ReactiveState.notReady)
    private var loadJob: Job? = null

    override val state: ReactiveState<T>
        get() = _state.state.getOrNull() ?: ReactiveState.notReady

    override fun addListener(listener: () -> Unit): () -> Unit = _state.addListener(listener)

    val isLoaded: Boolean get() = state.ready

    /** Start loading (non-blocking) */
    fun startLoading(scope: CoroutineScope) {
        if (loadJob != null) return
        loadJob = scope.launch {
            try {
                val result = loader()
                _state.value = ReactiveState(result)
            } catch (e: Exception) {
                _state.value = ReactiveState.exception(e)
            }
        }
    }

    /** Initialize from serialized SSR data (hydration) */
    fun hydrateFrom(json: String) {
        val value = Json.decodeFromString(serializer, json)
        _state.value = ReactiveState(value)
    }

    /** Serialize current value. Throws if not loaded. */
    fun serialize(): String {
        val value = state.getOrNull()
            ?: throw IllegalStateException("Cannot serialize unloaded resource '$key'")
        return Json.encodeToString(serializer, value)
    }
}
```

## Factory Function

```kotlin
// In commonMain (or jvmSsrMain if SSR-specific)
inline fun <reified T : Any> ViewWriter.ssrResource(
    key: String,
    noinline loader: suspend () -> T
): SsrResource<T> {
    val resource = SsrResource(
        key = key,
        serializer = serializer<T>(),
        loader = loader
    )

    // Register and start loading during SSR
    context.ssrContext?.registerResource(resource)

    // Check for hydration data on client
    HydrationContext.getData(key)?.let {
        resource.hydrateFrom(it)
    }

    return resource
}
```

## SsrContext Changes

```kotlin
class SsrContext(
    val basePath: String = "/",
    // ... existing fields
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val resources = mutableMapOf<String, SsrResource<*>>()

    fun registerResource(resource: SsrResource<*>) {
        resources[resource.key] = resource
        resource.startLoading(scope)  // Start loading immediately
    }

    /** Await all resources using built-in Reactive.await() */
    suspend fun awaitAllResources() {
        resources.values.forEach { resource ->
            resource.await()  // Uses reactive library's await - throws on error
        }
    }

    fun exportResourceData(): Map<String, String> {
        return resources.mapValues { (_, resource) -> resource.serialize() }
    }
}
```

## SsrRouter Changes (Single-Pass Render)

```kotlin
class SsrRouter(/* existing params */) {

    suspend fun renderPageWithPreload(page: Page): String {
        val context = SsrContext(basePath)
        context.title = page.title.state.getOrNull()

        // Single render - creates reactive structure with bindings
        val result = context.render {
            theme.onNext.col {
                with(page) { render() }
            }
        }

        // Await all discovered resources
        // Reactive bindings auto-update the FutureElement tree
        context.awaitAllResources()

        // Serialize the now-complete tree + resource data
        return document.render(result, context.exportResourceData())
    }
}
```

## SsrDocument Changes

```kotlin
class SsrDocument(/* existing params */) {

    fun render(
        result: SsrResult,
        resourceData: Map<String, String> = emptyMap()
    ): String = buildString {
        appendLine("<!DOCTYPE html>")
        appendLine("<html lang=\"$lang\">")
        appendLine("<head>")
        // ... existing head content ...
        appendLine("</head>")
        appendLine("<body>")
        appendLine(result.html)

        // Inject SSR resource data for hydration
        if (resourceData.isNotEmpty()) {
            val jsonData = Json.encodeToString(resourceData)
            appendLine("<script id=\"__SSR_DATA__\" type=\"application/json\">")
            appendLine(escapeScriptContent(jsonData))
            appendLine("</script>")
        }

        appendLine(additionalBodyContent)
        appendLine("</body>")
        appendLine("</html>")
    }

    private fun escapeScriptContent(json: String): String {
        return json.replace("</", "<\\/")
    }
}
```

## Client Hydration Context

```kotlin
// In jsMain
package com.lightningkite.kiteui.ssr

import kotlinx.browser.document
import kotlinx.serialization.json.Json

object HydrationContext {
    private var data: Map<String, String>? = null

    /** Initialize from DOM on page load */
    fun initFromDom() {
        val script = document.getElementById("__SSR_DATA__")
        if (script != null) {
            data = Json.decodeFromString(script.textContent ?: "{}")
        }
    }

    /** Get resource data by key */
    fun getData(key: String): String? = data?.get(key)

    /** Clear after hydration complete */
    fun clear() {
        data = null
    }
}
```

## SSR Render Flow

```
1. Router calls context.render { page.render() }
   └─> Creates FutureElement tree with reactive bindings
   └─> ssrResource() calls register resources and start loading

2. Router calls context.awaitAllResources()
   └─> Each resource.await() suspends until loaded
   └─> On load, ReactiveState changes from notReady to Ready(value)
   └─> Reactive bindings auto-update FutureElement content

3. Router calls document.render(result, resourceData)
   └─> FutureElement tree is now fully populated
   └─> Serialize tree to HTML
   └─> Inject __SSR_DATA__ script with serialized resource values
```

## Client Hydration Flow

```
1. Page loads with server-rendered HTML + __SSR_DATA__ script

2. Client JS calls HydrationContext.initFromDom()
   └─> Parses __SSR_DATA__ into memory

3. Client renders page
   └─> ssrResource() checks HydrationContext.getData(key)
   └─> If found, calls resource.hydrateFrom(json)
   └─> Resource immediately has Ready state - no fetch needed

4. Page is interactive with pre-loaded data
```

## Usage Example

```kotlin
@Serializable
data class User(val id: String, val name: String, val email: String)

@Serializable
data class Post(val id: String, val title: String, val content: String)

@Routable("users/{id}")
class UserPage(val id: String) : Page {

    override fun ViewWriter.render() = col {
        // Explicit string keys - user ensures uniqueness
        val user = ssrResource("user-$id") { api.fetchUser(id) }
        val posts = ssrResource("posts-$id") { api.fetchUserPosts(id) }

        // Non-nullable T - use ReactiveState for loading handling
        h1 { ::content { user().name } }

        text { ::content { "Email: ${user().email}" } }

        h2("Recent Posts")

        // Can also handle states explicitly
        text {
            ::content {
                posts.state.handle(
                    success = { "${it.size} posts" },
                    notReady = { "Loading posts..." },
                    exception = { "Failed to load posts" }
                )
            }
        }

        // When ready, iterate
        posts().forEach { post ->
            col {
                h3(post.title)
                text(post.content)
            }
        }
    }
}
```

## Files to Create/Modify

### New Files
- `library/src/commonMain/kotlin/com/lightningkite/kiteui/ssr/SsrResource.kt`
- `library/src/jsMain/kotlin/com/lightningkite/kiteui/ssr/HydrationContext.kt`

### Modify
- `library/src/jvmSsrMain/kotlin/com/lightningkite/kiteui/ssr/SsrContext.kt` - Add resource tracking
- `library/src/jvmSsrMain/kotlin/com/lightningkite/kiteui/ssr/SsrRouter.kt` - Single-pass render with await
- `library/src/jvmSsrMain/kotlin/com/lightningkite/kiteui/ssr/SsrDocument.kt` - Inject `__SSR_DATA__`

### Remove (after migration)
- `library/src/jvmSsrMain/kotlin/com/lightningkite/kiteui/ssr/SsrPreloadable.kt`
- `example-app/src/jvmSsrMain/kotlin/com/lightningkite/mppexampleapp/SsrDataExamplePage.kt` (or update to use SsrResource)

## Behavior Summary

| Scenario | Behavior |
|----------|----------|
| **SSR** | Render discovers resources → await all → structure auto-updates → serialize |
| **Hydration** | Parse `__SSR_DATA__` → resources load from cache → no fetch |
| **Client navigation** | No SSR data → resources fetch normally → reactive updates |
| **Resource failure** | `await()` throws → SSR render fails → HTTP 500 |

## Notes

- `@Serializable` annotation required on all types used with `ssrResource`
- Keys must be unique per page - user's responsibility
- Resources are not cached across navigations
- Error in any resource fails the entire SSR render
