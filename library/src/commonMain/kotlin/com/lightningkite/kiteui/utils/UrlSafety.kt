package com.lightningkite.kiteui.utils

/**
 * URL schemes considered safe to navigate to from content the application did not author.
 *
 * Everything outside this set is rejected. An allow-list is used rather than a deny-list
 * because new script-bearing schemes appear over time, and a deny-list silently fails open.
 *
 * The dangerous schemes differ per platform, which is why one shared list covers all of them:
 * `javascript:` and `data:` execute script or render an attacker-controlled document on web;
 * `intent:` and `file:` reach other applications and local storage on Android; a custom scheme
 * registered by any installed app is reachable through `UIApplication.openURL` on iOS. Allowing
 * only the handful of schemes that mean "show the user a document, a mail draft or a dialer"
 * covers all three without needing a per-platform list.
 */
public val safeLinkSchemes: Set<String> = setOf("http", "https", "mailto", "tel", "sms")

/**
 * Whether [url] is safe to use as a link target.
 *
 * Relative and absolute-path URLs are safe, as are URLs carrying one of [safeLinkSchemes].
 * `javascript:` executes script and `data:` renders an attacker-controlled document, so both
 * are rejected.
 *
 * Use this for any URL that originates outside the application: markdown, a CMS field, a user
 * profile, an API response. Application-authored constants do not need checking, though passing
 * them through is harmless.
 *
 * Where this is enforced: every sink that hands a URL to the platform applies it - `ExternalLink.to`
 * on all four platforms, and the raw-HTML sanitizer's `href` handling. That is the security
 * boundary, so application code gets the protection without opting in. [MarkdownRenderer] also
 * checks before it builds a link, but for a different reason: it can render an unsafe URL as plain
 * text, whereas a sink can only refuse to act on a click. The two are not redundant - one decides
 * how untrusted content is presented, the other guarantees nothing dangerous is ever handed to the
 * OS or browser.
 *
 * Browsers strip ASCII whitespace and C0 control characters from URLs before resolving the
 * scheme, so `java\tscript:alert(1)` reaches the same handler as `javascript:alert(1)`. Those
 * characters are removed here before the scheme is read, rather than being treated as
 * separators that would make the string look scheme-less.
 */
public fun isSafeLinkUrl(url: String): Boolean {
    val cleaned = url.filter { it.code > 0x20 && it.code != 0x7F }
    val colon = cleaned.indexOf(':')
    if (colon < 0) return true
    // A path, query or fragment delimiter before the colon means the colon is part of the
    // path rather than a scheme separator, e.g. "/a:b" or "?x=1:2".
    val delimiter = cleaned.indexOfFirst { it == '/' || it == '?' || it == '#' }
    if (delimiter in 0 until colon) return true
    return cleaned.substring(0, colon).lowercase() in safeLinkSchemes
}

/**
 * Returns [url] if [isSafeLinkUrl] accepts it, otherwise null.
 *
 * Dropping the target entirely is deliberate: rewriting it to something like "#" would leave a
 * link that looks functional but silently goes nowhere, which is harder to notice than a plain
 * piece of non-linked text.
 */
public fun safeLinkUrlOrNull(url: String?): String? =
    url?.takeIf { isSafeLinkUrl(it) }
