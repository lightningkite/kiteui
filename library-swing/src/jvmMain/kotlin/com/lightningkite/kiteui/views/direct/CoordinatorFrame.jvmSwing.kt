package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension as KiteDimension
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.MutableReactive
import com.lightningkite.reactive.core.Signal
import kotlinx.coroutines.launch
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JLayeredPane
import javax.swing.JPanel
import javax.swing.SwingUtilities

actual class CoordinatorFrame actual constructor(context: RContext) : RView(context) {
    override val native = JLayeredPane().apply {
        layout = CoordinatorLayoutManager()
    }

    private var leftSwipeAction: (suspend () -> Unit)? = null
    private var rightSwipeAction: (suspend () -> Unit)? = null
    private var dragStartX: Int? = null

    init {
        // Add mouse listeners for swipe gestures
        native.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                dragStartX = e.x
            }

            override fun mouseReleased(e: MouseEvent) {
                dragStartX?.let { startX ->
                    val deltaX = e.x - startX
                    val threshold = 100 // pixels

                    if (deltaX < -threshold) {
                        // Left swipe
                        leftSwipeAction?.let { action ->
                            launch { action() }
                        }
                    } else if (deltaX > threshold) {
                        // Right swipe
                        rightSwipeAction?.let { action ->
                            launch { action() }
                        }
                    }
                }
                dragStartX = null
            }
        })
    }

    actual fun bottomSheet(
        peekSize: KiteDimension?,
        partialRatio: Float,
        draggable: Boolean,
        startState: BottomSheetState,
        shouldRemoveExpandedCorners: Boolean,
        blockBehind: Boolean,
        content: ViewWriter.(control: BottomSheetControl) -> Unit
    ) {
        var sheetView: RView? = null
        var backView: RView? = null
        val state = Signal(startState)

        val control = object : BottomSheetControl {
            override val state: MutableReactive<BottomSheetState> = state
            override fun close() {
                sheetView?.let { sheet ->
                    this@CoordinatorFrame.removeChild(sheet)
                }
                backView?.let { back ->
                    this@CoordinatorFrame.removeChild(back)
                }
                sheetView = null
                backView = null
            }
        }

        withoutAnimation {
            if (blockBehind) {
                backView = split().dismissBackground {
                    opacity = 0.3
                    onClick { control.close() }
                }
            }

            sheetView = beforeNextElementSetup {
                if (native is JComponent) {
                    (native as JComponent).putClientProperty("kiteui.bottomSheet", true)
                    (native as JComponent).putClientProperty("kiteui.bottomSheetState", state)
                    (native as JComponent).putClientProperty("kiteui.bottomSheetPartialRatio", partialRatio)
                    (native as JComponent).putClientProperty("kiteui.bottomSheetPeekSize", peekSize)
                }
            }.split().produceOne {
                col { content(control) }
            }
        }

        // Force layout update
        native.revalidate()
        native.repaint()
    }

    actual fun leftSlidingPanel(
        ratio: Float?,
        blockBehind: Boolean,
        content: ViewWriter.(control: SlidingPanelControl) -> Unit
    ) {
        var panelView: RView? = null
        var backView: RView? = null

        val control = object : SlidingPanelControl {
            override fun close() {
                panelView?.let { panel ->
                    this@CoordinatorFrame.removeChild(panel)
                }
                backView?.let { back ->
                    this@CoordinatorFrame.removeChild(back)
                }
                panelView = null
                backView = null
            }
        }

        withoutAnimation {
            if (blockBehind) {
                backView = split().dismissBackground {
                    opacity = 0.3
                    onClick { control.close() }
                }
            }

            panelView = beforeNextElementSetup {
                if (native is JComponent) {
                    (native as JComponent).putClientProperty("kiteui.leftPanel", true)
                    (native as JComponent).putClientProperty("kiteui.panelRatio", ratio ?: 0.3f)
                }
            }.split().align(Align.Start, Align.Stretch).produceOne {
                content(control)
            }
        }

        // Force layout update
        native.revalidate()
        native.repaint()
    }

    actual fun rightSlidingPanel(
        ratio: Float?,
        blockBehind: Boolean,
        content: ViewWriter.(control: SlidingPanelControl) -> Unit
    ) {
        var panelView: RView? = null
        var backView: RView? = null

        val control = object : SlidingPanelControl {
            override fun close() {
                panelView?.let { panel ->
                    this@CoordinatorFrame.removeChild(panel)
                }
                backView?.let { back ->
                    this@CoordinatorFrame.removeChild(back)
                }
                panelView = null
                backView = null
            }
        }

        withoutAnimation {
            if (blockBehind) {
                backView = split().dismissBackground {
                    opacity = 0.3
                    onClick { control.close() }
                }
            }

            panelView = beforeNextElementSetup {
                if (native is JComponent) {
                    (native as JComponent).putClientProperty("kiteui.rightPanel", true)
                    (native as JComponent).putClientProperty("kiteui.panelRatio", ratio ?: 0.3f)
                }
            }.split().align(Align.End, Align.Stretch).produceOne {
                content(control)
            }
        }

        // Force layout update
        native.revalidate()
        native.repaint()
    }

    actual fun onLeftSwipe(action: suspend () -> Unit) {
        leftSwipeAction = action
    }

    actual fun onRightSwipe(action: suspend () -> Unit) {
        rightSwipeAction = action
    }

    override fun internalAddChild(index: Int, view: RView) {
        val container = native
        runOnUiThread {
            // Use JLayeredPane layers to control z-ordering
            // Background panels go in lower layers, sheets/panels in higher layers
            val layer = when {
                view.native is JComponent && (view.native as JComponent).getClientProperty("kiteui.bottomSheet") == true -> JLayeredPane.POPUP_LAYER
                view.native is JComponent && ((view.native as JComponent).getClientProperty("kiteui.leftPanel") == true ||
                                              (view.native as JComponent).getClientProperty("kiteui.rightPanel") == true) -> JLayeredPane.POPUP_LAYER
                else -> JLayeredPane.DEFAULT_LAYER
            }

            container.add(view.native, layer)
            container.revalidate()
            container.repaint()
        }
    }
}

