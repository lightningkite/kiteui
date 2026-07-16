package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.models.DropTargetDelegate

/**
 * Base interface for all UI elements in KiteUI.
 *
 * Every UI element in KiteUI implements this interface, whether it's a simple text view, a button,
 * or a complex container. Elements are backed by platform-native views (Android View, iOS UIView,
 * HTML Element, etc.) and provide a unified API for properties, theming, lifecycle, and reactivity.
 *
 * ## Understanding the Element Hierarchy
 *
 * The Element system has several layers:
 * - **[Element]** (this interface) - Pure contract that all UI elements must implement
 * - **[NativeElement]** - Platform-specific implementation that wraps the actual native view
 * - **[NativeElementCommonCode]** - Shared cross-platform logic for lifecycle, theming, etc.
 * - **[ContainerElement]** - Elements that can contain children (like `col`, `row`, `frame`)
 *
 * ```
 * ┌─────────────────────────────────────────────────┐
 * │ Element (interface)                             │ ← You interact with this
 * │ - Common properties (opacity, shown, theme...)  │
 * │ - Lifecycle (onStartup, onShutdown)             │
 * └─────────────────────────────────────────────────┘
 *                      ▲
 *                      │ implements
 * ┌─────────────────────────────────────────────────┐
 * │ NativeElement (expect/actual)                   │ ← Platform-specific rendering
 * │ - Android: Wraps android.view.View              │
 * │ - iOS: Wraps UIView                             │
 * │ - JS: Wraps HTMLElement                         │
 * └─────────────────────────────────────────────────┘
 *                      ▲
 *                      │ extends
 * ┌─────────────────────────────────────────────────┐
 * │ NativeElementCommonCode                         │ ← Shared implementation
 * │ - Lifecycle management                          │
 * │ - Theme pipeline                                │
 * │ - Process tracking                              │
 * └─────────────────────────────────────────────────┘
 * ```
 *
 * ## When Do You Interact With Elements?
 *
 * **Most common use case: Configuring elements in your UI code**
 * ```kotlin
 * col {
 *     text("Hello") {
 *         opacity = 0.8                    // Modifying Element properties
 *         shown = isVisible()
 *         debugName = "greeting-text"
 *     }
 * }
 * ```
 *
 * **Creating custom wrapper elements:**
 * ```kotlin
 * class HighlightableText(inner: TextView) : Element by inner {
 *     var highlighted: Boolean = false
 *         set(value) {
 *             field = value
 *             themeChoice = if (value) ImportantSemantic else ThemeDerivation.None
 *         }
 * }
 * ```
 *
 * ## When Do You Implement Elements?
 *
 * **You typically don't implement Element directly.** Instead:
 * - To create a **custom view**, extend [NativeElement] (see [NativeElement] docs)
 * - To create a **custom container**, extend [NativeContainerElement]
 * - To create a **wrapper** around an existing element, use delegation (see example below)
 *
 * ## Element Lifecycle
 *
 * Elements go through a specific lifecycle:
 *
 * 1. **Construction** - Element is created but not added to the view tree
 * 2. **[willAddChild][ElementWriter.willAddChild]** - Modifiers are applied, parent is set
 * 3. **Setup block** - User's setup lambda runs
 * 4. **[onStartup]** - Element lifecycle starts, reactivity begins
 * 5. **[addChild][ElementWriter.addChild]** - Added to native view hierarchy
 * 6. **Active** - Element is fully running
 * 7. **[onShutdown]** - Removed from view tree, resources released
 *
 * You should **never call [onStartup] or [onShutdown] directly** - the framework manages this.
 * When you remove an element from its parent, `onShutdown` is called automatically.
 *
 * ## Key Properties
 *
 * ### Visibility & Interaction
 * - [opacity] - Visual opacity (0.0 = transparent, 1.0 = opaque)
 * - [shown] - Whether the element is in the layout (false = removed from layout like `display: none`)
 * - [visible] - Whether the element is visible (false = invisible but still takes space)
 * - [ignoreInteraction] - Whether the element ignores touch/click events
 *
 * ### Theming
 * - [themeChoice] - Semantic theme derivations applied to this element (set via modifiers like `themed`, `card`, `important`)
 * - [themeAndBack] - The final computed theme including parent themes and state-based theming
 *
 * ### Padding
 * - [paddingByEdge] - Custom padding override (null = use theme padding)
 * - [safeAreaPadding] - Additional padding for safe areas (notches, home indicator)
 *
 * ### Hierarchy
 * - [parent] - The container element that holds this element (null if root)
 * - [context] - The [ElementContext] providing platform services
 * - [underlyingNativeElement] - The actual [NativeElement] implementation (important for wrappers)
 *
 * ### Debugging
 * - [debugName] - Human-readable name for debugging and logging
 * - [toString] - Returns debugName or auto-generated description
 *
 * ## Common Patterns
 *
 * ### Storing Element References
 * ```kotlin
 * class MyPage : Page {
 *     private lateinit var statusText: TextView
 *
 *     override fun ElementWriter.CanAddTheme.render() = col {
 *         text("Status") { statusText = this }
 *         button { text("Update") }.onClick {
 *             statusText.content = "Updated!"
 *         }
 *     }
 * }
 * ```
 *
 * ### Element Delegation (Wrapping)
 * ```kotlin
 * class LoggingButton(inner: Button) : Element by inner, InteractiveElement by inner {
 *     init {
 *         onClick {
 *             println("Button clicked: ${debugName}")
 *             // original onClick still fires
 *         }
 *     }
 * }
 * ```
 *
 * ### Reactive Element Properties
 * ```kotlin
 * val isLoading = Property(false)
 * val loadingIndicator: Element = ...
 *
 * loadingIndicator::shown { isLoading() }
 * ```
 *
 * ## Important Notes
 *
 * - **Thread safety**: Element properties should only be accessed from the main/UI thread
 * - **Lifecycle**: Don't access element properties before [onStartup] or after [onShutdown]
 * - **Delegation**: When using delegation, the framework tracks the "outermost" wrapper automatically
 * - **Parent**: The parent is automatically set when the element is added to a container
 *
 * @see NativeElement for creating custom platform-specific views
 * @see ContainerElement for elements that can contain children
 * @see ElementWriter for how elements are added to the view tree
 * @see ElementContext for platform services and configuration
 */
