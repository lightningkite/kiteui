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

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
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

                            // Or show your own indicator:
                            shownWhen { !data.state().ready }.row {
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
                            val retry = Signal(0)
                            val data = rememberSuspending {
                                retry()
                                fetchData()       // a thrown error reaches the views that read data
                            }

                            col {
                                text { ::content { "Success: ${'$'}{data()}" } }
                                errorText()       // shows errors from this col here instead of in a dialog
                                shownWhen { data.state().exception != null }.button {
                                    text("Retry")
                                    onClick { retry.value++ }
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

                            // Runs once; cancelled when the view goes away
                            launch {
                                while (true) {
                                    delay(5.seconds)
                                    currentTime.value = Clock.System.now()
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
                                colOf(items, id = { it.id }) { item ->
                                    card.text { ::content { item().name } }
                                }

                                row {
                                    button {
                                        text("Previous")
                                        ::enabled { currentPage() > 1 }
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
                    text("Grow how many items you request as the user nears the end. useLastWhileLoading keeps the current rows on screen while the bigger window loads:")

                    space()
                    code {
                        content = """
                            val limit = Signal(20)
                            val items = rememberSuspending(useLastWhileLoading = true) {
                                fetchItems(limit = limit())
                            }

                            expanding.recyclerView {
                                children(items, id = { it.id }) { item ->
                                    card.text { ::content { item().name } }
                                }
                                reactive {
                                    val loaded = items()
                                    if (lastIndex() >= loaded.size - 5 && loaded.size == limit()) limit.value += 20
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
                                    items.value += newItem

                                    try {
                                        val saved = createItem(newItem)
                                        // Replace temp with real item
                                        items.value = items.value.map { if (it.id == -1) saved else it }
                                    } catch (e: Exception) {
                                        // Roll back, then let onClick show the error
                                        items.value -= newItem
                                        throw e
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
                            val debounced = searchQuery.debounce(300.milliseconds)

                            val results = rememberSuspending {
                                val query = debounced()
                                if (query.length < 3) return@rememberSuspending emptyList()
                                searchApi(query)
                            }

                            field("Search") {
                                textInput {
                                    hint = "Search"
                                    keyboardHints = KeyboardHints.title
                                    content bind searchQuery
                                }
                            }

                            colOf(results, id = { it.id }) { result ->
                                text { ::content { result().name } }
                            }
                        """.trimIndent()
                    }

                    space()
                    text("debounce waits until typing pauses, so typing quickly won't trigger multiple searches.")
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
                                col {
                                    text { ::content { data().toString() } }
                                    errorText()   // the error shows here instead of in a dialog
                                }
                            """.trimIndent()
                        }
                    }
                }
            }
        }
    }
}
