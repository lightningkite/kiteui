package com.lightningkite.kiteui.ssr

/**
 * Result of SSR rendering containing all parts needed to construct a complete HTML page.
 */
internal data class SsrResult(
    /** The rendered HTML body content */
    val html: String,
    /** Generated CSS rules */
    val css: String,
    /** Additional head elements (fonts, external stylesheets, etc.) */
    val headElements: List<String>,
    /** Page title for <title> tag */
    val title: String? = null,
    /** Meta description for SEO */
    val description: String? = null,
    /** Canonical URL for SEO */
    val canonicalUrl: String? = null,
    /** OpenGraph and other meta tags (property -> content) */
    val metaTags: Map<String, String> = emptyMap()
)