@ViewDsl
// Element is a CoroutineScope (via KiteUiCoroutineScopeHelpers) but deliberately NOT a
// StatusListener/CoroutineContext.Element. Being both at once made an element an entry inside its
// own coroutineContext, so `element.job` (CoroutineScope.job) was ambiguous with the element's own
// StatusListener identity and child jobs could be mis-parented. NativeElement supplies a *separate*
// StatusListener object into its coroutineContext instead (see NativeElementCommonCode).
interface Element : KiteUiCoroutineScopeHelpers {
    /** Platform services and configuration for this element */
    val context: ElementContext

    /**
     * The underlying [NativeElement] that provides the actual platform-specific implementation.
     *
     * This is important when using element delegation/wrapping. If you create a wrapper element:
     * ```kotlin
     * class Wrapper(inner: TextView) : Element by inner { ... }
     * ```
     * Then `underlyingNativeElement` will point to the actual `TextView`, not the wrapper.
     * The framework uses this to access the native view even when elements are wrapped.
     */
    val underlyingNativeElement: NativeElement

    /**
     * The container element that holds this element, or null if this is a root element.
     *
     * Set automatically by the framework when the element is added to a container.
     * Used for theme inheritance and accessing parent properties.
     *
     * **Do not set this manually** - it's managed by [ContainerElement.addChild].
     */
    val parent: ContainerElement?

    /**
     * Called when the element's lifecycle starts.
     *
     * This happens after modifiers are applied and the setup block runs, but before the element
     * is added to the native view hierarchy. At this point:
     * - Reactive processes start running
     * - The element's theme is calculated
     * - Coroutines launched in the element's scope begin executing
     *
     * **Do not call this directly** - the framework calls it via [ElementWriter.write].
     *
     * When implementing custom elements, you can override this to perform initialization:
     * ```kotlin
     * override fun onStartup() {
     *     super.onStartup()
     *     // Your initialization here
     * }
     * ```
     */
    @OverrideOnly
    fun onStartup()

