package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.InternalKiteUi
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


/**
 * Base interface for writing elements to the view tree.
 *
 * Provides methods for adding child elements and managing context. The hierarchy of
 * sub-interfaces enforces the canonical modifier order:
 * `alignment.weight.shownWhen.theme.sizing.scrolling.element`
 */
@ViewTreeBuilder
interface ElementWriter : CoroutineScopeHelpers2 {
    val context: ElementContext
    fun willAddChild(element: Element)
    fun addChild(element: Element)

    class Split(parent: ElementWriter): ElementWriter by parent {
        override val context: ElementContext = parent.context.split()
    }

    class BeforeSetup(
        val base: ElementWriter,
        val setup: Element.() -> Unit
    ) : ElementWriter by base {
        override fun willAddChild(element: Element) {
            base.willAddChild(element)
            element.setup()
        }
    }

    // modifier enforcement interfaces to enforce view modifier order
    // canonical order: alignment.weight.shownWhen.theme.sizing.scrolling.element

    /**
     * Allows scrolling modifiers to be applied.
     *
     * Available modifiers:
     * - `scrolling`, `scrollingHorizontally`, `scrollingBoth` - Add scrolling behavior
     * - `scrollingWithRefresh`, `scrollingHorizontallyWithRefresh`, `scrollingBothWithRefresh` - Scrolling with pull-to-refresh
     */
    interface CanAddScrolling : ElementWriter

    /**
     * Allows sizing modifiers to be applied.
     *
     * Available modifiers (in addition to [CanAddScrolling] modifiers):
     * - `sizedBox(constraints)` - Apply size constraints
     * - `sizeConstraints(...)` - Set width/height constraints
     * - `changingSizeConstraints(...)` - Reactive size constraints
     * - `maxHeight(height)` - Set maximum height
     */
    interface CanAddSizing : CanAddScrolling

    /**
     * Allows theme modifiers to be applied.
     *
     * Available modifiers (in addition to [CanAddSizing] modifiers):
     * - Direct theme application: `themed(theme)`
     * - Grouping: `card`, `fieldTheme`, `buttonTheme`, `bar`, `nav`, `group`, `padded`
     * - Emphasis: `important`, `critical`, `warning`, `danger`, `affirmative`, `emphasized`, `compact`
     * - Text styling: `bold`, `italic`, `allCaps`, `strikethrough`, `underline`, `textSize(size)`, `withSpacing(multiplier)`
     */
    interface CanAddTheme : CanAddSizing

    /**
     * Allows visibility modifiers to be applied.
     *
     * Available modifiers (in addition to [CanAddTheme] modifiers):
     * - `shownWhen(default, condition)` - Conditionally show/hide element
     */
    interface CanAddShownWhen : CanAddTheme

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
 * Core function for writing an element to the view tree.
 *
 * This function:
 * 1. Calls [willAddChild] to allow modifiers to be applied
 * 2. Runs the setup block to configure the element
 * 3. Starts the element's lifecycle
 * 4. Adds the element as a child
 *
 * Most DSL functions use this internally to add views to the tree.
 */
@OptIn(ExperimentalContracts::class, InternalKiteUi::class)
inline fun <T : Element> ElementWriter.write(element: T, setup: T.() -> Unit): T {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    willAddChild(element)
    setup(element)
    element.underlyingNativeElement.startup()
    addChild(element)
    return element
}

/**
 * The primary interface for writing views and building UI.
 *
 * This is essentially an [ElementWriter] with unrestricted modifier access, allowing all modifiers to be applied.
 * This is the most permissive interface and is used as the receiver in most DSL functions.
 *
 * All modifiers can be applied in the canonical order:
 * `alignment.weight.shownWhen.theme.sizing.scrolling.element`
 *
 * - Alignment Modifiers: [ElementWriter.CanAddAlignment]
 * - Weight Modifiers: [ElementWriter.CanAddWeight]
 * - Visibility Modifiers: [ElementWriter.CanAddShownWhen]
 * - Theme Modifiers: [ElementWriter.CanAddTheme]
 * - Sizing Modifiers: [ElementWriter.CanAddSizing]
 * - Scrolling Modifiers: [ElementWriter.CanAddScrolling]
 */
interface ViewWriter : ElementWriter.CanAddAlignment