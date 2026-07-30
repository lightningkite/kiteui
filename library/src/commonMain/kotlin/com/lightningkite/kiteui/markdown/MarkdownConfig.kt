// by Claude
package com.lightningkite.kiteui.markdown

import com.lightningkite.kiteui.models.CardSemantic
import com.lightningkite.kiteui.models.DangerSemantic
import com.lightningkite.kiteui.models.ImportantSemantic
import com.lightningkite.kiteui.models.Semantic
import com.lightningkite.kiteui.models.WarningSemantic
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.Routes
import com.lightningkite.kiteui.navigation.UrlLikePath
import com.lightningkite.kiteui.utils.safeLinkUrlOrNull
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.externalLink
import com.lightningkite.kiteui.views.direct.link
import com.lightningkite.kiteui.views.themed

/**
 * Provides access to markdown parsing capabilities for custom block handlers.
 * This allows custom handlers to parse parts of their content as standard markdown.
 */
public interface MarkdownParseContext {
    /**
     * Parse text as block-level markdown content.
     * @param content The markdown text to parse
     * @return List of parsed block nodes
     */
    public fun parseBlocks(content: String): List<MarkdownNode>

    /**
     * Parse text as inline markdown content.
     * @param content The markdown text to parse
     * @return List of parsed inline nodes
     */
    public fun parseInline(content: String): List<MarkdownNode.InlineNode>
}

/**
 * Provides access to markdown rendering capabilities for custom block handlers.
 * This allows custom handlers to render child nodes and access configuration.
 */
public interface MarkdownRenderContext {
    /**
     * The markdown configuration, useful for resolving internal links.
     */
    public val config: MarkdownConfig

    /**
     * Render a markdown node.
     * Use this to render child nodes within your custom block.
     */
    public fun ViewWriter.renderNode(node: MarkdownNode)
}

/**
 * Handler for custom markdown block types (e.g., ::: note, ::: card).
 *
 * Implement this interface to define both parsing and rendering behavior
 * for custom block types in markdown.
 */
public interface CustomBlockHandler {
    /**
     * Transform the raw block content into a node during parsing.
     * The default implementation creates a CustomBlock with the content parsed as markdown children.
     *
     * @param blockType The type identifier from the ::: declaration (e.g., "note", "card")
     * @param attributes Key-value attributes from the ::: declaration (e.g., href="/path")
     * @param content The raw text content inside the custom block
     * @param context Provides parsing capabilities to parse parts of content as markdown
     * @return The parsed node (typically a CustomBlock, but can be any MarkdownNode)
     */
    public fun parse(
        blockType: String,
        attributes: Map<String, String>,
        content: String,
        context: MarkdownParseContext
    ): MarkdownNode = MarkdownNode.CustomBlock(blockType, attributes, context.parseBlocks(content))

    /**
     * Render the custom block.
     *
     * @param children The parsed child nodes
     * @param attributes The block's attributes (e.g., href from `::: card href="/path"`)
     * @param context Provides config and node rendering capability
     */
    public fun ViewWriter.render(
        children: List<MarkdownNode>,
        attributes: Map<String, String>,
        context: MarkdownRenderContext
    )

    public object Default : JustSemantic(CardSemantic)

    public companion object {
        public val default: Map<String, JustSemantic> = mapOf(
            "card" to CardSemantic,
            "important" to ImportantSemantic,
            "warning" to WarningSemantic,
            "danger" to DangerSemantic,
        ).mapValues { JustSemantic(it.value) }
    }

    public open class JustSemantic(public val semantic: Semantic) : CustomBlockHandler {

        override fun ViewWriter.render(
            children: List<MarkdownNode>,
            attributes: Map<String, String>,
            context: MarkdownRenderContext
        ) {
            val writer = themed(semantic)

            // Wrap in link if href attribute is present
            val href = attributes["href"]
            if (href != null) {
                val page = context.config.resolveInternalLink(href)
                if (page != null) {
                    writer.link {
                        to = { page }
                        col {
                            with(context) { children.forEach { renderNode(it) } }
                        }
                    }
                } else {
                    writer.externalLink {
                        // The href comes from a `::: block href="..."` declaration in untrusted
                        // markdown source, so it gets the same scheme check as inline links.
                        to = safeLinkUrlOrNull(href)
                        col {
                            with(context) { children.forEach { renderNode(it) } }
                        }
                    }
                }
            } else {
                writer.col {
                    with(context) { children.forEach { renderNode(it) } }
                }
            }
        }
    }
}