    /**
     * Called when the element is removed from the view tree and its lifecycle ends.
     *
     * This happens when:
     * - The element is removed from its parent container
     * - The parent is shut down
     * - The entire view tree is torn down
     *
     * At shutdown:
     * - All coroutines in the element's scope are cancelled
     * - Reactive processes stop
     * - Resources are released
     * - Parent is set to null
     *
     * **Do not call this directly** - it's called automatically when removing elements.
     *
     * When implementing custom elements, you can override this to clean up resources:
     * ```kotlin
     * override fun onShutdown() {
     *     myResource.close()
     *     super.onShutdown()
     * }
     * ```
     */
    @OverrideOnly
    fun onShutdown()

    /**
     * Visual opacity of the element (0.0 = fully transparent, 1.0 = fully opaque).
     *
     * Animates smoothly when changed on most platforms.
     * ```kotlin
     * element.opacity = 0.5  // 50% transparent
     * ```
     */
    var opacity: Double

    /**
     * Whether the element is shown (present in the layout).
     *
     * - `true` - Element is visible and takes up space in layout
     * - `false` - Element is hidden and removed from layout (like CSS `display: none`)
     *
     * This is different from [visible] - when `shown = false`, the element doesn't take up
     * any space, whereas `visible = false` keeps the space but makes it invisible.
     *
     * ```kotlin
     * element.shown = false  // Removes from layout
     * ```
     */
    var shown: Boolean

    /**
     * Whether the element is visible (but still in the layout).
     *
     * - `true` - Element is visible
     * - `false` - Element is invisible but still takes up space (like CSS `visibility: hidden`)
     *
     * Use [shown] instead if you want to remove the element from the layout entirely.
     */
    var visible: Boolean

    /**
     * Whether the element ignores user interaction (touch, click, focus, etc.).
     *
     * When `true`, the element won't respond to user input. Visual appearance is unchanged -
     * use theme modifiers like `disabled` if you want visual feedback.
     *
     * ```kotlin
     * button.ignoreInteraction = true  // Button can't be clicked
     * ```
     */
    var ignoreInteraction: Boolean

    /**
     * Custom padding override for this element, or null to use theme padding.
     *
     * When set, overrides the padding from the current theme. Does not cascade
     * to children.
     *
     * ```kotlin
     * element.paddingByEdge = Edges(top = 10.px, bottom = 10.px)
     * ```
     *
     * @see safeAreaPadding
     */
    var paddingByEdge: Edges?

    /**
     * Additional padding for device safe areas (notches, home indicators, etc.).
     *
     * This is added to [paddingByEdge] (or theme padding) to create the final padding.
     * Usually set automatically by platform code for root elements.
     *
     * ```kotlin
     * element.safeAreaPadding = Edges(top = 44.px)  // iPhone notch
     * ```
     */
    var safeAreaPadding: Edges?

    /**
     * The semantic theme derivations applied to this element by modifiers.
     *
     * Set by theme modifiers like [themed], `card`, `important`, `critical`, etc. Can also be set
     * programmatically, but this is uncommon - use modifiers instead.
     *
     * The framework combines this with parent themes and element state (loading, working)
     * to produce the final [themeAndBack].
     *
     * ```kotlin
     * // Via modifiers (preferred):
     * card - important - button { }
     *
     * // Programmatically (rare):
     * element.themeChoice = CardSemantic + ImportantSemantic
     * ```
     *
     * @see themeAndBack for the final computed theme
     */
    var themeChoice: ThemeDerivation

    /**
     * The final computed theme for this element.
     *
     * This is the result of combining:
     * 1. Base theme (from parent)
     * 2. [themeChoice] (from modifiers)
     * 3. Dynamic themes (from [dynamicThemed])
     * 4. Element state (selected, checked, disabled)
     * 5. Processing state (loading, working)
     *
     * **Read-only** - to change the theme, use theme modifiers or [themeChoice] if modifiers won't work for your situation.
     *
     * Access the theme:
     * ```kotlin
     * val bgColor = element.themeAndBack.theme.background
     * ```
     */
    val themeAndBack: ThemeAndBack

