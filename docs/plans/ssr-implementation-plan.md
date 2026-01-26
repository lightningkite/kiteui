# SSR Implementation Plan

**Status:** In Progress (Phases 1-6 Complete)
**Created:** 2025-12-20
**Last Updated:** 2025-12-21

## Overview

This plan outlines the work needed to make KiteUI's JVM SSR target production-ready. The foundation is solid (HTML rendering, CSS generation, theming), but several features are needed for practical use.

## Completed (2025-12-20)

- [x] Fix `innerHtmlUnsafe` not being rendered
- [x] Fix self-closing tags (only void elements now self-close)

## Completed (2025-12-21)

- [x] **Phase 1: SsrContext** - Request isolation, per-context CSS, preloaded data storage
- [x] **Phase 2: Route-based rendering** - SsrRouter integrates with AutoRoutes for automatic URL-to-HTML
- [x] **Phase 3: Data loading** - SsrPreloadable interface for async data fetch before render
- [x] **Phase 4: SEO/Meta tags** - PageMeta with OpenGraph and Twitter Card support
- [x] **Phase 5: Full HTML document** - SsrDocument renders complete HTML with all head elements
- [x] **Phase 6: Testing** - Test server with shell scripts (local/ssr-test/)

### Key Files Created:
- `library/src/jvmSsrMain/kotlin/com/lightningkite/kiteui/ssr/SsrContext.kt`
- `library/src/jvmSsrMain/kotlin/com/lightningkite/kiteui/ssr/SsrResult.kt`
- `library/src/jvmSsrMain/kotlin/com/lightningkite/kiteui/ssr/SsrDocument.kt`
- `library/src/jvmSsrMain/kotlin/com/lightningkite/kiteui/ssr/SsrRouter.kt`
- `library/src/jvmSsrMain/kotlin/com/lightningkite/kiteui/ssr/SsrPreloadable.kt`
- `library/src/jvmSsrMain/kotlin/com/lightningkite/kiteui/ssr/PageMeta.kt`
- `example-app/src/jvmSsrMain/kotlin/com/lightningkite/mppexampleapp/SsrServer.kt`
- `example-app/src/jvmSsrMain/kotlin/com/lightningkite/mppexampleapp/SsrDataExamplePage.kt`
- `local/ssr-test/*.sh` - Testing scripts

## Phase 1: SSR Context & Request Isolation

**Goal:** Enable multiple concurrent requests without state pollution.

### Problem

`DynamicCss.rules` accumulates indefinitely. In a long-running server, CSS rules from all requests pile up, and different requests can see each other's CSS.

### Solution: `SsrContext` class

```kotlin
class SsrContext(
    val basePath: String = "/",
    val windowInfo: WindowStatistics = WindowStatistics(1920.px, 1080.px, 1f),
) {
    val rContext = RContext(basePath)

    // For data preloading
    private val preloadedData = mutableMapOf<String, Any?>()

    fun <T> preload(key: String, value: T) {
        preloadedData[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getPreloaded(key: String): T? = preloadedData[key] as? T

    // Render a page
    fun render(page: Page): SsrResult {
        val frame = Frame(rContext)
        with(frame) {
            page.render()
        }
        return SsrResult(
            html = buildString { frame.children[0].native.render(this) },
            css = rContext.dynamicCss.emit(),
            headElements = rContext.dynamicCss.headElements.toList()
        )
    }
}

data class SsrResult(
    val html: String,
    val css: String,
    val headElements: List<String>,
    val title: String? = null,
    val metaTags: Map<String, String> = emptyMap()
)
```

### Tasks

1. Create `SsrContext.kt` in `library/src/jvmSsrMain/kotlin/.../`
2. Add `preload()` and `getPreloaded()` for data injection
3. Modify `DynamicCss` to support per-context instances (already does via RContext)
4. Add test for concurrent request isolation

### Files to Modify/Create

- `library/src/jvmSsrMain/kotlin/com/lightningkite/kiteui/ssr/SsrContext.kt` (new)
- `library/src/jvmSsrTest/kotlin/SsrContextTest.kt` (new)

---

## Phase 2: Route-Based Rendering

**Goal:** Render any route from a URL string.

### Problem

Currently you manually create a Frame and render a page. There's no way to:
- Parse a URL to a Page
- Render with proper navigation context
- Handle route parameters

### Solution: `SsrRouter`

```kotlin
class SsrRouter(
    val routes: Routes,  // Your app's routes
    val basePath: String = "/"
) {
    fun render(url: String): SsrResult? {
        val page = routes.parse(url) ?: return null
        val context = SsrContext(basePath)
        return context.render(page)
    }

    suspend fun renderWithData(
        url: String,
        preloader: suspend (Page, SsrContext) -> Unit
    ): SsrResult? {
        val page = routes.parse(url) ?: return null
        val context = SsrContext(basePath)
        preloader(page, context)
        return context.render(page)
    }
}
```

