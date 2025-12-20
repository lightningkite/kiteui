package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.*
import java.awt.Component
import java.awt.Container
import java.awt.Point
import java.awt.Rectangle
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.dnd.*
import javax.swing.*

actual abstract class RView actual constructor(context: RContext) : RViewHelper(context) {
    abstract val native: Component

    init {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw Exception("Cannot create views on any thread but the EDT (Event Dispatch Thread)")
        }
    }

    actual override var showOnPrint: Boolean = true

    override var opacity: Double
        get() = super.opacity
        set(value) {
            super.opacity = value
            if (native is JComponent) {
                (native as JComponent).apply {
                    isOpaque = value >= 1.0
                    // Swing doesn't have direct alpha support, but we can use AlphaComposite in custom painting
                    // For now, just handle fully transparent vs opaque
                }
            }
        }

    override var shown: Boolean
        get() = super.shown
        set(value) {
            super.shown = value
            native.isVisible = value
        }

    override var visible: Boolean
        get() = super.visible
        set(value) {
            super.visible = value
            // Swing doesn't have separate visible/invisible states like Android
            // We'll use the same as shown for now
            if (!value && shown) {
                native.isVisible = false
            } else if (value && shown) {
                native.isVisible = true
            }
        }

    override var gap: Dimension?
        get() = super.gap
        set(value) {
            super.gap = value
            // Gap is typically handled by layout managers
            // Trigger layout update if needed
            if (native is Container) {
                (native as Container).revalidate()
            }
        }

    override var ignoreInteraction: Boolean
        get() = super.ignoreInteraction
        set(value) {
            super.ignoreInteraction = value
            native.isEnabled = !value
            native.isFocusable = !value
        }

    override var paddingByEdge: Edges?
        get() = super.paddingByEdge
        set(value) {
            super.paddingByEdge = value
            // Padding is typically handled via borders in Swing
            if (native is JComponent && value != null) {
                val insets = java.awt.Insets(
                    value.top.px.toInt(),
                    value.left.px.toInt(),
                    value.bottom.px.toInt(),
                    value.right.px.toInt()
                )
                (native as JComponent).border = BorderFactory.createEmptyBorder(
                    insets.top, insets.left, insets.bottom, insets.right
                )
            }
        }

    override var transitionId: String?
        get() = super.transitionId
        set(value) {
            super.transitionId = value
            // Store as client property for potential transition animations
            if (native is JComponent) {
                (native as JComponent).putClientProperty("transitionId", value)
            }
        }

    // Drag and drop support
    override var dragData: DragData?
        get() = super.dragData
        set(value) {
            super.dragData = value
            if (value == null) {
                if (native is JComponent) {
                    (native as JComponent).transferHandler = null
                }
            } else {
                if (native is JComponent) {
                    val comp = native as JComponent
                    comp.transferHandler = object : TransferHandler() {
                        override fun getSourceActions(c: JComponent): Int = COPY_OR_MOVE

                        override fun createTransferable(c: JComponent): Transferable {
                            return object : Transferable {
                                override fun getTransferDataFlavors(): Array<DataFlavor> {
                                    return arrayOf(DataFlavor.stringFlavor)
                                }

                                override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
                                    return flavor == DataFlavor.stringFlavor
                                }

                                override fun getTransferData(flavor: DataFlavor): Any {
                                    return value.data ?: ""
                                }
                            }
                        }
                    }
                }
            }
        }

    actual override fun scrollIntoView(horizontal: Align?, vertical: Align?, animate: Boolean) {
        if (native is JComponent) {
            (native as JComponent).scrollRectToVisible(native.bounds)
        }
    }

    actual override fun requestFocus() {
        runOnUiThread {
            native.requestFocusInWindow()
        }
    }

    actual override fun screenRectangle(): Rect? {
        if (!native.isShowing) return null

        val location = native.locationOnScreen
        val size = native.size

        return Rect(
            left = location.x.toDouble(),
            top = location.y.toDouble(),
            right = (location.x + size.width).toDouble(),
            bottom = (location.y + size.height).toDouble()
        )
    }

    actual override fun applyTheme(theme: ThemeAndBack) {
        // Apply theme to component
        if (native is JComponent) {
            val comp = native as JComponent
            // Store the theme for subclasses to use
            comp.putClientProperty("kiteui.theme", theme)

            // Apply background if drawBackground is true
            if (theme.drawBackground) {
                val backgroundColor = theme.theme.background.closestColor()
                comp.background = backgroundColor.toAwt()
                comp.isOpaque = true
            } else {
                comp.isOpaque = false
            }
        }
    }

    actual override fun internalAddChild(index: Int, view: RView) {
        if (native is Container) {
            val container = native as Container
            runOnUiThread {
                container.add(view.native, index)
                container.revalidate()
                container.repaint()
            }
        } else {
            throw UnsupportedOperationException("Cannot add children to ${native.javaClass.simpleName}")
        }
    }

    actual override fun internalRemoveChild(index: Int) {
        if (native is Container) {
            val container = native as Container
            runOnUiThread {
                if (index < container.componentCount) {
                    container.remove(index)
                    container.revalidate()
                    container.repaint()
                }
            }
        }
    }

    actual override fun internalClearChildren() {
        if (native is Container) {
            val container = native as Container
            runOnUiThread {
                container.removeAll()
                container.revalidate()
                container.repaint()
            }
        }
    }
}

// Extension to convert Dimension to pixels
val Dimension.px: Double
    get() = value // Dimension already has a value property in pixels

// Animation support
var animationsEnabled: Boolean = false
actual val RView.areAnimationsEnabled: Boolean get() = animationsEnabled
actual inline fun RView.withoutAnimation(action: () -> Unit) {
    if (!animationsEnabled) {
        action()
        return
    }
    try {
        animationsEnabled = false
        action()
    } finally {
        animationsEnabled = true
    }
}