    /**
     * Data that can be dragged from this element, or null if not draggable.
     *
     * Setting this makes the element draggable (usually via long-press on mobile).
     * ```kotlin
     * element.dragData = DragData(
     *     label = "Item",
     *     data = mapOf("text/plain" to "Item data")
     * )
     * ```
     */
    var dragData: DragData?

    /**
     * Delegate that handles drag-and-drop operations when items are dragged over this element.
     *
     * Implement [DropTargetDelegate] to handle drag enter, over, drop, etc.
     * ```kotlin
     * element.dropTargetDelegate = object : DropTargetDelegate {
     *     override fun drop(event: DragEvent): Boolean {
     *         // Handle the drop
     *         return true
     *     }
     * }
     * ```
     */
    var dropTargetDelegate: DropTargetDelegate?

    /**
     * Scrolls this element into view within its scrollable ancestor.
     *
     * @param horizontal Horizontal alignment (null = don't scroll horizontally)
     * @param vertical Vertical alignment (null = don't scroll vertically)
     * @param animate Whether to animate the scroll
     *
     * ```kotlin
     * element.scrollIntoView(
     *     horizontal = Align.Center,
     *     vertical = Align.Start,
     *     animate = true
     * )
     * ```
     */
    fun scrollIntoView(horizontal: Align?, vertical: Align?, animate: Boolean = true)

    /**
     * Requests keyboard focus for this element.
     *
     * Useful for text inputs, buttons, etc. The element must be focusable.
     * ```kotlin
     * textInput.requestFocus()  // Keyboard appears
     * ```
     */
    fun requestFocus()

    /**
     * Human-readable name for debugging and logging.
     *
     * Shows up in debug output, error messages, and developer tools.
     * ```kotlin
     * text("Welcome") { debugName = "welcome-message" }
     * ```
     */
    var debugName: String?

    /**
     * Whether this element should be visible when printing (web platform).
     *
     * Currently only affects web. When false, the element is hidden in print media.
     */
    var showOnPrint: Boolean

    // --- ACCESSIBILITY ---

    /**
     * Explicit accessible label override.
     *
     * When null (default), the platform auto-derives a label from the element's content:
     * - Button/Link: from `action.title`
     * - TextInput/TextArea: from associated `label()` or `hint`
     * - IconView/ImageView: from `description`
     *
     * Set this only when the auto-derived label is insufficient (e.g., a complex custom widget).
     */
    var accessibleLabel: String?

    /**
     * Marks this element as a live region for screen reader announcements of dynamic content.
     *
     * When content inside this element changes, assistive technologies will announce the update.
     *
     * Use via the `liveRegion()` modifier:
     * ```kotlin
     * liveRegion() - col { text { ::content { statusMessage() } } }
     * ```
     */
    var accessibleLiveRegion: LiveRegionMode

    /**
     * Associates this element as the accessible label for another element.
     *
     * On web, creates a `<label for="id">` association. On Android, sets `labelFor`.
     * On iOS, copies this element's text to the target's `accessibilityLabel`.
     *
     * Automatically set by [field][com.lightningkite.kiteui.views.l2.field] and [label][com.lightningkite.kiteui.views.l2.label] when they contain an interactive element.
     */
    var labelFor: Element?

    /**
     * Associates this element with a description element (e.g., error text, help text).
     *
     * On web, sets `aria-describedby` linking to the description element.
     * On Android, stores the association for components that announce description changes.
     * On iOS, sets `accessibilityHint` from the description element's text.
     *
     * Automatically set by [errorText][com.lightningkite.kiteui.views.l2.errorText] and
     * [issueText][com.lightningkite.kiteui.reactive.issueText] for form validation.
     */
    var describedBy: Element?

    val driverValue: String? get() = null
    val driverActions: Map<String, suspend (List<String>) -> String> get() = AiDriver.Defaults.defaultDriverActions(this)
    fun driverDisplay(options: DriverSnapshotOptions): String = AiDriver.Defaults.defaultDriverDisplay(this, options)

    override fun toString(): String

    companion object;

