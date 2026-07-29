@file:OptIn(InternalKiteUi::class, ExperimentalKiteUi::class, OverrideOnly::class)

package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack

/**
 * Platform-specific native container element implementation.
 *
 * This is the base class for all container elements (elements that can hold children).
 * It extends [NativeElement] with child management functionality. Each platform provides
 * its own implementation for adding/removing children in the native view hierarchy.
 *
 * ## Built-in Containers
 *
 * KiteUI provides these standard containers:
 * - `LinearLayout` - Base for `col` (vertical) and `row` (horizontal) layouts
 * - `Frame` - Stack layout where children overlay each other
 * - `ScrollView` - Scrollable container (from `scrolling` modifier)
 * - `SwapView` - Container that shows one child at a time
 * - `ProgrammaticLayout` - Custom layout with programmatic positioning
 *
 * ## Creating a Custom Container
 *
 * To create a custom container, you need to implement child management for each platform:
 *
 * ### Step 1: Common Declaration
 * ```kotlin
 * // In commonMain
 * expect class GridLayout(context: ElementContext) : NativeContainerElement {
 *     var columns: Int
 * }
 *
 * // DSL function
 * inline fun ViewWriter.grid(
 *     columns: Int = 2,
 *     setup: GridLayout.() -> Unit = {}
 * ): GridLayout = write(GridLayout(context).apply { this.columns = columns }, setup)
 * ```
 *
 * ### Step 2: Platform Implementation (Android Example)
 * ```kotlin
 * // In androidMain
 * actual class GridLayout actual constructor(context: ElementContext) :
 *     NativeContainerElement(context) {
 *
 *     // The native Android view
 *     override val native = android.widget.GridLayout(context.activity)
 *
 *     actual var columns: Int
 *         get() = native.columnCount
 *         set(value) { native.columnCount = value }
 *
 *     override fun nativeAddChild(index: Int, element: Element) {
 *         // Add to native view hierarchy
 *         native.addView(element.native, index)
 *     }
 *
 *     override fun nativeRemoveChild(index: Int) {
 *         // Remove from native view hierarchy
 *         native.removeViewAt(index)
 *     }
 *
 *     override fun nativeClearChildren() {
 *         // Remove all children from native view hierarchy
 *         native.removeAllViews()
 *     }
 * }
 * ```
 *
 * ### Step 3: Repeat for Other Platforms
 * Implement the same for iOS, JS, and other platforms.
 *
 * ## What You Must Implement
 *
 * Platform implementations must provide:
 *
 * ### [nativeAddChild]
 * Add a child to the native view hierarchy at the specified index.
 * - The child's [Element.parent] has already been set
 * - The child is already in the [children] list
 * - Just add it to your platform's native container
 *
 * ### [nativeRemoveChild]
 * Remove a child from the native view hierarchy by index.
 * - Don't modify the [children] list (already done)
 * - Don't call [Element.onShutdown] (already done)
 * - Just remove from your platform's native container
 *
 * ### [nativeClearChildren]
 * Remove all children from the native view hierarchy.
 * - [children] will be cleared automatically
 * - Children will be shut down automatically
 * - Just clear your platform's native container
 *
 * ## What You Get For Free
 *
 * [NativeContainerElementCommonCode] provides:
 * - **Child list management** - The [children] list is maintained automatically
 * - **Child lifecycle** - Children are started/stopped at the right times
 * - **Theme cascading** - Theme changes automatically propagate to children
 * - **Parent tracking** - The [Element.parent] property is set correctly
 * - **Wrapper support** - Works correctly with element delegation
 *
 * ## Important: parentElement vs this
 *
 * Containers track both `this` and `parentElement`:
 * - `this` - The actual [NativeContainerElement]
 * - `parentElement` - The outermost wrapper (if the container is wrapped via delegation)
 *
 * When setting children's [Element.parent], use `parentElement`, not `this`:
 *
 * ```kotlin
 * // In NativeContainerElementCommonCode (already done for you):
 * override fun willAddChild(element: Element) {
 *     element.underlyingNativeElement.parent = parentElement  // Not 'this'!
 * }
 * ```
 *
 * This ensures that if your container is wrapped, children see the wrapper as their parent,
 * not the inner container. This is handled automatically - you don't need to worry about it
 * unless you're doing advanced customization.
 *
 * ## Layout Implementation
 *
 * Container elements often need to handle layout. How you do this depends on the platform:
 *
 * **Android**: Use existing layouts or create custom `ViewGroup`
 * ```kotlin
 * override val native = object : ViewGroup(context.activity) {
 *     override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
 *         // Position children
 *     }
 * }
 * ```
 *
 * **iOS**: Use Auto Layout or override `layoutSubviews`
 * ```kotlin
 * override val native = object : UIView() {
 *     override fun layoutSubviews() {
 *         super.layoutSubviews()
 *         // Position children
 *     }
 * }
 * ```
 *
 * **Web**: Use CSS flexbox/grid or manual positioning
 * ```kotlin
 * override val native = document.createElement("div").apply {
 *     style.display = "flex"
 *     style.flexDirection = "column"
 * }
 * ```
 *
 * ## Example: Simple Overlay Container
 *
 * ```kotlin
 * // Common
 * expect class OverlayContainer(context: ElementContext) : NativeContainerElement
 *
 * // Android
 * actual class OverlayContainer actual constructor(context: ElementContext) :
 *     NativeContainerElement(context) {
 *
 *     override val native = FrameLayout(context.activity)  // Children stack on top of each other
 *
 *     override fun nativeAddChild(index: Int, element: Element) {
 *         native.addView(element.native, index)
 *     }
 *
 *     override fun nativeRemoveChild(index: Int) {
 *         native.removeViewAt(index)
 *     }
 *
 *     override fun nativeClearChildren() {
 *         native.removeAllViews()
 *     }
 * }
 * ```
 *
 * @see NativeContainerElementCommonCode for shared implementation
 * @see ContainerElement for the public interface
 * @see NativeElement for the base element class
 */
