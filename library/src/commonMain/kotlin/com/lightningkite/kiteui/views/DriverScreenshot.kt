package com.lightningkite.kiteui.views

/**
 * Captures a screenshot of this view and returns it as a base64-encoded PNG string.
 * Platform-specific: not supported on JVM SSR.
 */
expect suspend fun RView.driverScreenshot(): String
