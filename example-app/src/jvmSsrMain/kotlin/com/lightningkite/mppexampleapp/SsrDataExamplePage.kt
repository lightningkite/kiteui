package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.ssr.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive
import kotlinx.coroutines.delay

/**
 * Example page demonstrating SSR data preloading and SEO metadata.
 *
 * This page implements [SsrPreloadable] to fetch data before rendering.
 * When SSR renders this page, it calls preload() first, which populates
 * the data fields. The render() method then uses that data.
 *
 * This pattern ensures server-rendered HTML includes actual content
 * rather than loading states.
 *
 * It also demonstrates using [PageMeta] for structured SEO metadata
 * including OpenGraph and Twitter Card tags.
 */
class SsrDataExamplePage(val userId: String = "123") : Page, SsrPreloadable {

    // Data fields populated during preload
    var userName: String? = null
        private set
    var userEmail: String? = null
        private set
    var userPosts: List<String>? = null
        private set
    var userAvatarUrl: String? = null
        private set
    var loadTimeMs: Long = 0
        private set

    override val title: Reactive<String>
        get() = Constant(userName ?: "User Profile")

    override suspend fun preload(context: SsrContext) {
        val startTime = System.currentTimeMillis()

        // Simulate fetching user data from an API
        // In a real app, this would be: userData = api.fetchUser(userId)
        delay(50) // Simulate network latency

        userName = "John Doe"
        userEmail = "john.doe@example.com"
        userAvatarUrl = "https://example.com/avatars/$userId.jpg"
        userPosts = listOf(
            "Hello, this is my first post!",
            "SSR is working great with KiteUI",
            "Data preloading makes SEO so much better"
        )

        loadTimeMs = System.currentTimeMillis() - startTime

        // Apply structured SEO metadata using PageMeta
        context.applyMeta(pageMeta {
            title = "Profile: $userName"
            description = "View the profile of $userName. ${userPosts?.size ?: 0} posts."
            canonicalUrl = "https://example.com/users/$userId"
            author = userName

            openGraph {
                type = OpenGraph.Type.PROFILE
                image = userAvatarUrl
                imageAlt = "Profile picture of $userName"
                imageWidth = 400
                imageHeight = 400
                siteName = "KiteUI Example App"
                locale = "en_US"
            }

            twitter {
                card = TwitterCard.Card.SUMMARY
                site = "@kiteui"
                creator = "@${userName?.replace(" ", "")?.lowercase()}"
            }
        })
    }

    override fun ElementWriter.CanAddTheme.render() {
        col {
            h1("SSR Data Preloading Example")

            text("This page demonstrates how to preload data for SSR.")

            separator {}

            // Show user data (populated by preload)
            userName?.let { name ->
                h2("User Profile")
                text("Name: $name")
                userEmail?.let { text("Email: $it") }
            } ?: run {
                // This would show on client-side before hydration
                text("Loading user data...")
            }

            separator {}

            // Show posts
            userPosts?.let { posts ->
                h3("Recent Posts")
                posts.forEach { post ->
                    col {
                        text("• $post")
                    }
                }
            }

            separator {}

            // Show preload timing (demonstrates that preload ran)
            subtext("Data loaded in ${loadTimeMs}ms during SSR preload")
            subtext("User ID: $userId")
        }
    }
}