    data class DriverSnapshotOptions(
        val includeHidden: Boolean = false,
        val interactiveOnly: Boolean = false,
        val includeThemes: Boolean = false,
        val includeActions: Boolean = true,
    )

    object Debugger {
        var removeBeforeShutdown = false
        var leakDetect = false
        var debugTarget: Element? = null

        /**
         * When true, every [NativeElement] increments a live-instance counter on creation and
         * decrements it on shutdown. After navigating away from a page and back, the totals should
         * return to their baseline; a monotonic climb means elements are being leaked (their
         * [Element.onShutdown] never ran). This signal is GC-independent, so it is reliable on
         * platforms where forcing a garbage collection isn't possible (e.g. the browser).
         */
        var countInstances = false
        val liveInstancesByClass: MutableMap<String, Int> = mutableMapOf()
        var liveInstanceTotal: Int = 0
            private set

        fun recordCreated(element: Element) {
            liveInstanceTotal++
            val k = element::class.simpleName ?: "?"
            liveInstancesByClass[k] = (liveInstancesByClass[k] ?: 0) + 1
        }

        fun recordShutdown(element: Element) {
            liveInstanceTotal--
            val k = element::class.simpleName ?: "?"
            val v = (liveInstancesByClass[k] ?: 0) - 1
            if (v <= 0) liveInstancesByClass.remove(k) else liveInstancesByClass[k] = v
        }
    }
}

/**
 * An element that has child elements, but cannot add or remove them.
 *
 * This interface is for elements that internally manage children but don't expose
 * child management methods to external code.
 *
 * For example, [SwapView][com.lightningkite.kiteui.views.direct.SwapView]
 * has children, but children in a `SwapView` should only be added through the `swap`
 * method. This makes `SwapView` a view with children but without the ability to add
 * children directly.
 *
 * If you need to dynamically add/remove children, use [ContainerElement] instead.
 *
 * ## Example: Internal-Only Children
 * ```kotlin
 * class CustomSwitch(context: ElementContext) : NativeElement(context), ElementWithChildren {
 *     private val track: Element = ...
 *     private val thumb: Element = ...
 *
 *     override val children: List<Element> = listOf(track, thumb)
 *
 *     // Children are managed internally, not exposed for modification
 * }
 * ```
 *
 * @see ContainerElement for elements that allow external child management
 */
interface ElementWithChildren : Element {
    /** List of child elements. Read-only - children cannot be added or removed. */
    val children: List<Element>
}

/**
 * An element that can contain and manage child elements.
 *
 * Container elements are the building blocks of layout - they hold other elements and arrange them
 * according to their layout strategy. Examples: `col`, `row`, `frame`, custom layouts.
 *
 * Containers automatically:
 * - Manage child lifecycle (starting and shutting down children)
 * - Propagate theme changes to children
 * - Handle native view hierarchy updates
 * - Implement [ViewWriter] for DSL-style child creation
 *
 * ## Built-in Containers
 *
 * KiteUI provides several standard containers:
 * - `col` - Vertical linear layout (like CSS flexbox column)
 * - `row` - Horizontal linear layout (like CSS flexbox row)
 * - `frame` - Stack/overlay layout (children layered on top of each other)
 *
 * Most elements that implement [ContainerElement] behave like a `frame` unless
 * otherwise specified (i.e. [Button][com.lightningkite.kiteui.views.direct.Button]).
 *
 * ## Using Containers in DSL
 *
 * Because [ContainerElement] extends [ViewWriter], you can add children using the DSL:
 *
 * ```kotlin
 * fun ViewWriter.myCustomWidget() = col {  // col is a ContainerElement
 *     text("Title")      // Added as child
 *     button {           // Added as child
 *         text("Click")
 *     }
 * }
 * ```
 *
 * ## Creating Custom Containers
 *
 * To create a custom container, extend [NativeContainerElement]:
 *
 * ```kotlin
 * // Common code
 * expect class GridLayout(context: ElementContext) : NativeContainerElement
 *
 * // Platform-specific implementation (Android example)
 * actual class GridLayout actual constructor(context: ElementContext) :
 *     NativeContainerElement(context) {
 *
 *     override val native = android.widget.GridLayout(context.activity)
 *
 *     // other platform-specific needs are specified by the platform's NativeContainerElement impl
 * }
 * ```
 *
 * ## Theme Propagation
 *
 * Containers automatically propagate theme changes to their children. When a container's
 * cascading theme changes (via `card`, `important`, etc.), all children refresh their themes.
 *
 * @see NativeContainerElement for implementing custom platform-specific containers
 * @see ViewWriter for the DSL interface containers provide
 * @see Element for the base element interface
 */
