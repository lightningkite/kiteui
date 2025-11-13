package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.exceptions.ExceptionHandlers
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope

/**
 * Base class for all views in the KiteUI framework.
 *
 * RView is a platform-agnostic view abstraction that delegates rendering to platform-specific implementations.
 * Each platform (Android, iOS, JS, JVM) provides its own `actual` implementation of this `expect` class.
 *
 * RView extends [RViewHelper] which provides the core view functionality including:
 * - Hierarchy management (parent/child relationships)
 * - Theming and styling
 * - Lifecycle management
 * - Exception handling
 * - Reactive state management
 * - Coroutine scope integration
 *
 * **IMPORTANT:** RViews must be created on the main/UI thread. Platform implementations enforce this requirement.
 *
 * @param context The rendering context that provides platform-specific configuration and capabilities.
 *
 * @see RViewHelper for the common implementation details
 * @see RViewWriter for views that delegate gap handling to their parent
 * @see RViewWithAction for views that need to track action states
 */
expect abstract class RView constructor(context: RContext) : RViewHelper {
    /**
     * Whether this view should be visible when printing.
     * Platform implementations may handle this differently (e.g., CSS media queries on web).
     */
    override var showOnPrint: Boolean

    /**
     * Scrolls this view into the visible area of its nearest scrollable ancestor.
     *
     * @param horizontal The horizontal alignment target, or null to not scroll horizontally.
     * @param vertical The vertical alignment target, or null to not scroll vertically.
     * @param animate Whether to animate the scroll transition.
     */
    override fun scrollIntoView(horizontal: Align?, vertical: Align?, animate: Boolean)

    /**
     * Requests keyboard focus for this view.
     * The actual behavior is platform-specific and may be delayed slightly to ensure proper rendering.
     */
    override fun requestFocus()

    /**
     * Returns the absolute screen rectangle for this view, or null if the view is not currently visible.
     * Coordinates are relative to the screen/window origin.
     */
    override fun screenRectangle(): Rect?

    /**
     * Applies the given theme to this view's visual appearance.
     * This method is called automatically when the theme changes and should update
     * platform-specific rendering properties.
     *
     * @param theme The theme and background rendering configuration to apply.
     */
    override fun applyTheme(theme: ThemeAndBack)

    /**
     * Platform-specific implementation for adding a child view at a specific index.
     * **IMPORTANT:** This should only be called by [RViewHelper.addChild], not directly.
     *
     * @param index The index at which to insert the child.
     * @param view The child view to add.
     */
    override fun internalAddChild(index: Int, view: RView)

    /**
     * Platform-specific implementation for removing a child view at a specific index.
     * **IMPORTANT:** This should only be called by [RViewHelper.removeChild], not directly.
     *
     * @param index The index of the child to remove.
     */
    override fun internalRemoveChild(index: Int)

    /**
     * Platform-specific implementation for removing all child views.
     * **IMPORTANT:** This should only be called by [RViewHelper.clearChildren], not directly.
     */
    override fun internalClearChildren()
}

/**
 * A specialized view that delegates its gap (spacing) property to its parent view.
 *
 * This is useful for wrapper views that should inherit spacing from their container
 * rather than defining their own spacing behavior.
 */
abstract class RViewWriter(context: RContext) : RView(context) {
    /**
     * Returns the explicitly set gap, or if null, delegates to the parent's gap.
     * This allows wrapper views to transparently inherit spacing from their containers.
     */
    override var gap: Dimension? = null
        get() = field ?: parent?.gap
}

/**
 * A view that manages an [Action] and automatically listens to its working state.
 *
 * This base class handles the lifecycle of action state listeners, automatically
 * cleaning them up when the view is removed or when a new action is set.
 */
abstract class RViewWithAction(context: RContext) : RView(context) {
    private var actionStatusRemove: (() -> Unit)? = null
    init { onRemove { actionStatusRemove?.invoke(); actionStatusRemove = null } }

    /**
     * The action associated with this view. When set, the view will automatically
     * listen to the action's working state and update accordingly.
     */
    var action: Action? = null
        set(value) {
            field = value
            actionSet(value)
        }

    /**
     * Called when the action is changed. Override this to customize how the view
     * responds to action state changes.
     *
     * @param value The new action, or null if the action was cleared.
     */
    open fun actionSet(value: Action?) {
        actionStatusRemove?.invoke()
        actionStatusRemove = value?.let { listenForWorking(it) }
    }
}

/**
 * A view that manages both a primary and secondary [Action], listening to both their working states.
 *
 * A secondary action is typically a right-click or long-tap.
 */
abstract class RViewWithSecondaryAction(context: RContext) : RViewWithAction(context) {
    private var secondaryActionStatusRemove: (() -> Unit)? = null
    init { onRemove { secondaryActionStatusRemove?.invoke(); secondaryActionStatusRemove = null } }

    /**
     * The secondary action associated with this view. When set, the view will automatically
     * listen to this action's working state in addition to the primary action.
     */
    var secondaryAction: Action? = null
        set(value) {
            field = value
            secondaryActionSet(value)
        }

    /**
     * Called when the secondary action is changed. Override this to customize how the view
     * responds to secondary action state changes.
     *
     * @param value The new secondary action, or null if it was cleared.
     */
    open fun secondaryActionSet(value: Action?) {
        secondaryActionStatusRemove?.invoke()
        secondaryActionStatusRemove = value?.let { listenForWorking(it) }
    }
}

/**
 * Returns whether animations are currently enabled for this view.
 * This is a platform-specific property that respects system-wide animation settings.
 */
expect val RView.areAnimationsEnabled: Boolean

/**
 * Executes the given action with animations temporarily disabled.
 *
 * This is useful when you need to make immediate visual changes without transitions,
 * such as during initial setup or when responding to rapid state changes.
 *
 * @param action The code to execute without animations.
 */
expect inline fun RView.withoutAnimation(action: () -> Unit)
