package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.Frame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Root handle returned by [elementTree].  Provides direct access to the live
 * element tree for structural and theme assertions without going through the
 * driver text protocol.
 *
 * @property root The root [Frame] that holds all top-level children written by
 *   the [content] block.  Cast to [ContainerElement] to walk [children].
 * @property context The [ElementContext] used during construction.
 */
class ElementTreeHandle(
    val root: Frame,
    val context: ElementContext,
) {
    /** All children of the root frame. */
    val children: List<Element> get() = root.children

    /** Convenience: first child, or throws if the root has no children. */
    val firstChild: Element get() = children.first()

    /**
     * Recursively walks the tree and returns all elements in breadth-first
     * order (root excluded).
     */
    fun allDescendants(): List<Element> {
        val result = mutableListOf<Element>()
        fun visit(e: Element) {
            result.add(e)
            if (e is ElementWithChildren) e.children.forEach { visit(it) }
        }
        children.forEach { visit(it) }
        return result
    }

    /**
     * Returns the first descendant with the given [debugName], or null.
     */
    fun findByName(name: String): Element? =
        allDescendants().firstOrNull { it.debugName == name }

    /**
     * Collects [themeAndBack] from every descendant in breadth-first order.
     * Useful for asserting that a specific pattern of backgrounds is or isn't drawn.
     */
    fun allThemeAndBacks(): List<ThemeAndBack> = allDescendants().map { it.themeAndBack }

    /**
     * Tears down the element tree, triggering [Element.onShutdown] on the root
     * (which cascades to all children).  Call this to verify no elements leak
     * when [Element.Debugger.countInstances] is enabled.
     */
    @OptIn(OverrideOnly::class)
    fun shutdown() = root.onShutdown()
}

/**
 * A minimal, non-placeholder theme suitable for tests that need to observe
 * theme cascade behavior.  [Theme.placeholder] causes cascade deduplication
 * to always trigger (initial value equals computed value), preventing child
 * theme refresh.  Using this default avoids that problem.
 */
val ElementTestTheme = Theme(id = "test")

/**
 * Builds a live element tree for unit testing structural and theme invariants.
 *
 * Uses the same bootstrap sequence as [com.lightningkite.kiteui.testing.uiTest]:
 * creates a real [ElementContext], starts the root frame, sets the root theme,
 * writes children, and lets the theme pipeline cascade correctly.  The returned
 * [ElementTreeHandle] exposes the root and helpers for tree inspection.
 *
 * **Important:** A non-placeholder [rootTheme] is required for theme-cascade
 * tests.  [Theme.placeholder] has a coincidental initial value that prevents the
 * cascade from triggering.  [ElementTestTheme] (the default) works correctly.
 *
 * Usage:
 * ```kotlin
 * val tree = elementTree {
 *     card.col {
 *         debugName = "outer"
 *         col { debugName = "inner" }
 *     }
 * }
 * val outer = tree.findByName("outer")!!
 * val inner = tree.findByName("inner")!!
 * assertTrue(outer.drawsBackground)   // card draws a background
 * assertFalse(inner.drawsBackground)  // plain child does not
 * ```
 *
 * @param rootTheme Theme to apply at the root.  Must be non-placeholder for
 *   theme-cascade assertions to work correctly.  Defaults to [ElementTestTheme].
 * @param content The DSL block to populate the tree.
 * @return A handle to the live element tree.
 */
@OptIn(ExperimentalCoroutinesApi::class, OverrideOnly::class)
fun elementTree(
    rootTheme: Theme = ElementTestTheme,
    content: ViewWriter.() -> Unit,
): ElementTreeHandle {
    Dispatchers.setMain(Dispatchers.Unconfined)
    try {
        val context = ElementContext("/")
        val root = Frame(context)

        // Start root first so the parent-active check passes when children start.
        root.onStartup()

        // Apply the root theme after startup so it cascades to children.
        root.themeChoice = ThemeDerivation.SetAsBase(rootTheme)

        with(root) { content() }

        return ElementTreeHandle(root, context)
    } finally {
        Dispatchers.resetMain()
    }
}

// ---------------------------------------------------------------------------
// Tree-traversal helpers available in tests without importing the handle class
// ---------------------------------------------------------------------------

/**
 * Returns the element's theme id (from [ThemeAndBack.theme]).
 * Shorthand for `element.themeAndBack.theme.id`.
 */
val Element.themeId: String get() = themeAndBack.theme.id

/**
 * Whether this element draws a background (its [ThemeAndBack.drawBackground] is true).
 */
val Element.drawsBackground: Boolean get() = themeAndBack.drawBackground

/**
 * All direct children of this element, or an empty list if not a container.
 */
val Element.childElements: List<Element>
    get() = (this as? ElementWithChildren)?.children ?: emptyList()

/**
 * Recursively collects all descendants of this element (breadth-first, not including self).
 */
fun Element.allDescendants(): List<Element> {
    val result = mutableListOf<Element>()
    fun visit(e: Element) {
        result.add(e)
        if (e is ElementWithChildren) e.children.forEach { visit(it) }
    }
    childElements.forEach { visit(it) }
    return result
}