public expect abstract class NativeContainerElement(context: ElementContext) : ContainerElement, NativeContainerElementCommonCode {
    /**
     * Adds a child element to the native view hierarchy at the specified index.
     *
     * This is called after:
     * - The child has been added to the [children] list
     * - The child's [Element.parent] has been set
     *
     * Your implementation should only add the child to your platform's native container.
     *
     * @param index The index where the child should be inserted
     * @param element The child element to add
     */
    override fun nativeAddChild(index: Int, element: Element)

    /**
     * Removes a child element from the native view hierarchy by index.
     *
     * This is called before:
     * - The child is removed from the [children] list
     * - The child's [Element.onShutdown] is called
     *
     * Your implementation should only remove the child from your platform's native container.
     *
     * @param index The index of the child to remove
     */
    override fun nativeRemoveChild(index: Int)

    /**
     * Removes all children from the native view hierarchy.
     *
     * This is called before:
     * - The [children] list is cleared
     * - All children are shut down
     *
     * Your implementation should only clear your platform's native container.
     */
    override fun nativeClearChildren()
}

/**
 * Shared platform-independent code for native container elements.
 *
 * This is only directly inherited by [NativeContainerElement]. Any instance of this class is also a [NativeContainerElement].
 *
 * # Implementation Details
 *
 * This abstract class extends [NativeElement] with child management functionality that works
 * the same across all platforms. Platform-specific [NativeContainerElement] implementations
 * inherit from this to get container features for free.
 *
 * ## What This Class Provides
 *
 * ### 1. Child List Management
 * Maintains the [children] list automatically:
 * - Adds children when [addChild] is called
 * - Removes children when [removeChild] is called
 * - Provides read-only access via [children]
 *
 * ### 2. Child Lifecycle
 * Manages child startup and shutdown:
 * - Children are kept alive while in the container
 * - [Element.onShutdown] is called automatically when removing children
 * - All children are shut down when the container is shut down
 *
 * ### 3. Theme Cascading
 * Automatically propagates theme changes to children:
 * - When a container's cascading theme changes (via `card`, `important`, etc.),
 *   all children refresh their themes
 * - Non-cascading themes don't propagate
 * - The `revert` theme property controls cascading
 *
 * ### 4. Parent Tracking
 * Sets [Element.parent] correctly for all children:
 * - Uses `parentElement` (outermost wrapper) instead of `this`
 * - Supports element delegation pattern
 * - Parent is set in [willAddChild] before native hierarchy update
 *
 * ### 5. ElementWriter Implementation
 * Implements [ContainerElement] which extends [ViewWriter], so containers can be used
 * in the DSL to add children:
 * ```kotlin
 * col {  // col is a container
 *     text("Child 1")
 *     text("Child 2")
 * }
 * ```
 *
 * ## The Delegation Pattern for Containers
 *
 * Containers support element delegation just like regular elements, but with an extra
 * consideration - the `parentElement` field:
 *
 * ```kotlin
 * class AnimatedColumn(inner: LinearLayout) : ContainerElement by inner {
 *     init {
 *         // When children are added, they see this wrapper as their parent,
 *         // not the inner LinearLayout. This happens automatically.
 *     }
 * }
 * ```
 *
 * When [outermostElement] is set (during [ElementWriter.write]), the `parentElement` field
 * is also updated:
 * ```kotlin
 * override var outermostElement: Element = this
 *     set(value) {
 *         parentElement = value as? ContainerElement ?: this
 *         field = value
 *     }
 * ```
 *
 * This ensures children see the wrapper as their parent, which is important for:
 * - Theme inheritance (children inherit from the wrapper's theme)
 * - Property access (children can cast `parent` to the wrapper type)
 * - Debug information (toString shows the wrapper, not inner element)
 *
 * ## Child Management Flow
 *
 * When you call [addChild]:
 * 1. [willAddChild] is called (in [ElementWriter])
 *    - Sets `child.parent = parentElement`
 *    - Calls [nativeWillAddChild] hook
 * 2. Child is added to [children] list
 * 3. [nativeAddChild] is called
 *    - Platform adds child to native view hierarchy
 * 4. Parent is double-checked and set to `parentElement` if needed
 *
 * When you call [removeChild]:
 * 1. [nativeRemoveChild] is called
 *    - Platform removes child from native view hierarchy
 * 2. Child is removed from [children] list
 * 3. `child.onShutdown()` is called
 *    - Child's lifecycle ends
 *
 * ## Shutdown Behavior
 *
 * When the container is shut down:
 * - If [Element.Debugger.removeBeforeShutdown] is true:
 *   - Children are removed one by one via [removeChild]
 *   - This allows observing the shutdown process for debugging
 * - Otherwise:
 *   - All children are shut down in-place
 *   - [children] list is cleared
 *   - More efficient for production
 *
 * ## Theme Cascading Details
 *
 * Containers override [themeAndBack] to detect when the cascading theme changes:
 *
 * ```kotlin
 * override var themeAndBack: ThemeAndBack = Theme.placeholder.withBack
 *     set(value) {
 *         val oldCascading = field.theme.let { it.revert ?: it }
 *         field = value
 *         nativeApplyTheme(value)
 *         refreshPadding()
 *         val newCascading = value.theme.let { it.revert ?: it }
 *         if (oldCascading != newCascading) {
 *             // Theme changed - tell all children to refresh
 *             for (child in children) child.underlyingNativeElement.refreshTheming()
 *         }
 *     }
 * ```
 *
 * The `revert` property indicates the theme that children should inherit. If the new
 * and old revert themes differ, all children refresh their themes.
 *
 * ## You Should Not Extend This Directly
 *
 * **Don't extend [NativeContainerElementCommonCode] directly.** Instead:
 * - Extend [NativeContainerElement] for custom containers
 * - The expect/actual mechanism ensures you inherit this automatically
 *
 * @see NativeContainerElement for creating custom containers
 * @see ContainerElement for the public interface
 * @see NativeElement for the base element class
 */
