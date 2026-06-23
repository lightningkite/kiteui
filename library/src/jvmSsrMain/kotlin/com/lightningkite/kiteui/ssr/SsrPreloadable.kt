package com.lightningkite.kiteui.ssr

import com.lightningkite.kiteui.navigation.Page

/**
 * Interface for pages that need to preload data before SSR rendering.
 *
 * Implement this interface on your Page class to load data asynchronously
 * before the page is rendered on the server. This ensures that server-rendered
 * HTML includes the actual content rather than loading states.
 *
 * Example:
 * ```kotlin
 * @Routable("users/{id}")
 * class UserPage(val id: String) : Page, SsrPreloadable {
 *     // Data that will be loaded during preload
 *     var userData: User? = null
 *         private set
 *
 *     override suspend fun preload(context: SsrContext) {
 *         // Fetch data before rendering
 *         userData = api.fetchUser(id)
 *
 *         // Optionally set page metadata
 *         context.title = userData?.name
 *         context.description = "Profile page for ${userData?.name}"
 *     }
 *
 *     override fun ElementWriter.CanAddTheme.render() = col {
 *         // Use preloaded data - will be populated during SSR
 *         userData?.let { user ->
 *             h1(user.name)
 *             text(user.bio)
 *         } ?: run {
 *             // Client-side will show this until hydration
 *             text("Loading...")
 *         }
 *     }
 * }
 * ```
 *
 * Notes:
 * - preload() is only called during SSR, not on the client
 * - Keep preload fast - it blocks the HTTP response
 * - Use context.preload(key, value) to store data that can be serialized for hydration
 * - Set context.title, context.description etc. for SEO metadata
 */
interface SsrPreloadable {
    /**
     * Load data before rendering this page.
     *
     * Called by SsrRouter before render(). Use this to:
     * - Fetch API data
     * - Set page metadata (title, description)
     * - Store data for hydration
     *
     * @param context The SSR context for this request
     */
    suspend fun preload(context: SsrContext)
}

/**
 * Helper to check if a Page supports preloading.
 */
fun Page.asPreloadable(): SsrPreloadable? = this as? SsrPreloadable
