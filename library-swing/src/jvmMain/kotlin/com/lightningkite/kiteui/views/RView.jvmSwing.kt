package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.direct.RoundedBorder
import java.awt.Component
import java.awt.Container
import java.awt.Point
import java.awt.Rectangle
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.dnd.*
import javax.swing.*
import kotlin.math.min
import kotlin.math.roundToInt

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
            // Set client property so MouseTransparentPanel can check it in contains()
            if (native is JComponent) {
                (native as JComponent).putClientProperty("kiteui.ignoreInteraction", value)
            }
        }

    override var accessibleLabel: String?
        get() = super.accessibleLabel
        set(value) {
            super.accessibleLabel = value
            native.accessibleContext?.accessibleName = value
        }

    override var accessibleHeading: Int?
        get() = super.accessibleHeading
        set(value) {
            super.accessibleHeading = value
            // Swing's AccessibleContext doesn't have a native heading concept.
            // Store as client property for potential assistive technology bridge.
            if (native is JComponent) {
                (native as JComponent).putClientProperty("kiteui.accessibleHeading", value)
            }
        }

    override var accessibleLiveRegion: LiveRegionMode
        get() = super.accessibleLiveRegion
        set(value) {
            super.accessibleLiveRegion = value
            // Swing doesn't have native live region support.
            // Store as client property for potential assistive technology bridge.
            if (native is JComponent) {
                (native as JComponent).putClientProperty("kiteui.accessibleLiveRegion", value.name)
            }
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

    // Child alignment defaults - sync to native component for layout managers to read
    override var newChildHorizontalAlign: Align?
        get() = super.newChildHorizontalAlign
        set(value) {
            super.newChildHorizontalAlign = value
            if (native is JComponent) {
                (native as JComponent).putClientProperty("kiteui.newChildHorizontalAlign", value)
            }
        }

    override var newChildVerticalAlign: Align?
        get() = super.newChildVerticalAlign
        set(value) {
            super.newChildVerticalAlign = value
            if (native is JComponent) {
                (native as JComponent).putClientProperty("kiteui.newChildVerticalAlign", value)
            }
        }

    // Drag and drop support
    private var dragGestureRecognizer: DragGestureRecognizer? = null
    private var currentDragGestureListener: DragGestureListener? = null

    override var dragData: DragData?
        get() = super.dragData
        set(value) {
            super.dragData = value
            if (native is JComponent) {
                val comp = native as JComponent

                // Remove existing gesture recognizer if any
                dragGestureRecognizer?.let { recognizer ->
                    currentDragGestureListener?.let { listener ->
                        recognizer.removeDragGestureListener(listener)
                    }
                    dragGestureRecognizer = null
                    currentDragGestureListener = null
                }

                if (value == null) {
                    comp.transferHandler = null
                } else {
                    // Create transferable for the drag data
                    val transferable = object : Transferable {
                        override fun getTransferDataFlavors(): Array<DataFlavor> {
                            return arrayOf(DataFlavor.stringFlavor)
                        }

                        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
                            return flavor == DataFlavor.stringFlavor
                        }

                        override fun getTransferData(flavor: DataFlavor): Any {
                            return value.data
                        }
                    }

                    // Set up drag gesture recognizer to initiate the drag
                    val dragSource = DragSource.getDefaultDragSource()
                    val listener = DragGestureListener { dge ->
                        dragSource.startDrag(
                            dge,
                            DragSource.DefaultCopyDrop,
                            transferable,
                            object : DragSourceListener {
                                override fun dragEnter(dsde: DragSourceDragEvent) {}
                                override fun dragOver(dsde: DragSourceDragEvent) {}
                                override fun dropActionChanged(dsde: DragSourceDragEvent) {}
                                override fun dragExit(dse: DragSourceEvent) {}
                                override fun dragDropEnd(dsde: DragSourceDropEvent) {}
                            }
                        )
                    }
                    currentDragGestureListener = listener
                    dragGestureRecognizer = dragSource.createDefaultDragGestureRecognizer(
                        comp,
                        DnDConstants.ACTION_COPY_OR_MOVE,
                        listener
                    )
                }
            }
        }

    // Drop target support
    override var dropTargetDelegate: DropTargetDelegate?
        get() = super.dropTargetDelegate
        set(value) {
            super.dropTargetDelegate = value
            if (native is JComponent) {
                val comp = native as JComponent
                if (value == null) {
                    comp.dropTarget = null
                } else {
                    comp.dropTarget = DropTarget(comp, DnDConstants.ACTION_COPY_OR_MOVE, object : DropTargetListener {
                        private fun DropTargetDragEvent.toDragEvent(): DragEvent {
                            val transferable = transferable
                            val typeToData = mutableMapOf<String, String>()
                            for (flavor in transferable.transferDataFlavors) {
                                if (flavor == DataFlavor.stringFlavor) {
                                    try {
                                        val data = transferable.getTransferData(flavor) as? String
                                        if (data != null) {
                                            typeToData["text/plain"] = data
                                        }
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                }
                            }
                            return DragEvent(
                                data = DragData("", typeToData),
                                xInView = location.x.toDouble(),
                                yInView = location.y.toDouble()
                            )
                        }

                        private fun DropTargetDropEvent.toDragEvent(): DragEvent {
                            val transferable = transferable
                            val typeToData = mutableMapOf<String, String>()
                            for (flavor in transferable.transferDataFlavors) {
                                if (flavor == DataFlavor.stringFlavor) {
                                    try {
                                        val data = transferable.getTransferData(flavor) as? String
                                        if (data != null) {
                                            typeToData["text/plain"] = data
                                        }
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                }
                            }
                            return DragEvent(
                                data = DragData("", typeToData),
                                xInView = location.x.toDouble(),
                                yInView = location.y.toDouble()
                            )
                        }

                        override fun dragEnter(dtde: DropTargetDragEvent) {
                            if (value.enter(dtde.toDragEvent())) {
                                dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE)
                            } else {
                                dtde.rejectDrag()
                            }
                        }

                        override fun dragOver(dtde: DropTargetDragEvent) {
                            if (value.over(dtde.toDragEvent())) {
                                dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE)
                            } else {
                                dtde.rejectDrag()
                            }
                        }

                        override fun dropActionChanged(dtde: DropTargetDragEvent) {
                            // No-op
                        }

                        override fun dragExit(dte: DropTargetEvent) {
                            val fakeEvent = DragEvent(
                                data = DragData("", emptyMap()),
                                xInView = 0.0,
                                yInView = 0.0
                            )
                            value.exit(fakeEvent)
                        }

                        override fun drop(dtde: DropTargetDropEvent) {
                            dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE)
                            val handled = value.drop(dtde.toDragEvent())
                            dtde.dropComplete(handled)
                        }
                    }, true)
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
    actual override fun parentRectangle(): Rect? {
        if (!native.isShowing) return null

        val location = native.location
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

            val t = theme.theme

            // Get padding - apply if theme.padding is true OR if paddingByEdge is explicitly set
            val paddingInsets = if (paddingByEdge != null) {
                val p = paddingByEdge!!
                java.awt.Insets(
                    p.top.px.toInt(),
                    p.left.px.toInt(),
                    p.bottom.px.toInt(),
                    p.right.px.toInt()
                )
            } else if (theme.padding) {
                // Clickable elements get padding from theme even without background
                val p = t.padding
                java.awt.Insets(
                    p.top.px.toInt(),
                    p.left.px.toInt(),
                    p.bottom.px.toInt(),
                    p.right.px.toInt()
                )
            } else {
                null
            }

            // Apply background if drawBackground is true
            if (theme.drawBackground) {
                val backgroundColor = t.background.closestColor()
                comp.background = backgroundColor.toAwt()
                comp.isOpaque = false // Set to false so border can paint rounded background

                // Calculate corner radius
                val cornerRadiusPx = when (val cr = t.cornerRadii) {
                    is CornerRadii.RatioOfSpacing -> (cr.value * t.gap.value).toInt()
                    is CornerRadii.Fixed -> cr.value.value.toInt()
                    is CornerRadii.AdaptiveToSpacing -> min(t.gap.value, cr.value.value).toInt()
                    is CornerRadii.RatioOfSize -> {
                        if (cr.ratio >= 0.5f) 9999
                        else (cr.ratio * min(comp.width, comp.height)).toInt()
                    }
                    is CornerRadii.PerCorner -> cr.value.value.toInt()
                }.coerceAtLeast(0)

                // Get border styling
                val outlineColor = t.outline.closestColor()
                val borderColor = outlineColor.toAwt()
                val borderThickness = t.outlineWidth.value.toInt().coerceAtLeast(0)

                // Apply rounded border with padding
                if (cornerRadiusPx > 0 || borderThickness > 0) {
                    comp.border = RoundedBorder(
                        borderColor, borderThickness, cornerRadiusPx, backgroundColor.toAwt(),
                        paddingInsets ?: java.awt.Insets(0, 0, 0, 0)
                    )
                } else if (paddingInsets != null) {
                    // Just padding, no rounded corners or border
                    comp.border = BorderFactory.createEmptyBorder(
                        paddingInsets.top, paddingInsets.left, paddingInsets.bottom, paddingInsets.right
                    )
                } else {
                    comp.border = null
                }
            } else {
                comp.isOpaque = false
                // Apply padding even without background (for clickable elements)
                if (paddingInsets != null) {
                    comp.border = BorderFactory.createEmptyBorder(
                        paddingInsets.top, paddingInsets.left, paddingInsets.bottom, paddingInsets.right
                    )
                } else {
                    comp.border = null
                }
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
