package com.lightningkite.kiteui.views

/**
 * Attempts to start the AI driver daemon on the given port if it's not already running.
 * Only does anything on JVM SSR (where the daemon JAR is available locally).
 * All other platforms are no-ops.
 */
internal expect fun tryAutoStartDaemon(port: Int)
