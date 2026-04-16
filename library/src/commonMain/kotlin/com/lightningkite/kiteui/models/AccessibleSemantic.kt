package com.lightningkite.kiteui.models

/**
 * Semantic role for an element, used by assistive technologies and for HTML semantic elements.
 *
 * On web, this sets the actual HTML tag (e.g., `<h1>`, `<nav>`, `<main>`) for proper
 * semantics and SEO. On iOS/Android, maps to native accessibility APIs where applicable.
 */
sealed class AccessibleSemantic {
    /** Heading at the given level (1-6). Web: renders as `<h1>`-`<h6>`. */
    data class Heading(val level: Int) : AccessibleSemantic()

    /** Primary content area. Web: `<main>`. */
    data object Main : AccessibleSemantic()

    /** Navigation section. Web: `<nav>`. */
    data object Navigation : AccessibleSemantic()

    /** Site header / banner. Web: `<header>`. */
    data object Banner : AccessibleSemantic()

    /** Footer / content info. Web: `<footer>`. */
    data object ContentInfo : AccessibleSemantic()

    /** Complementary content (sidebar). Web: `<aside>`. */
    data object Complementary : AccessibleSemantic()

    /** Search section. Web: `<search>`. */
    data object Search : AccessibleSemantic()
}
