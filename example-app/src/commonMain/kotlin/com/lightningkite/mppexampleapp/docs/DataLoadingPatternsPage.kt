package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.mppexampleapp.widgets.code
import com.lightningkite.reactive.core.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Routable("docs/data-loading")
object DataLoadingPatternsPage : DocPage {
    override val covers: List<String> = listOf(
        "data loading", "async", "suspending", "remember", "loading states",
        "error handling", "retry", "caching", "rememberSuspending"
    )

    override fun ViewWriter.render(): Unit = run {
        article {
            titledSection("Data Loading Patterns") {
                text("Learn how to load, cache, and handle asynchronous data in KiteUI.")

                space()

                titledSection("Basic Data Loading") {
                    text("Use rememberSuspending for async operations:")

                    space()
                    code {
                        content = """
                            val userData = rememberSuspending {
                                // This runs in a coroutine
                                delay(1000) // Simulated network call
                                fetchUser(userId)
                            }

                            col {
                                text {
                                    ::content { "Name: ${'$'}{userData().name}" }
                                }
                            }
                        """.trimIndent()
                    }

                    space()
                    text("Key points:")
                    card.col {
                        text("• rememberSuspending runs once when the component mounts")
                        text("• Views automatically show a loading state while data loads")
                        text("• The result is cached - it won't re-fetch on every render")
                        text("• Throws will propagate and show error state")
                    }
                }

                space()

                titledSection("Dependent Data Loading") {
                    text("Load data that depends on other reactive values:")

                    space()
                    code {
                        content = """
                            val userId = Signal(1)

                            val user = rememberSuspending {
                                // Re-runs when userId changes
                                delay(1000)
                                fetchUser(userId())
                            }

                            button {
                                text("Next User")
                                onClick {
                                    userId.value++
                                    // User will automatically reload
                                }
                            }

                            text {
                                ::content { "User: ${'$'}{user().name}" }
                            }
                        """.trimIndent()
                    }

                    space()
                    text("The reactive block tracks dependencies - when userId() changes, the data automatically reloads.")
                }

                space()

                titledSection("Loading States") {
                    text("Display loading indicators while data fetches:")

                    space()
                    code {
                        content = """
                            val data = rememberSuspending {
                                delay(2000)
                                fetchData()
                            }

                            // While loading, views show default loading state
                            text {
                                ::content { data().toString() }
                            }

                            // Or handle explicitly:
                            try {
                                text { ::content { "Data: ${'$'}{data()}" } }
                            } catch (e: LoadingException) {
                                activityIndicator()
                                text("Loading...")
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Error Handling") {
                    text("Handle errors in data loading:")

                    space()
                    code {
                        content = """
                            val data = rememberSuspending {
                                try {
                                    fetchData()
                                } catch (e: Exception) {
                                    // Handle or re-throw
                                    throw e
                                }
                            }

                            // Catch errors in UI
                            try {
                                affirmative.text {
                                    ::content { "Success: ${'$'}{data()}" }
                                }
                            } catch (e: Exception) {
                                danger.col {
                                    text("Error: ${'$'}{e.message}")
                                    button {
                                        text("Retry")
                                        onClick {
                                            // Trigger reload
                                        }
                                    }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Manual Refetching") {
                    text("Trigger manual data reloads:")

                    space()
                    code {
                        content = """
                            val refreshTrigger = Signal(0)

                            val data = rememberSuspending {
                                refreshTrigger() // Track dependency
                                fetchData()
                            }

                            button {
                                text("Refresh")
                                onClick {
                                    refreshTrigger.value++
                                    // Data will reload automatically
                                }
                            }

                            text {
                                ::content { "Data: ${'$'}{data()}" }
                            }
                        """.trimIndent()
                    }

                    space()
                    text("Incrementing refreshTrigger causes rememberSuspending to re-run.")
                }

                space()

                titledSection("Multiple Async Operations") {
                    text("Load multiple data sources in parallel:")

                    space()
                    code {
                        content = """
                            val user = rememberSuspending { fetchUser(userId) }
                            val posts = rememberSuspending { fetchPosts(userId) }
                            val comments = rememberSuspending { fetchComments(userId) }

                            // All three load in parallel
                            col {
                                text { ::content { "User: ${'$'}{user().name}" } }
                                text { ::content { "Posts: ${'$'}{posts().size}" } }
                                text { ::content { "Comments: ${'$'}{comments().size}" } }
                            }
                        """.trimIndent()
                    }

                    space()
                    text("Each rememberSuspending runs independently in parallel.")
                }

                space()

                titledSection("Sequential Loading") {
                    text("Load data sequentially when one depends on another:")

                    space()
                    code {
                        content = """
                            val data = rememberSuspending {
                                // Load sequentially inside one suspending block
                                val user = fetchUser(userId)
                                val profile = fetchProfile(user.profileId)
                                val settings = fetchSettings(profile.settingsId)

                                Triple(user, profile, settings)
                            }

                            text {
                                ::content {
                                    val (user, profile, settings) = data()
                                    "Loaded: ${'$'}{user.name}, ${'$'}{profile.bio}"
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Caching Strategies") {
                    text("rememberSuspending automatically caches results:")

                    space()
                    code {
                        content = """
                            val userData = rememberSuspending {
                                println("Fetching user data...") // Only prints once
                                fetchUser(userId)
                            }

                            // This will use cached data, not refetch
                            repeat(10) {
                                text { ::content { userData().name } }
                            }
                        """.trimIndent()
                    }

                    space()
                    text("To invalidate cache, change a tracked dependency:")
                    code {
                        content = """
                            val cacheKey = Signal(0)

                            val data = rememberSuspending {
                                cacheKey() // Track this
                                fetchData()
                            }

                            button {
                                text("Invalidate Cache")
                                onClick {
                                    cacheKey.value++ // Forces reload
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Polling / Auto-Refresh") {
                    text("Automatically refresh data on an interval:")

                    space()
                    code {
                        content = """
                            val currentTime = Signal(Clock.System.now())

                            // Update every 5 seconds
                            reactiveScope {
                                launch {
                                    while (true) {
                                        delay(5000)
                                        currentTime.value = Clock.System.now()
                                    }
                                }
                            }

                            val data = rememberSuspending {
                                currentTime() // Re-runs every 5 seconds
                                fetchLatestData()
                            }

                            text {
                                ::content { "Data: ${'$'}{data()}" }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Pagination") {
                    text("Load data in pages:")

                    space()
                    code {
                        content = """
                            val currentPage = Signal(1)
                            val pageSize = 20

                            val items = rememberSuspending {
                                fetchPage(
                                    page = currentPage(),
                                    pageSize = pageSize
                                )
                            }

                            col {
                                forEach(items) { item ->
                                    card.text { ::content { item().name } }
                                }

                                row {
                                    button {
                                        text("Previous")
                                        enabled = remember { currentPage() > 1 }
                                        onClick { currentPage.value-- }
                                    }

                                    text {
                                        ::content { "Page ${'$'}{currentPage()}" }
                                    }

                                    button {
                                        text("Next")
                                        onClick { currentPage.value++ }
                                    }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Infinite Scroll") {
                    text("Load more data as the user scrolls:")

                    space()
                    code {
                        content = """
                            val allItems = Signal(listOf<Item>())
                            val page = Signal(1)
                            val hasMore = Signal(true)

                            val newItems = rememberSuspending {
                                if (!hasMore()) return@rememberSuspending emptyList()
                                val items = fetchPage(page())
                                hasMore.value = items.isNotEmpty()
                                items
                            }

                            reactiveScope {
                                // Append new items to list
                                allItems.value = allItems() + newItems()
                            }

                            scrolling.col {
                                forEach(allItems) { item ->
                                    card.text { ::content { item().name } }
                                }

                                if (hasMore()) {
                                    button {
                                        text("Load More")
                                        onClick { page.value++ }
                                    }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Optimistic Updates") {
                    text("Update UI immediately, then sync with server:")

                    space()
                    code {
                        content = """
                            val items = Signal(listOf<Item>())

                            button {
                                text("Add Item")
                                onClick {
                                    val newItem = Item(id = -1, name = "New")

                                    // Optimistically add to UI
                                    items.value = items() + newItem

                                    launch {
                                        try {
                                            // Save to server
                                            val saved = createItem(newItem)

                                            // Replace temp with real item
                                            items.value = items().map {
                                                if (it.id == -1) saved else it
                                            }
                                        } catch (e: Exception) {
                                            // Rollback on error
                                            items.value = items() - newItem
                                            toast("Failed to create item")
                                        }
                                    }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Debounced Search") {
                    text("Search with debouncing to reduce API calls:")

                    space()
                    code {
                        content = """
                            val searchQuery = Signal("")

                            // Debounced search results
                            val results = rememberSuspending {
                                val query = searchQuery()
                                if (query.length < 3) return@rememberSuspending emptyList()

                                delay(300) // Debounce
                                searchApi(query)
                            }

                            field("Search") {
                                textInput {
                                    content bind searchQuery
                                }
                            }

                            forEach(results) { result ->
                                text { ::content { result().name } }
                            }
                        """.trimIndent()
                    }

                    space()
                    text("The delay provides natural debouncing - typing quickly won't trigger multiple searches.")
                }

                space()

                titledSection("Best Practices") {
                    card.col {
                        text("✓ Use rememberSuspending for all async operations")
                        text("✓ Let KiteUI handle loading states automatically")
                        text("✓ Catch and handle errors gracefully")
                        text("✓ Use reactive dependencies to trigger reloads")
                        text("✓ Cache expensive operations")
                        text("✓ Provide manual refresh options for users")
                        text("✓ Show progress indicators for long operations")
                        text("✓ Handle offline/network error scenarios")
                        text("✓ Implement optimistic updates for better UX")
                        text("✓ Debounce search and frequent operations")
                    }
                }

                space()

                titledSection("Common Patterns Summary") {
                    card.col {
                        h5("Basic Load:")
                        code {
                            content = """
                                val data = rememberSuspending { fetchData() }
                                text { ::content { data().toString() } }
                            """.trimIndent()
                        }

                        space()
                        h5("Dependent Load:")
                        code {
                            content = """
                                val id = Signal(1)
                                val data = rememberSuspending { fetchData(id()) }
                            """.trimIndent()
                        }

                        space()
                        h5("Manual Refresh:")
                        code {
                            content = """
                                val trigger = Signal(0)
                                val data = rememberSuspending {
                                    trigger()
                                    fetchData()
                                }
                                button {
                                    text("Refresh")
                                    onClick { trigger.value++ }
                                }
                            """.trimIndent()
                        }

                        space()
                        h5("Error Handling:")
                        code {
                            content = """
                                try {
                                    text { ::content { data().toString() } }
                                } catch (e: Exception) {
                                    danger.text("Error: ${'$'}{e.message}")
                                }
                            """.trimIndent()
                        }
                    }
                }
            }
        }
    }
}
