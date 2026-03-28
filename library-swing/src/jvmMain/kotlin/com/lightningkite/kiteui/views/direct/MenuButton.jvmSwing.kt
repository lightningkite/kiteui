package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewWriter
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

actual class MenuButton actual constructor(context: RContext) : RView(context) {
    override val driverActions get() = super.driverActions + menuDriverActions()
    private val button = JPanel().apply {
        isOpaque = false
    }

    override val native = button

    private var menuCreator: (Frame.() -> Unit)? = null
    private var currentPopup: JPopupMenu? = null

    actual var enabled: Boolean
        get() = button.isEnabled
        set(value) {
            button.isEnabled = value
            refreshTheming()
        }

    actual fun opensMenu(createMenu: Frame.() -> Unit) {
        menuCreator = createMenu
    }

    actual var requireClick: Boolean = true

    actual var preferredDirection: PopoverPreferredDirection = PopoverPreferredDirection.belowLeft

    init {
        button.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (enabled && requireClick && e.button == MouseEvent.BUTTON1) {
                    showMenu()
                }
            }

            override fun mouseEntered(e: MouseEvent) {
                if (enabled && !requireClick) {
                    showMenu()
                }
            }
        })
    }

    private fun showMenu() {
        val creator = menuCreator ?: return

        // Close any existing popup
        currentPopup?.isVisible = false
        currentPopup = null

        // Create a Frame to hold the menu content
        val menuFrame = FrameImpl(context.split())

        // Create a ViewWriter that writes into the Frame
        val writer = object : ViewWriter() {
            override val representsView: RView = menuFrame
            override val context: RContext = menuFrame.context
            override val coroutineContext get() = this@MenuButton.coroutineContext
            override fun willAddChild(view: RView) {}
            override fun addChild(view: RView) {
                // Add child to the frame using the public API
                menuFrame.addChild(view)
            }
        }

        // Build the menu content
        writer.run { menuFrame.creator() }
        menuFrame.postSetup()

        // Create popup menu and add the frame's native component
        val popup = JPopupMenu().apply {
            isLightWeightPopupEnabled = true
            add(menuFrame.native)

            // Add listener to clear reference when popup is closed
            addPopupMenuListener(object : javax.swing.event.PopupMenuListener {
                override fun popupMenuWillBecomeVisible(e: javax.swing.event.PopupMenuEvent) {}
                override fun popupMenuWillBecomeInvisible(e: javax.swing.event.PopupMenuEvent) {
                    if (currentPopup == this@apply) {
                        currentPopup = null
                    }
                }
                override fun popupMenuCanceled(e: javax.swing.event.PopupMenuEvent) {
                    if (currentPopup == this@apply) {
                        currentPopup = null
                    }
                }
            })
        }

        currentPopup = popup

        // Show the popup based on preferred direction
        val (x, y) = calculatePopupPosition(preferredDirection)
        popup.show(button, x, y)
    }

    private fun calculatePopupPosition(direction: PopoverPreferredDirection): Pair<Int, Int> {
        val width = button.width
        val height = button.height

        // Calculate position based on direction
        return if (direction.horizontal) {
            // Left/Right positioning
            val x = if (direction.after) width else 0
            val y = when (direction.align) {
                Align.Start -> 0
                Align.Center -> height / 2
                Align.End -> height
                Align.Stretch -> 0
            }
            x to y
        } else {
            // Above/Below positioning
            val y = if (direction.after) height else 0
            val x = when (direction.align) {
                Align.Start -> 0
                Align.Center -> width / 2
                Align.End -> width
                Align.Stretch -> 0
            }
            x to y
        }
    }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme[ClickableSemantic]
        if (!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }
}