/**
 * Configuration for markdown rendering.
 *
 * Allows customization of parsing, rendering, and link handling behavior.
 *
 * @param customBlocks Map of custom block type identifiers to handlers.
 *        Used for both parsing and rendering ::: custom blocks.
 * @param internalLinkPattern Regex pattern to detect internal (same-app) URLs.
 *        Matching URLs will use `Link` component for SPA navigation.
 * @param onInternalLink Function to resolve internal URLs to Page instances.
 *        If this returns null, the link will be treated as external.
 */
public data class MarkdownConfig(
    val customBlocks: Map<String, CustomBlockHandler> = CustomBlockHandler.default,
    val internalLinkPattern: Regex? = null,
    val onInternalLink: ((String) -> Page?)? = null,
) {
    public companion object {
        /**
         * Default configuration with no custom handlers or internal link support.
         */
        public val Default: MarkdownConfig = MarkdownConfig()

        /**
         * Creates a configuration that uses the provided Routes to handle internal links.
         *
         * Internal links (URLs starting with "/" or matching the baseUrl) will be parsed
         * using the Routes instance and rendered as SPA navigation links instead of
         * full page reloads.
         *
         * @param routes The Routes instance to parse internal URLs into Pages.
         * @param baseUrl Optional base URL for internal links. Defaults to "/" which matches
         *        all absolute paths. Use a full URL like "https://example.com" to also
         *        match absolute URLs to your domain.
         *
         * Example:
         * ```kotlin
         * // In your app's routes file:
         * val myRoutes = Routes(...)
         *
         * // Create markdown config that handles internal links:
         * val mdConfig = MarkdownConfig.forRoutes(myRoutes)
         *
         * // Use in markdown rendering:
         * markdown("[Go to About](/about)", config = mdConfig)
         * // The "/about" link will use SPA navigation
         * ```
         */
        public fun forRoutes(
            routes: Routes,
            baseUrl: String = "/"
        ): MarkdownConfig {
            val pattern = if (baseUrl == "/") {
                // Match all absolute paths starting with /
                Regex("^/.*")
            } else {
                // Match URLs starting with the base URL or absolute paths
                Regex("^(${Regex.escape(baseUrl)}|/).*")
            }

            return MarkdownConfig(
                internalLinkPattern = pattern,
                onInternalLink = { url ->
                    // Remove the base URL prefix if present to get the path
                    val path = when {
                        baseUrl != "/" && url.startsWith(baseUrl) -> url.removePrefix(baseUrl)
                        else -> url
                    }
                    routes.parseOrNull(UrlLikePath.fromUrlString(path))
                }
            )
        }

        /**
         * Creates a configuration with internal link handling for SPA navigation.
         *
         * @param pattern Regex pattern for internal URLs (e.g., Regex("^/.*") for all paths)
         * @param resolver Function to convert URL to Page instance
         */
        public fun withInternalLinks(
            pattern: Regex,
            resolver: (String) -> Page?
        ): MarkdownConfig = MarkdownConfig(
            internalLinkPattern = pattern,
            onInternalLink = resolver
        )
    }

    /**
     * Combines this config with additional custom block handlers.
     */
    public fun withCustomBlocks(
        blocks: Map<String, CustomBlockHandler>
    ): MarkdownConfig = copy(
        customBlocks = customBlocks + blocks
    )

    /**
     * Creates a new config with internal link handling.
     */
    public fun withInternalLinks(
        pattern: Regex,
        resolver: (String) -> Page?
    ): MarkdownConfig = copy(
        internalLinkPattern = pattern,
        onInternalLink = resolver
    )

    /**
     * Creates a new config that uses the provided Routes to handle internal links.
     *
     * @param routes The Routes instance to parse internal URLs into Pages.
     * @param baseUrl Optional base URL for internal links. Defaults to "/" which matches
     *        all absolute paths.
     */
    public fun forRoutes(
        routes: Routes,
        baseUrl: String = "/"
    ): MarkdownConfig {
        val pattern = if (baseUrl == "/") {
            Regex("^/.*")
        } else {
            Regex("^(${Regex.escape(baseUrl)}|/).*")
        }

        return copy(
            internalLinkPattern = pattern,
            onInternalLink = { url ->
                val path = when {
                    baseUrl != "/" && url.startsWith(baseUrl) -> url.removePrefix(baseUrl)
                    else -> url
                }
                routes.parseOrNull(UrlLikePath.fromUrlString(path))
            }
        )
    }

    /**
     * Determines if a URL should be treated as an internal link.
     */
    public fun isInternalLink(url: String): Boolean {
        val pattern = internalLinkPattern ?: return false
        return pattern.matches(url)
    }

    /**
     * Attempts to resolve an internal URL to a Page.
     * Returns null if the URL is not internal or cannot be resolved.
     */
    public fun resolveInternalLink(url: String): Page? {
        if (!isInternalLink(url)) return null
        return onInternalLink?.invoke(url)
    }
}
