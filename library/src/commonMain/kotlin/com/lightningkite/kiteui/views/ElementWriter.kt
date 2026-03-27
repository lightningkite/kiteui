package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.OverrideOnly
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


/**
 * Base interface for writing elements to the view tree.
 *
 * Provides methods for adding child elements and managing context. The hierarchy of
 * sub-interfaces enforces the canonical modifier order:
 *
 * `alignment.weight.shownWhen.theme.sizing.scrolling.element`
 */
@ViewTreeBuilder
interface ElementWriter : KiteUiCoroutineScopeHelpers {
    val context: ElementContext

    /**
     * Called before [addChild] to add modifiers and to configure the element.
     *
     * You should almost never call this method yourself. Use [ElementWriter.write] to add elements
     * to the view tree.
     *
     * The only case where you should call this yourself is if you are creating a wrapper around [ElementWriter], in
     * this case you __should__ call [willAddChild] on the wrapped writer, typically before your own setup logic
     * in order to keep the order of operations FIFO.
     *
     * ```kotlin
     * class Wrapper(val wraps: ElementWriter) : ElementWriter by wraps {
     *    override fun willAddChild(element: Element) {
     *       // make sure to call the wrapped writer's implementation to complete the chain
     *       wraps.willAddChild(element)
     *       // do something else
     *    }
     * }
     * ```
     */
    @OverrideOnly
    fun willAddChild(element: Element)

    /**
     * Directly add an element to the view hierarchy.
     *
     * You should almost never call this method yourself. Use [ElementWriter.write] to add elements
     * to the view tree.
     *
     * The only case where you should call this yourself is if you are creating a wrapper around [ElementWriter], in
     * this case it makes sense to call [addChild] on the wrapped writer to complete the delegation chain.
     *
     * ```kotlin
     * class Wrapper(val wraps: ElementWriter) : ElementWriter by wraps {
     *    override fun addChild(element: Element) {
     *       // make sure to call the wrapped writer's implementation to complete the chain
     *       wraps.addChild(element)
     *    }
     * }
     * ```
     */
    @OverrideOnly
    fun addChild(element: Element)


    // modifier enforcement interfaces to enforce view modifier order
    // canonical order: alignment.weight.shownWhen.sizing.theme.scrolling.element

    /**
     * Allows scrolling modifiers to be applied.
     *
     * Available modifiers:
     * - `scrolling`, `scrollingHorizontally`, `scrollingBoth` - Add scrolling behavior
     * - `scrollingWithRefresh`, `scrollingHorizontallyWithRefresh`, `scrollingBothWithRefresh` - Scrolling with pull-to-refresh
     */
    interface CanAddScrolling : ElementWriter

    /**
     * Allows dynamic theme modifiers to be applied.
     *
     * Unlike other modifiers, theme modifiers are _repeatable_, meaning that you can apply theming multiple times on a single element.
     * The end result is the sum of all applied themes. Dynamic themes require all static themes to be defined, which is why this
     * is a separate modifier interface.
     *
     * Available modifiers (in addition to [CanAddScrolling] modifiers):
     * - Dynamic theme application: `dynamicThemed(...)`
     */
    interface CanAddDynamicTheme : CanAddScrolling

    /**
     * Allows static theme modifiers to be applied.
     *
     * Unlike other modifiers, theme modifiers are _repeatable_, meaning that you can apply theming multiple times on a single element.
     * The end result is the sum of all applied themes.
     *
     * Available modifiers (in addition to [CanAddDynamicTheme] modifiers):
     * - Direct theme application: `themed(theme)`
     * - Grouping: `card`, `fieldTheme`, `buttonTheme`, `bar`, `nav`, `group`, `padded`
     * - Emphasis: `important`, `critical`, `warning`, `danger`, `affirmative`, `emphasized`, `compact`
     * - Text styling: `bold`, `italic`, `allCaps`, `strikethrough`, `underline`, `textSize(size)`, `withSpacing(multiplier)`
     */
    interface CanAddTheme : CanAddDynamicTheme

    /**
     * Allows sizing modifiers to be applied.
     *
     * Available modifiers (in addition to [CanAddTheme] modifiers):
     * - `sizedBox(constraints)` - Apply size constraints
     * - `sizeConstraints(...)` - Apply size constraints
     * - `changingSizeConstraints(...)` - Reactive size constraints
     * - `maxHeight(height)` - Set maximum height
     */
    interface CanAddSizing : CanAddTheme

    /**
     * Allows visibility modifiers to be applied.
     *
     * Available modifiers (in addition to [CanAddTheme] modifiers):
     * - `shownWhen(default, condition)` - Conditionally show/hide element
     */
    interface CanAddShownWhen : CanAddSizing

    /**
     * Allows weight modifiers to be applied.
     *
     * Available modifiers (in addition to [CanAddShownWhen] modifiers):
     * - `weight(amount)` - Set flex weight in layout
     * - `changingWeight(amount)` - Reactive flex weight
     * - `expanding` - Shorthand for `weight(1f)`
     */
    interface CanAddWeight : CanAddShownWhen

    /**
     * Allows alignment modifiers to be applied. Most permissive modifier interface.
     *
     * Available modifiers (in addition to [CanAddWeight] modifiers):
     * - Basic alignment: `align(horizontal, vertical)`, `gravity(horizontal, vertical)`
     * - Edge alignment: `atStart`, `atEnd`, `atTop`, `atBottom`
     * - Center alignment: `centered`, `centeredHorizontally`, `centeredVertically`
     * - Corner alignment: `atTopStart`, `atTopCenter`, `atTopEnd`, `atCenterStart`, `atCenterEnd`, `atBottomStart`, `atBottomCenter`, `atBottomEnd`
     * - Special: `maxWidthCentered(width)` - Center with max width constraint
     */
    interface CanAddAlignment : CanAddWeight
}

/**
 * Write an element to the view tree, with an optional [setup].
 *
 * This function:
 * 1. Calls [willAddChild] to allow modifiers to be applied
 * 2. Runs the [setup] block to configure the element
 * 3. Starts the element's lifecycle
 * 4. Adds the element as a child in the view hierarchy
 */
@OptIn(ExperimentalContracts::class, InternalKiteUi::class, OverrideOnly::class)
inline fun <T : Element> ElementWriter.write(element: T, setup: T.() -> Unit = {}): T {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    willAddChild(element)
    setup(element)
    element.onStartup()
    addChild(element)
    return element
}

/**
 * Interface for writing views with any modifiers.
 *
 * This is essentially an [ElementWriter] with unrestricted modifier access.
 * This is the most permissive interface allowing all modifiers to be applied in the canonical order:
 *
 * `alignment.weight.shownWhen.sizing.theme.scrolling.element`
 *
 * - Alignment Modifiers: [ElementWriter.CanAddAlignment]
 * - Weight Modifiers: [ElementWriter.CanAddWeight]
 * - Visibility Modifiers: [ElementWriter.CanAddShownWhen]
 * - Theme Modifiers: [ElementWriter.CanAddTheme]
 * - Sizing Modifiers: [ElementWriter.CanAddSizing]
 * - Scrolling Modifiers: [ElementWriter.CanAddScrolling]
 */
interface ViewWriter : ElementWriter.CanAddAlignment