/**
 * Custom layout manager for CoordinatorFrame that handles:
 * - Main content in default layer (fills entire space)
 * - Bottom sheets (positioned at bottom, height depends on state)
 * - Sliding panels (positioned at left/right, width depends on ratio)
 */
private class CoordinatorLayoutManager : LayoutManager {
    override fun addLayoutComponent(name: String?, comp: Component?) {}
    override fun removeLayoutComponent(comp: Component?) {}

    override fun preferredLayoutSize(parent: Container): java.awt.Dimension {
        var maxWidth = 0
        var maxHeight = 0

        for (i in 0 until parent.componentCount) {
            val child = parent.getComponent(i)
            if (child.isVisible) {
                val childSize = child.preferredSize
                maxWidth = maxOf(maxWidth, childSize.width)
                maxHeight = maxOf(maxHeight, childSize.height)
            }
        }

        val insets = parent.insets
        return java.awt.Dimension(
            maxWidth + insets.left + insets.right,
            maxHeight + insets.top + insets.bottom
        )
    }

    override fun minimumLayoutSize(parent: Container): java.awt.Dimension = preferredLayoutSize(parent)

    override fun layoutContainer(parent: Container) {
        val insets = parent.insets
        val availableWidth = parent.width - insets.left - insets.right
        val availableHeight = parent.height - insets.top - insets.bottom

        for (i in 0 until parent.componentCount) {
            val child = parent.getComponent(i)
            if (!child.isVisible) continue

            val isBottomSheet = child is JComponent && child.getClientProperty("kiteui.bottomSheet") == true
            val isLeftPanel = child is JComponent && child.getClientProperty("kiteui.leftPanel") == true
            val isRightPanel = child is JComponent && child.getClientProperty("kiteui.rightPanel") == true

            when {
                isBottomSheet -> {
                    val state = (child as? JComponent)?.getClientProperty("kiteui.bottomSheetState") as? Signal<*>
                    val partialRatio = (child as? JComponent)?.getClientProperty("kiteui.bottomSheetPartialRatio") as? Float ?: 0.5f
                    val peekSize = (child as? JComponent)?.getClientProperty("kiteui.bottomSheetPeekSize") as? KiteDimension

                    val currentState = (state?.value as? BottomSheetState) ?: BottomSheetState.EXPANDED

                    val sheetHeight = when (currentState) {
                        BottomSheetState.EXPANDED -> availableHeight
                        BottomSheetState.PARTIALLY_EXPANDED -> (availableHeight * partialRatio).toInt()
                        BottomSheetState.COLLAPSED -> peekSize?.px?.toInt() ?: 100
                    }

                    val y = insets.top + availableHeight - sheetHeight
                    child.setBounds(insets.left, y, availableWidth, sheetHeight)
                }
                isLeftPanel -> {
                    val ratio = (child as? JComponent)?.getClientProperty("kiteui.panelRatio") as? Float ?: 0.3f
                    val panelWidth = (availableWidth * ratio).toInt()
                    child.setBounds(insets.left, insets.top, panelWidth, availableHeight)
                }
                isRightPanel -> {
                    val ratio = (child as? JComponent)?.getClientProperty("kiteui.panelRatio") as? Float ?: 0.3f
                    val panelWidth = (availableWidth * ratio).toInt()
                    val x = insets.left + availableWidth - panelWidth
                    child.setBounds(x, insets.top, panelWidth, availableHeight)
                }
                else -> {
                    // Default layer - fill entire space
                    val horizontalAlign = if (child is JComponent) child.horizontalAlign else Align.Stretch
                    val verticalAlign = if (child is JComponent) child.verticalAlign else Align.Stretch

                    val prefSize = child.preferredSize
                    val childWidth = when (horizontalAlign) {
                        Align.Stretch -> availableWidth
                        else -> prefSize.width.coerceAtMost(availableWidth)
                    }
                    val childHeight = when (verticalAlign) {
                        Align.Stretch -> availableHeight
                        else -> prefSize.height.coerceAtMost(availableHeight)
                    }

                    val x = when (horizontalAlign) {
                        Align.Start -> insets.left
                        Align.Center -> insets.left + (availableWidth - childWidth) / 2
                        Align.End -> insets.left + (availableWidth - childWidth)
                        Align.Stretch -> insets.left
                    }
                    val y = when (verticalAlign) {
                        Align.Start -> insets.top
                        Align.Center -> insets.top + (availableHeight - childHeight) / 2
                        Align.End -> insets.top + (availableHeight - childHeight)
                        Align.Stretch -> insets.top
                    }

                    child.setBounds(x, y, childWidth, childHeight)
                }
            }
        }
    }
}

actual class CoordinatorDragHandle actual constructor(context: RContext) : RView(context) {
    override val native = object : JPanel() {
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2d = g as Graphics2D
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            val handleWidth = 40
            val handleHeight = 4
            val x = (width - handleWidth) / 2
            val y = (height - handleHeight) / 2

            g2d.color = Color.GRAY
            g2d.fillRoundRect(x, y, handleWidth, handleHeight, handleHeight, handleHeight)
        }
    }.apply {
        preferredSize = Dimension(48, 24)
        minimumSize = Dimension(48, 24)
        background = Color.LIGHT_GRAY
        isOpaque = false
    }
}