### Tasks

1. Ensure `Routes.parse(url): Page?` exists and works (check current implementation)
2. Create `SsrRouter.kt`
3. Add integration with `ScreenStack` for nested navigation
4. Test with example-app routes

### Files to Modify/Create

- `library/src/jvmSsrMain/kotlin/com/lightningkite/kiteui/ssr/SsrRouter.kt` (new)
- Check `library/src/commonMain/kotlin/com/lightningkite/kiteui/navigation/Routes.kt`

---

## Phase 3: Data Loading Strategy

**Goal:** Pages can declare data dependencies that load before render.

### Problem

```kotlin
// This won't have data in SSR:
col {
    val user = shared { api.fetchUser(id) }  // Async!
    text { ::content { user().name } }  // Will be empty/loading
}
```

### Solution Options

**Option A: Suspend preload method on Page** (Recommended)

```kotlin
interface Page {
    fun ViewWriter.render()

    // New: optional preload hook for SSR
    suspend fun preload(context: SsrContext) {}
}

// Usage in page:
@Routable("users/{id}")
class UserPage(val id: String) : Page {
    private var userData: User? = null

    override suspend fun preload(context: SsrContext) {
        userData = api.fetchUser(id)
    }

    override fun ViewWriter.render() = col {
        userData?.let { user ->
            text(user.name)
        } ?: text("Loading...")
    }
}
```

**Option B: Explicit data injection**

```kotlin
// At render time:
ssrRouter.renderWithData("/users/123") { page, context ->
    if (page is UserPage) {
        context.preload("user", api.fetchUser(page.id))
    }
}

// In page:
class UserPage(val id: String) : Page {
    override fun ViewWriter.render() = col {
        val user = ssrContext.getPreloaded<User>("user")
        // ...
    }
}
```

### Recommendation

Option A is cleaner - pages are self-contained and know their own data needs.

### Tasks

1. Add optional `suspend fun preload(context: SsrContext)` to Page interface
2. Modify SsrRouter to call preload before render
3. Document pattern for SSR-aware pages
4. Create example showing data loading

### Files to Modify/Create

- `library/src/commonMain/kotlin/com/lightningkite/kiteui/navigation/Page.kt`
- `library/src/jvmSsrMain/kotlin/com/lightningkite/kiteui/ssr/SsrRouter.kt`

---

## Phase 4: SEO & Meta Tags

**Goal:** Pages can contribute `<title>`, meta tags, OpenGraph data.

### Problem

No way to set page-specific:
- `<title>`
- `<meta name="description">`
- OpenGraph tags
- Canonical URLs

### Solution: `SsrMeta` collection

```kotlin
interface Page {
    fun ViewWriter.render()
    suspend fun preload(context: SsrContext) {}

    // New: optional metadata for SSR
    fun meta(): PageMeta? = null
}

data class PageMeta(
    val title: String? = null,
    val description: String? = null,
    val canonicalUrl: String? = null,
    val openGraph: Map<String, String> = emptyMap(),
    val additionalTags: List<String> = emptyList()
)

// Usage:
@Routable("products/{id}")
class ProductPage(val id: String) : Page {
    private var product: Product? = null

    override suspend fun preload(context: SsrContext) {
        product = api.fetchProduct(id)
    }

    override fun meta() = PageMeta(
        title = product?.name ?: "Loading...",
        description = product?.description,
        openGraph = mapOf(
            "og:title" to (product?.name ?: ""),
            "og:image" to (product?.imageUrl ?: "")
        )
    )

    override fun ViewWriter.render() = col { ... }
}
```

### Tasks

1. Create `PageMeta` data class
2. Add `meta()` to Page interface
3. Update `SsrResult` to include metadata
4. Create helper to render full HTML document with meta tags
5. Test with crawler-like validation

### Files to Modify/Create

- `library/src/commonMain/kotlin/com/lightningkite/kiteui/navigation/PageMeta.kt` (new)
- `library/src/commonMain/kotlin/com/lightningkite/kiteui/navigation/Page.kt`
- `library/src/jvmSsrMain/kotlin/com/lightningkite/kiteui/ssr/SsrDocument.kt` (new)

---

## Phase 5: Full HTML Document Rendering

**Goal:** One-call API to render complete HTML document.

### Solution