interface ContainerElement : Element, ElementWithChildren, ViewWriter {
    /**
     * The underlying platform-specific container implementation.
     *
     * Like [Element.underlyingNativeElement], but guaranteed to be a [NativeContainerElement].
     */
    override val underlyingNativeElement: NativeContainerElement

    /**
     * List of child elements in this container, in order.
     */
    override val children: List<Element>

    /**
     * Adds a child element at the specified index.
     *
     * **⚠️ WARNING: Don't call this directly!** This bypasses the element lifecycle.
     *
     * ## ✅ Use the DSL instead:
     * ```kotlin
     * col {
     *     text("Child")  // Proper lifecycle via ElementWriter.write()
     * }
     * ```
     *
     * ## ❌ Don't call directly:
     * ```kotlin
     * container.addChild(child)  // Skips modifiers, onStartup, etc.
     * ```
     *
     * Calling this directly skips critical lifecycle steps: modifier application,
     * [Element.onStartup], delegation tracking, and setup blocks. The element will
     * appear in the view tree but be partially broken (no reactivity, no modifiers).
     *
     * **See [ElementWriter.addChild] for complete documentation** on the element lifecycle
     * and when it's appropriate to call container methods directly (spoiler: almost never).
     *
     * @param index The position to insert the child (0 = first, children.size = last)
     * @param element The child element to add
     * @throws IndexOutOfBoundsException if index is invalid
     *
     * @see ElementWriter.write for the correct way to add elements
     * @see ElementWriter.addChild for complete lifecycle documentation
     */
    @OverrideOnly
    fun addChild(index: Int, element: Element)

    /**
     * Removes the child element at the specified index.
     *
     * The child is:
     * 1. Removed from the native view hierarchy
     * 2. Removed from the [children] list
     * 3. [onShutdown][Element.onShutdown] is called on the child
     * 4. The child's [parent][Element.parent] is set to null
     *
     * **The child is shut down** - all its reactive processes stop and resources are released.
     *
     * @param index The index of the child to remove
     * @throws IndexOutOfBoundsException if index is invalid
     */
    fun removeChild(index: Int)

    /**
     * Adds a child element at the end of the children list.
     *
     * **⚠️ WARNING: Don't call this directly!** Bypasses element lifecycle.
     * Use the DSL instead. See [addChild(Int, Element)][addChild] and [ElementWriter.addChild]
     * for complete documentation.
     *
     * Equivalent to `addChild(children.size, element)`.
     */
    @OverrideOnly
    override fun addChild(element: Element) = addChild(children.size, element)

    /**
     * Removes the specified child element from this container.
     *
     * Finds the child in the [children] list and removes it. If the child is not
     * found, throws an exception.
     *
     * @param element The child element to remove
     * @throws IllegalArgumentException if the element is not a child of this container
     */
    fun removeChild(element: Element) {
        val i = children.indexOf(element)
        if (i != -1) removeChild(i)
        else throw IllegalArgumentException("$element is not a child of $this!")
    }

    /**
     * Removes all child elements from this container.
     *
     * All children are shut down and removed from the native view hierarchy.
     * Equivalent to calling [removeChild] for each child, but may be more efficient.
     */
    fun clearChildren() { for (i in children.indices.reversed()) removeChild(i) }



    /** Spacing used for child [CornerRadii.RatioOfSpacing] and [CornerRadii.AdaptiveToSpacing] calculations. */
    @Deprecated("Will probably be removed in the future.")
    val spacingForChildCornerRadii: Dimension get() {
        val pad = padding ?: themeAndBack.theme.padding.top
        val gap = themeAndBack.theme.gap
        return minOf(pad, gap)
    }
}