public abstract class NativeContainerElementCommonCode internal constructor(context: ElementContext) : NativeElement(context), ContainerElement {
    override val underlyingNativeElement: NativeContainerElement get() = this as NativeContainerElement

    // --- CHILDREN ---

    private val internalChildren = ArrayList<Element>()

    /**
     * Read-only list of child elements.
     *
     * Backed by [internalChildren] which is managed automatically. Do not modify directly.
     */
    override val children: List<Element> get() = internalChildren

    /**
     * Platform-specific child addition to native view hierarchy.
     *
     * Called after the child is added to [children]. Just add to your native container.
     */
    protected abstract fun nativeAddChild(index: Int, element: Element)

    /**
     * Optional hook called before a child is added to native hierarchy.
     *
     * Override this if you need to prepare the child or native container before addition.
     * Default implementation does nothing.
     */
    protected open fun nativeWillAddChild(element: Element) {}

    /**
     * Platform-specific child removal from native view hierarchy.
     *
     * Called before the child is removed from [children] and shut down.
     * Just remove from your native container.
     */
    protected abstract fun nativeRemoveChild(index: Int)

    /**
     * Platform-specific removal of all children from native view hierarchy.
     *
     * Called before [children] is cleared and children are shut down.
     * Just clear your native container.
     */
    protected abstract fun nativeClearChildren()

