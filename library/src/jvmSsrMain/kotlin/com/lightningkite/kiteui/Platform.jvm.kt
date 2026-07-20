package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Color

/**
 * Thread-local storage for SSR request user agent.
 * This allows Platform.probablyAppleUser and Platform.userAgent to return
 * correct values based on the client's user agent during SSR rendering.
 * - by Claude
 */
public object SsrUserAgentContext {
    @PublishedApi
    internal val threadLocal: ThreadLocal<String?> = ThreadLocal<String?>()

    /**
     * Set the user agent for the current SSR request.
     * Call this at the start of SSR rendering.
     */
    public fun set(userAgent: String?) {
        threadLocal.set(userAgent)
    }

    /**
     * Clear the user agent after SSR rendering completes.
     */
    public fun clear() {
        threadLocal.remove()
    }

    /**
     * Get the current user agent, or null if not in SSR context.
     */
    public fun get(): String? = threadLocal.get()

    /**
     * Execute a block with a specific user agent set.
     */
    public inline fun <T> withUserAgent(userAgent: String?, block: () -> T): T {
        val previous = threadLocal.get()
        try {
            threadLocal.set(userAgent)
            return block()
        } finally {
            if (previous != null) {
                threadLocal.set(previous)
            } else {
                threadLocal.remove()
            }
        }
    }
}

public actual val Platform.Companion.current: Platform
    get() = Platform.Desktop
public actual val Platform.Companion.probablyAppleUser: Boolean
    get() = SsrUserAgentContext.get()?.let { ua ->
        // Match the same logic as JS: check for Mac/iPhone/iPod/iPad
        ua.contains("Mac", ignoreCase = true) ||
        ua.contains("iPhone", ignoreCase = true) ||
        ua.contains("iPod", ignoreCase = true) ||
        ua.contains("iPad", ignoreCase = true)
    } ?: false
public actual val Platform.Companion.usesTouchscreen: Boolean
    get() = SsrUserAgentContext.get()?.let { ua ->
        // Mobile devices typically have touchscreens
        ua.contains("iPhone", ignoreCase = true) ||
        ua.contains("iPad", ignoreCase = true) ||
        ua.contains("Android", ignoreCase = true) ||
        ua.contains("Mobile", ignoreCase = true)
    } ?: false
public actual val Platform.Companion.userAgent: String
    get() = SsrUserAgentContext.get()
        ?: "JVM ${Runtime.version()} ${System.getProperty("os.name") ?: "Unknown"}"

// by Claude - SSR is always a development server
public actual val Platform.Companion.isDevelopment: Boolean
    get() = true

public actual fun setStatusBarColor(color: Color) {
}