package com.lightningkite.kiteui.ssr

/**
 * Structured metadata for SEO and social sharing.
 *
 * Use this class to define page metadata in a structured way that automatically
 * generates the appropriate meta tags for SEO, OpenGraph (Facebook/LinkedIn),
 * and Twitter Cards.
 *
 * Example usage:
 * ```kotlin
 * class ProductPage(val id: String) : Page, SsrPreloadable {
 *     private var product: Product? = null
 *
 *     override suspend fun preload(context: SsrContext) {
 *         product = api.fetchProduct(id)
 *
 *         // Apply structured metadata
 *         context.applyMeta(PageMeta(
 *             title = product?.name ?: "Product",
 *             description = product?.description,
 *             canonicalUrl = "https://example.com/products/$id",
 *             openGraph = OpenGraph(
 *                 type = OpenGraph.Type.PRODUCT,
 *                 image = product?.imageUrl,
 *                 siteName = "My Store"
 *             ),
 *             twitter = TwitterCard.summary(
 *                 site = "@mystore"
 *             )
 *         ))
 *     }
 * }
 * ```
 */
public data class PageMeta(
    /** Page title for <title> and og:title */
    val title: String? = null,
    /** Meta description for SEO and og:description */
    val description: String? = null,
    /** Canonical URL for SEO and og:url */
    val canonicalUrl: String? = null,
    /** OpenGraph metadata for social sharing */
    val openGraph: OpenGraph? = null,
    /** Twitter Card metadata */
    val twitter: TwitterCard? = null,
    /** Robots directive (e.g., "noindex, nofollow") */
    val robots: String? = null,
    /** Author name */
    val author: String? = null,
    /** Keywords (comma-separated) - less important for modern SEO but still used */
    val keywords: String? = null,
    /** Additional custom meta tags (name/property -> content) */
    val customTags: Map<String, String> = emptyMap()
) {
    /**
     * Convert to flat map of meta tags for rendering.
     */
    public fun toMetaTags(): Map<String, String> = buildMap {
        // Standard meta tags
        robots?.let { put("robots", it) }
        author?.let { put("author", it) }
        keywords?.let { put("keywords", it) }

        // OpenGraph tags
        openGraph?.let { og ->
            put("og:type", og.type.value)
            // Use page title/description as fallback for OG
            (og.title ?: title)?.let { put("og:title", it) }
            (og.description ?: description)?.let { put("og:description", it) }
            (og.url ?: canonicalUrl)?.let { put("og:url", it) }
            og.image?.let { put("og:image", it) }
            og.imageAlt?.let { put("og:image:alt", it) }
            og.imageWidth?.let { put("og:image:width", it.toString()) }
            og.imageHeight?.let { put("og:image:height", it.toString()) }
            og.siteName?.let { put("og:site_name", it) }
            og.locale?.let { put("og:locale", it) }
            og.additionalTags.forEach { (k, v) -> put(k, v) }
        }

        // Twitter Card tags
        twitter?.let { tw ->
            put("twitter:card", tw.card.value)
            (tw.title ?: openGraph?.title ?: title)?.let { put("twitter:title", it) }
            (tw.description ?: openGraph?.description ?: description)?.let { put("twitter:description", it) }
            (tw.image ?: openGraph?.image)?.let { put("twitter:image", it) }
            (tw.imageAlt ?: openGraph?.imageAlt)?.let { put("twitter:image:alt", it) }
            tw.site?.let { put("twitter:site", it) }
            tw.creator?.let { put("twitter:creator", it) }
            tw.additionalTags.forEach { (k, v) -> put(k, v) }
        }

        // Custom tags
        putAll(customTags)
    }
}

/**
 * OpenGraph metadata for Facebook, LinkedIn, and other social platforms.
 *
 * @see https://ogp.me/
 */
public data class OpenGraph(
    /** The type of content (website, article, product, etc.) */
    val type: Type = Type.WEBSITE,
    /** Title override (defaults to PageMeta.title) */
    val title: String? = null,
    /** Description override (defaults to PageMeta.description) */
    val description: String? = null,
    /** URL override (defaults to PageMeta.canonicalUrl) */
    val url: String? = null,
    /** Image URL for social preview */
    val image: String? = null,
    /** Alt text for the image */
    val imageAlt: String? = null,
    /** Image width in pixels */
    val imageWidth: Int? = null,
    /** Image height in pixels */
    val imageHeight: Int? = null,
    /** Site name (e.g., "My Company") */
    val siteName: String? = null,
    /** Locale (e.g., "en_US") */
    val locale: String? = null,
    /** Additional OG tags not covered by standard properties */
    val additionalTags: Map<String, String> = emptyMap()
) {
    public enum class Type(public val value: String) {
        WEBSITE("website"),
        ARTICLE("article"),
        PRODUCT("product"),
        PROFILE("profile"),
        BOOK("book"),
        MUSIC_SONG("music.song"),
        MUSIC_ALBUM("music.album"),
        VIDEO_MOVIE("video.movie"),
        VIDEO_EPISODE("video.episode"),
        VIDEO_OTHER("video.other")
    }

    public companion object {
        /**
         * Create OpenGraph metadata for an article/blog post.
         */
        public fun article(
            publishedTime: String? = null,
            modifiedTime: String? = null,
            author: String? = null,
            section: String? = null,
            tags: List<String> = emptyList(),
            image: String? = null,
            siteName: String? = null
        ) = OpenGraph(
            type = Type.ARTICLE,
            image = image,
            siteName = siteName,
            additionalTags = buildMap {
                publishedTime?.let { put("article:published_time", it) }
                modifiedTime?.let { put("article:modified_time", it) }
                author?.let { put("article:author", it) }
                section?.let { put("article:section", it) }
                tags.forEachIndexed { i, tag -> put("article:tag:$i", tag) }
            }
        )

        /**
         * Create OpenGraph metadata for a product.
         */
        public fun product(
            price: String? = null,
            currency: String? = null,
            availability: String? = null,
            image: String? = null,
            siteName: String? = null
        ) = OpenGraph(
            type = Type.PRODUCT,
            image = image,
            siteName = siteName,
            additionalTags = buildMap {
                price?.let { put("product:price:amount", it) }
                currency?.let { put("product:price:currency", it) }
                availability?.let { put("product:availability", it) }
            }
        )
    }
}

