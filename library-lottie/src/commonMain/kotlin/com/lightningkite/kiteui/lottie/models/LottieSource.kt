package com.lightningkite.kiteui.lottie.models

/**
 * Sealed class representing sources for Lottie animations.
 */
sealed class LottieSource

/**
 * A Lottie animation loaded from a remote URL.
 * @param url The URL to the Lottie JSON file (or .lottie file)
 */
data class LottieRemote(val url: String) : LottieSource() {
    override fun hashCode(): Int = url.hashCode()
    override fun equals(other: Any?): Boolean = other is LottieRemote && other.url == this.url
    override fun toString(): String = "LottieRemote($url)"
}

/**
 * A Lottie animation from raw JSON data.
 * @param json The Lottie animation JSON as a string
 * @param cacheKey Optional cache key for identifying this animation
 */
data class LottieRaw(val json: String, val cacheKey: String? = null) : LottieSource()
