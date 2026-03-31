package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.OverrideOnly
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


/**
 * Base interface for writing elements to the view tree.
 *
 * [ElementWriter] defines how elements are added to the view hierarchy. The most common
 * implementation you'll encounter is [ViewWriter], which allows all modifiers. [Container elements][ContainerElement] like
 * [frames][com.lightningkite.kiteui.views.direct.Frame], [columns][com.lightningkite.kiteui.views.direct.RowOrCol], and [rows][com.lightningkite.kiteui.views.direct.RowOrCol]
 * implement this to manage their children.
 *
 * ## The Modifier System
 *
 * The hierarchy of sub-interfaces enforces the canonical modifier order:
 *
 * `alignment → weight → shownWhen → sizing → theme → scrolling → element`
 *
 * Each modifier returns a more restricted [ElementWriter] interface, preventing modifiers
 * from being applied out of order at compile time.
 *
 * ## Creating Wrappers
 *
 * You can create [ElementWriter] wrappers to intercept or modify element addition - this is
 * how the modifier system works. Common use cases:
 * - Implementing custom modifiers
 * - Tracking or observing element creation
 * - Applying transformations to elements
 *
 * Example wrapper that tracks all added elements:
 * ```kotlin
 * class TrackingWriter(val wraps: ElementWriter) : ElementWriter by wraps {
 *     val addedElements = mutableListOf<Element>()
 *
 *     override fun addChild(element: Element) {
 *         wraps.addChild(element)  // Delegate to the wrapped writer
 *         addedElements.add(element)  // Track the addition
 *     }
 * }
 * ```
 *
 * ## Native Implementations
 *
 * To see how [ElementWriter] is implemented at the platform level, look at:
 * - [NativeContainerElement] - Base class for native containers
 * - Platform-specific container implementations (e.g., `StackElement`, `LinearElement`)
 *
 * @see ViewWriter The most permissive interface allowing all modifiers
 * @see willAddChild Prepares elements before addition
 * @see addChild Adds prepared elements to the hierarchy
 */
@ViewDsl
interface ElementWriter : KiteUiCoroutineScopeHelpers {
    val context: ElementContext

    /**
     * Prepares an element before it's added to the view hierarchy - modifiers are applied here.
     *
     * **Don't call this directly** - use [ElementWriter.write] which handles the lifecycle for you.
     *
     * ## Creating a Wrapper
     *
     * Creating [ElementWriter] wrappers is a common pattern - it's how modifiers work:
     *
     * ```kotlin
     * class AlignmentWrapper(val wraps: ElementWriter, val align: Align) : ElementWriter by wraps {
     *     override fun willAddChild(element: Element) {
     *         wraps.willAddChild(element)         // Delegate first (FIFO order)
     *         element.nativeAlignment = align     // Then apply your logic
     *     }
     * }
     * ```
     *
     * Just delegate most functionality to the wrapped writer, optionally doing setup before or after.
     *
     * ## Implementing Container ElementWriters (Less Common)
     *
     * When creating a new container from scratch, use [willAddChild] to set up the parent-child relationship:
     *
     * ```kotlin
     * override fun willAddChild(element: Element) {
     *     element.underlyingNativeElement.parent = this
     * }
     * ```
     *
     * **Your responsibility:** Prepare the element (apply modifiers, set up relationships)
     *
     * **Not your responsibility:** Adding to native hierarchy/children list (that's [addChild]),
     * or starting lifecycle (the framework handles [Element.onStartup])
     *
     * ## Calling Directly (Rare - Advanced Use Only)
     *
     * If you must call this directly, you are responsible for ensuring:
     * 1. [Element.ensureOutermostElement] was called on the element first
     * 2. [Element.onStartup] is called after this but before [addChild]
     */
    @OverrideOnly
    fun willAddChild(element: Element)

    /**
     * Adds an element to the view hierarchy after [willAddChild] has prepared it.
     *
     * **Don't call this directly** - use [ElementWriter.write] which handles the lifecycle for you.
     *
     * This is where the element is actually inserted into the native view hierarchy. [willAddChild] is called
     * first to prepare, then this completes the addition.
     *
     * ## Implementing ElementWriter
     *
     * Creating [ElementWriter] wrappers is a common pattern - it's how the modifier system works.
     *
     * As long as you delegate calls back to the wrapped writer you can put whatever logic you want in the wrapper.
     * For example, here is a wrapper which collects all written elements into a list.
     *
     * ```
     * class CollectElementsWriter(private val wraps: ElementWriter) : Element by wraps {
     *      val elements = mutableListOf<Element>()
     *
     *      override fun addChild(element: Element) {
     *         wraps.addChild(element)
     *         elements.add(element)
     *      }
     * }
     *
     * ```
     *
     * **What you can assume:** By the time this is called, [willAddChild] and [Element.onStartup]
     * have already run - modifiers are applied and the element is started.
     *
     * ### Non-Wrappers (Advanced)
     *
     * Creating an [ElementWriter] that is not simply delegating to another writer means actually writing
     * to the native view hierarchy. [NativeContainerElement] implements all of this logic already, if you
     * want to know how this works under-the-hood look at the implementation there.
     *
     * One location where you may want to create a 'from-scratch' [ElementWriter] is a custom app-root writer,
     * the writer which adds the root element to the view hierarchy.
     *
     * ## Calling Directly
     *
     * **Don't call this directly in UI code.** Use [ElementWriter.write] instead - it handles the
     * complete lifecycle including [willAddChild], [Element.onStartup], and [addChild] in the correct order.
     *
     * If you must call this directly (rare), you are responsible for ensuring:
     * 1. [Element.ensureOutermostElement] was called on the element first
     * 2. [willAddChild] was called before this
     * 3. [Element.onStartup] was called after [willAddChild] but before this
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

/**
 * Write an element to the view tree, with an optional [setup].
 *
 * This function:
 * 1. Calls `willAddChild` to apply queued modifiers
 * 2. Runs the [setup] block to configure the element
 * 3. Starts the element's lifecycle
 * 4. Adds the element as a child in the view hierarchy
 */
@OptIn(ExperimentalContracts::class, InternalKiteUi::class, OverrideOnly::class)
inline fun <T : Element> ElementWriter.write(element: T, setup: T.() -> Unit = {}): T {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    element.ensureOutermostElement()
    willAddChild(element)
    setup(element)
    element.onStartup()
    addChild(element)
    return element
}