    /**
     * The outermost element if this container is wrapped via delegation.
     *
     * If someone creates a wrapper via delegation:
     * ```
     * class AnimatedCol(inner: LinearLayout) : ContainerElement by inner
     * ```
     * Then `parentElement` will be the `AnimatedCol`, not the inner `LinearLayout`.
     *
     * This ensures children see the wrapper as their parent, not the inner container.
     */
    private var parentElement: ContainerElement = this

    /**
     * Tracks the outermost wrapper element when using delegation.
     *
     * When set, also updates [parentElement] to the outermost [ContainerElement] wrapper
     * (or `this` if the wrapper isn't a container).
     *
     * This ensures that when children are added, their [Element.parent] is set to the
     * wrapper, not the inner native container.
     */
    final override var outermostElement: Element = this
        set(value) {
            parentElement = value as? ContainerElement ?: this
            field = value
        }

    /**
     * Implementation of [ElementWriter.willAddChild] for containers.
     *
     * This is called by the framework (via [ElementWriter.write]) before adding a child.
     * Sets the child's [Element.parent] to [parentElement] (the outermost wrapper).
     *
     * **Don't call this directly** - it's part of the internal element lifecycle.
     * Use [ElementWriter.write] or the DSL to add children.
     *
     * @see ElementWriter.willAddChild for general documentation
     */
    override fun willAddChild(element: Element) {
        if (checkIsShutdown("willAddChild")) return
        element.underlyingNativeElement.parent = parentElement
        nativeWillAddChild(element)
    }

    /**
     * Implementation of [ContainerElement.addChild] for native containers.
     *
     * This is called by the framework after [willAddChild] and [Element.onStartup].
     * Adds the child to [children] list and native view hierarchy.
     *
     * **Don't call this directly** - it's part of the internal element lifecycle.
     * Use [ElementWriter.write] or the DSL to add children.
     *
     * @see ContainerElement.addChild for complete documentation and warnings
     */
    override fun addChild(index: Int, element: Element) {
        if (checkIsShutdown("addChild")) return
        internalChildren.add(index, element)
        nativeAddChild(index, element)
        if (element.parent?.underlyingNativeElement !== this) {
            element.underlyingNativeElement.parent = parentElement
        }
    }

    /** Convenience wrapper - adds child at the end. See [addChild(Int, Element)][addChild]. */
    override fun addChild(element: Element) = addChild(children.size, element)

    override fun removeChild(index: Int) {
        if (checkIsShutdown("removeChild")) return
        if (index !in children.indices) throw IndexOutOfBoundsException("$index not in range ${children.indices}")
        nativeRemoveChild(index)
        internalChildren.removeAt(index).onShutdown()
    }

    override fun removeChild(element: Element) {
        if (checkIsShutdown("removeChild")) return
        val i = children.indexOf(element)
        if (i != -1) {
            nativeRemoveChild(i)
            internalChildren.removeAt(i).onShutdown()
        }
        else throw IllegalArgumentException("$element is not a child of $this!")
    }

    override fun clearChildren() {
        if (checkIsShutdown("clearChildren")) return
        nativeClearChildren()
        for (e in internalChildren) e.onShutdown()
        internalChildren.clear()
    }



    // --- LIFECYCLE ---

    override fun onShutdown() {
        if (isShutdown) return
        if (Element.Debugger.removeBeforeShutdown) {
            for (index in internalChildren.lastIndex downTo 0) {
                removeChild(index)
            }
        } else {
            internalChildren.forEach { it.onShutdown() }
            internalChildren.clear()
        }
        super.onShutdown()
    }


    // --- THEMING ---

    final override var themeAndBack: ThemeAndBack = Theme.placeholder.withBack
        set(value) {
            if (value == field) return
            val oldCascading = field.theme.let { it.revert ?: it }
            field = value       // Do not call super.themeAndBack = value, it breaks everything for some reason
            nativeApplyTheme(value)
            refreshPadding()
            val newCascading = value.theme.let { it.revert ?: it }
            if (oldCascading != newCascading) {
                for (child in children) child.underlyingNativeElement.refreshTheming()
            }
        }
}