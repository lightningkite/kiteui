package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.ssr.ssrResource
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.card
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

/**
 * Demonstration page for the SsrResource pattern.
 *
 * ## How SsrResource Works:
 *
 * ### During Server-Side Rendering (SSR):
 * 1. `ssrResource()` is called during `render()`
 * 2. The resource registers with SsrContext and starts loading
 * 3. After render, SsrRouter awaits all resources
 * 4. Data is serialized into `__SSR_DATA__` script tag in HTML
 *
 * ### During Client Hydration:
 * 1. JS app initializes and calls `HydrationContext.initFromDom()`
 * 2. `ssrResource()` finds pre-loaded data via `HydrationContext.getData(key)`
 * 3. Resource is immediately ready - no network fetch needed!
 *
 * ### During Client-Side Navigation:
 * 1. No hydration data available
 * 2. `ssrResource()` starts loading via the loader function
 * 3. UI shows loading state, then updates when data arrives
 *
 * ## Key Benefits:
 * - **No explicit preload()** - Resources auto-register during render
 * - **Works in commonMain** - Not just jvmSsrMain
 * - **Fully reactive** - Integrates with KiteUI's reactive system
 * - **Automatic serialization** - Data types must be @Serializable
 */
@Routable("test/ssr-resource/{userId}")
class SsrResourceExamplePage(val userId: String = "demo-user-123") : Page {

    override val title: Reactive<String> = Constant("SSR Resource Demo")

    override fun ViewWriter.render() {
        scrolling.col {
            h1("SSR Resource Demo")
            text("This page demonstrates the SsrResource pattern for SSR data loading.")

            separator {}

            // Section: How It Works
            card.col {
                h2("How It Works")
                text("""
                    SsrResource provides a reactive way to load data that:
                    • During SSR: Automatically registers, loads, and serializes data for hydration
                    • On hydration: Loads from pre-rendered __SSR_DATA__ (no fetch needed)
                    • On client navigation: Fetches fresh data via the loader
                """.trimIndent())
            }

            separator {}

            // Create SSR-aware resources
            // Types must be @Serializable
            val user = ssrResource("user-$userId") { fetchUser(userId) }
            val posts = ssrResource("posts-$userId") { fetchPosts(userId) }
            val stats = ssrResource("stats-$userId") { fetchStats(userId) }

            // Section: User Profile
            // SsrResource implements Reactive<ReactiveState<T>> so user() returns ReactiveState<T>
            card.col {
                h2("User Profile")
                subtext("Resource key: user-$userId")

                text {
                    ::content {
                        user().handle(
                            success = { "✓ Name: ${it.name}" },
                            notReady = { "⏳ Loading user..." },
                            exception = { "✗ Error: ${it.message}" }
                        )
                    }
                }
                text {
                    ::content {
                        user().handle(
                            success = { "✓ Email: ${it.email}" },
                            notReady = { "" },
                            exception = { "" }
                        )
                    }
                }
                text {
                    ::content {
                        user().handle(
                            success = { "✓ Role: ${it.role}" },
                            notReady = { "" },
                            exception = { "" }
                        )
                    }
                }
            }

            // Section: User Stats
            card.col {
                h2("User Statistics")
                subtext("Resource key: stats-$userId")

                text {
                    ::content {
                        stats().handle(
                            success = { "✓ Posts: ${it.postCount}" },
                            notReady = { "⏳ Loading stats..." },
                            exception = { "✗ Failed to load stats" }
                        )
                    }
                }
                text {
                    ::content {
                        stats().handle(
                            success = { "✓ Followers: ${it.followers}" },
                            notReady = { "" },
                            exception = { "" }
                        )
                    }
                }
                text {
                    ::content {
                        stats().handle(
                            success = { "✓ Following: ${it.following}" },
                            notReady = { "" },
                            exception = { "" }
                        )
                    }
                }
            }

            // Section: User Posts
            card.col {
                h2("Recent Posts")
                subtext("Resource key: posts-$userId")

                text {
                    ::content {
                        posts().handle(
                            success = { postList ->
                                "✓ ${postList.size} posts loaded:\n\n" +
                                        postList.joinToString("\n\n") { post ->
                                            "📝 ${post.title}\n   ${post.content.take(60)}..."
                                        }
                            },
                            notReady = { "⏳ Loading posts..." },
                            exception = { "✗ Failed to load posts: ${it.message}" }
                        )
                    }
                }
            }

            separator {}

            // Section: Technical Details
            card.col {
                h2("Technical Details")
                text("User ID from URL parameter: $userId")
                text("Resource keys are namespaced by userId to ensure uniqueness.")

                separator {}

                h3("View Page Source to See:")
                text("""
                    • __SSR_DATA__ script tag with serialized JSON
                    • Pre-rendered content (no "Loading..." in source)
                    • Resource keys matching the ones shown above
                """.trimIndent())
            }

            separator {}

            // Section: Code Example
            card.col {
                h2("Code Example")
                text("""
                    // Create SSR-aware resource
                    val user = ssrResource("user-${"$"}userId") { fetchUser(userId) }

                    // Use reactively in UI - user() returns ReactiveState<T>
                    text {
                        ::content {
                            user().handle(
                                success = { "Name: ${"$"}{it.name}" },
                                notReady = { "Loading..." },
                                exception = { "Error: ${"$"}{it.message}" }
                            )
                        }
                    }
                """.trimIndent())
            }

            separator {}

            // Navigation links using externalLink for SSR compatibility
            card.col {
                h3("Try Different User IDs")
                text("Navigate to see how client-side navigation fetches fresh data:")
                row {
                    externalLink {
                        to = "/test/ssr-resource/alice"
                        text("Alice")
                    }
                    space {}
                    externalLink {
                        to = "/test/ssr-resource/bob"
                        text("Bob")
                    }
                    space {}
                    externalLink {
                        to = "/test/ssr-resource/charlie"
                        text("Charlie")
                    }
                }
            }
        }
    }
}