```kotlin
class SsrDocument(
    val lang: String = "en",
    val baseHref: String = "/",
    val additionalHeadContent: String = "",
    val additionalBodyContent: String = "",  // For hydration scripts
) {
    fun render(result: SsrResult): String = buildString {
        appendLine("<!DOCTYPE html>")
        appendLine("<html lang=\"$lang\">")
        appendLine("<head>")
        appendLine("  <meta charset=\"UTF-8\">")
        appendLine("  <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
        result.meta?.title?.let { appendLine("  <title>$it</title>") }
        result.meta?.description?.let {
            appendLine("  <meta name=\"description\" content=\"$it\">")
        }
        result.meta?.openGraph?.forEach { (k, v) ->
            appendLine("  <meta property=\"$k\" content=\"$v\">")
        }
        appendLine("  <base href=\"$baseHref\">")
        result.headElements.forEach { appendLine("  $it") }
        appendLine("  <style>${result.css}</style>")
        appendLine(additionalHeadContent)
        appendLine("</head>")
        appendLine("<body>")
        appendLine(result.html)
        appendLine(additionalBodyContent)
        appendLine("</body>")
        appendLine("</html>")
    }
}
```

### Tasks

1. Create `SsrDocument.kt`
2. Handle CSS/head element merging
3. Add escaping for meta tag content
4. Create example Ktor/Javalin integration

---

## Phase 6: Testing Infrastructure

**Goal:** Comprehensive SSR testing with browser verification.

### Components Needed

1. **Unit Tests** - Test individual components render correctly
2. **Integration Tests** - Test full page render with routing
3. **Visual Tests** - Render in browser, compare screenshots
4. **Server Tests** - Run a test server, hit with HTTP requests

### Test Server Setup

```kotlin
// In test-utilities or example-app
fun createSsrTestServer(
    routes: Routes,
    port: Int = 8080
): ApplicationEngine {
    return embeddedServer(Netty, port = port) {
        routing {
            get("/{path...}") {
                val url = call.request.uri
                val result = SsrRouter(routes).render(url)
                if (result != null) {
                    call.respondText(
                        SsrDocument().render(result),
                        ContentType.Text.Html
                    )
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
}
```

### Tasks

1. Add Ktor server dependency to test module
2. Create `SsrTestServer.kt`
3. Create test that:
   - Starts server
   - Renders example-app pages
   - Validates HTML structure
4. Add Chrome/Playwright test for visual verification
5. Compare SSR output with JS client output

### Files to Create

- `example-app/src/jvmSsrMain/kotlin/.../SsrServer.kt`
- `example-app/src/jvmSsrTest/kotlin/.../SsrServerTest.kt`

---

## Phase 7: Hydration (Future)

**Goal:** Client JS can "pick up" server-rendered HTML.

### This is the most complex phase. Key challenges:

1. **Stable IDs** - Elements need matching IDs between server and client
2. **State Serialization** - Reactive state must be serializable
3. **Event Reconnection** - Client JS must find elements and attach handlers
4. **Diff Handling** - What if client state differs from server?

### Approach

1. **ID Generation**: Add `data-ssr-id` attributes during SSR
2. **State Export**: Serialize reactive values to JSON in `<script>` tag
3. **Hydration Entry Point**: Client JS function that:
   - Parses serialized state
   - Walks DOM to find `data-ssr-id` elements
   - Reconnects handlers without re-rendering

### Not in initial scope - revisit after Phases 1-6 complete.

---

## Testing Strategy Summary

| Phase | Test Type | What to Verify |
|-------|-----------|----------------|
| 1 | Unit | Context isolation, concurrent requests |
| 2 | Integration | Route parsing, page rendering |
| 3 | Integration | Data preloading, async handling |
| 4 | Unit | Meta tag generation, escaping |
| 5 | Integration | Full document structure |
| 6 | E2E | Browser loads page, CSS correct |

---

## Implementation Order

1. **Phase 1** - SsrContext (foundation for everything else)
2. **Phase 5** - SsrDocument (needed for testing)
3. **Phase 6** - Test infrastructure (enables verification of other phases)
4. **Phase 2** - Route-based rendering
5. **Phase 3** - Data loading
6. **Phase 4** - SEO/Meta tags
7. **Phase 7** - Hydration (future)

---

## Success Criteria

SSR is "production ready" when:

1. Multiple concurrent requests don't interfere
2. Pages with data dependencies can preload
3. SEO crawlers see complete page content
4. Generated HTML is valid HTML5
5. CSS applies correctly in browser
6. Pages load and display correctly
7. Meta tags are present for SEO

---

## Notes

- Keep SSR and client rendering code paths aligned
- Avoid SSR-only or client-only components where possible
- Document any SSR limitations clearly
- Performance: consider streaming for large pages (future)