/**
 * Twitter Card metadata.
 *
 * @see https://developer.twitter.com/en/docs/twitter-for-websites/cards/overview/abouts-cards
 */
public data class TwitterCard(
    /** Card type */
    val card: Card = Card.SUMMARY,
    /** Title override (defaults to PageMeta.title) */
    val title: String? = null,
    /** Description override (defaults to PageMeta.description) */
    val description: String? = null,
    /** Image URL override (defaults to OpenGraph.image) */
    val image: String? = null,
    /** Alt text for the image */
    val imageAlt: String? = null,
    /** @username of the website */
    val site: String? = null,
    /** @username of the content creator */
    val creator: String? = null,
    /** Additional Twitter tags */
    val additionalTags: Map<String, String> = emptyMap()
) {
    public enum class Card(public val value: String) {
        SUMMARY("summary"),
        SUMMARY_LARGE_IMAGE("summary_large_image"),
        APP("app"),
        PLAYER("player")
    }

    public companion object {
        /**
         * Create a summary card (small square image).
         */
        public fun summary(
            site: String? = null,
            creator: String? = null
        ) = TwitterCard(
            card = Card.SUMMARY,
            site = site,
            creator = creator
        )

        /**
         * Create a summary card with large image.
         */
        public fun summaryLargeImage(
            site: String? = null,
            creator: String? = null,
            image: String? = null
        ) = TwitterCard(
            card = Card.SUMMARY_LARGE_IMAGE,
            site = site,
            creator = creator,
            image = image
        )
    }
}

/**
 * Apply PageMeta to this SsrContext.
 *
 * This sets the title, description, canonical URL, and all meta tags
 * from the PageMeta structure.
 */
public fun SsrContext.applyMeta(meta: PageMeta) {
    meta.title?.let { title = it }
    meta.description?.let { description = it }
    meta.canonicalUrl?.let { canonicalUrl = it }
    metaTags.putAll(meta.toMetaTags())
}

/**
 * DSL builder for PageMeta.
 */
public fun pageMeta(block: PageMetaBuilder.() -> Unit): PageMeta {
    return PageMetaBuilder().apply(block).build()
}

/**
 * Builder for creating PageMeta with DSL syntax.
 */
public class PageMetaBuilder {
    public var title: String? = null
    public var description: String? = null
    public var canonicalUrl: String? = null
    public var robots: String? = null
    public var author: String? = null
    public var keywords: String? = null

    private var openGraph: OpenGraph? = null
    private var twitter: TwitterCard? = null
    private val customTags = mutableMapOf<String, String>()

    /**
     * Configure OpenGraph metadata.
     */
    public fun openGraph(block: OpenGraphBuilder.() -> Unit) {
        openGraph = OpenGraphBuilder().apply(block).build()
    }

    /**
     * Configure Twitter Card metadata.
     */
    public fun twitter(block: TwitterCardBuilder.() -> Unit) {
        twitter = TwitterCardBuilder().apply(block).build()
    }

    /**
     * Add a custom meta tag.
     */
    public fun meta(name: String, content: String) {
        customTags[name] = content
    }

    /**
     * Mark page as noindex (not indexed by search engines).
     */
    public fun noindex() {
        robots = "noindex"
    }

    /**
     * Mark page as noindex and nofollow.
     */
    public fun noindexNofollow() {
        robots = "noindex, nofollow"
    }

    public fun build() = PageMeta(
        title = title,
        description = description,
        canonicalUrl = canonicalUrl,
        openGraph = openGraph,
        twitter = twitter,
        robots = robots,
        author = author,
        keywords = keywords,
        customTags = customTags.toMap()
    )
}

public class OpenGraphBuilder {
    public var type: OpenGraph.Type = OpenGraph.Type.WEBSITE
    public var title: String? = null
    public var description: String? = null
    public var url: String? = null
    public var image: String? = null
    public var imageAlt: String? = null
    public var imageWidth: Int? = null
    public var imageHeight: Int? = null
    public var siteName: String? = null
    public var locale: String? = null
    private val additionalTags = mutableMapOf<String, String>()

    public fun tag(property: String, content: String) {
        additionalTags[property] = content
    }

    public fun build() = OpenGraph(
        type = type,
        title = title,
        description = description,
        url = url,
        image = image,
        imageAlt = imageAlt,
        imageWidth = imageWidth,
        imageHeight = imageHeight,
        siteName = siteName,
        locale = locale,
        additionalTags = additionalTags.toMap()
    )
}

public class TwitterCardBuilder {
    public var card: TwitterCard.Card = TwitterCard.Card.SUMMARY
    public var title: String? = null
    public var description: String? = null
    public var image: String? = null
    public var imageAlt: String? = null
    public var site: String? = null
    public var creator: String? = null
    private val additionalTags = mutableMapOf<String, String>()

    public fun tag(name: String, content: String) {
        additionalTags[name] = content
    }

    public fun build() = TwitterCard(
        card = card,
        title = title,
        description = description,
        image = image,
        imageAlt = imageAlt,
        site = site,
        creator = creator,
        additionalTags = additionalTags.toMap()
    )
}