// ============================================================
// Data Models (must be @Serializable for SsrResource)
// ============================================================

@Serializable
data class UserData(
    val id: String,
    val name: String,
    val email: String,
    val role: String
)

@Serializable
data class PostData(
    val id: String,
    val title: String,
    val content: String,
    val timestamp: String
)

@Serializable
data class UserStats(
    val userId: String,
    val postCount: Int,
    val followers: Int,
    val following: Int
)

// ============================================================
// Simulated API Calls
// ============================================================

private suspend fun fetchUser(userId: String): UserData {
    delay(100) // Simulate network latency
    return UserData(
        id = userId,
        name = when (userId) {
            "alice" -> "Alice Johnson"
            "bob" -> "Bob Smith"
            "charlie" -> "Charlie Brown"
            else -> "Demo User"
        },
        email = "$userId@example.com",
        role = when (userId) {
            "alice" -> "Administrator"
            "bob" -> "Developer"
            "charlie" -> "Designer"
            else -> "Member"
        }
    )
}

private suspend fun fetchPosts(userId: String): List<PostData> {
    delay(150) // Simulate network latency
    return listOf(
        PostData(
            id = "1",
            title = "Getting Started with KiteUI",
            content = "KiteUI is a Kotlin Multiplatform UI framework that uses native view components on each platform...",
            timestamp = "2024-01-15"
        ),
        PostData(
            id = "2",
            title = "SSR with SsrResource",
            content = "The SsrResource pattern makes server-side rendering seamless by automatically handling data serialization...",
            timestamp = "2024-01-16"
        ),
        PostData(
            id = "3",
            title = "Reactive Data Binding",
            content = "KiteUI's reactive system is inspired by Solid.js, providing fine-grained reactivity without recomposition...",
            timestamp = "2024-01-17"
        )
    )
}

private suspend fun fetchStats(userId: String): UserStats {
    delay(80) // Simulate network latency
    return UserStats(
        userId = userId,
        postCount = when (userId) {
            "alice" -> 42
            "bob" -> 128
            "charlie" -> 23
            else -> 3
        },
        followers = when (userId) {
            "alice" -> 1500
            "bob" -> 890
            "charlie" -> 345
            else -> 0
        },
        following = when (userId) {
            "alice" -> 200
            "bob" -> 156
            "charlie" -> 89
            else -> 0
        }
    )
}
