package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.canvas.DrawingContext2DImpl
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.*
import javax.swing.JComponent

actual class Canvas actual constructor(context: RContext) : RView(context) {
    override val native = CanvasComponent()

    actual var delegate: CanvasDelegate?
        get() = native.delegate
        set(value) {
            native.delegate = value
            value?.theme = themeAndBack.theme
            value?.invalidate = { native.repaint() }
            value?.onResize(native.width.toDouble(), native.height.toDouble())
        }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        delegate?.theme = theme.theme
        delegate?.invalidate?.invoke()
    }

    inner class CanvasComponent : JComponent() {
        var delegate: CanvasDelegate? = null
            set(value) {
                field?.invalidate = {}
                field = value
                field?.invalidate = { repaint() }
                value?.onResize(width.toDouble(), height.toDouble())
                repaint()
            }

        init {
            isFocusable = true

            // Add mouse listeners for pointer events
            addMouseListener(object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    requestFocusInWindow()
                    delegate?.onPointerDown(
                        e.button,
                        e.x.toDouble(),
                        e.y.toDouble(),
                        width.toDouble(),
                        height.toDouble()
                    )
                }

                override fun mouseReleased(e: MouseEvent) {
                    delegate?.onPointerUp(
                        e.button,
                        e.x.toDouble(),
                        e.y.toDouble(),
                        width.toDouble(),
                        height.toDouble()
                    )
                }
            })

            addMouseMotionListener(object : MouseMotionAdapter() {
                override fun mouseMoved(e: MouseEvent) {
                    delegate?.onPointerMove(
                        -1, // No button for move
                        e.x.toDouble(),
                        e.y.toDouble(),
                        width.toDouble(),
                        height.toDouble()
                    )
                }

                override fun mouseDragged(e: MouseEvent) {
                    delegate?.onPointerMove(
                        e.button,
                        e.x.toDouble(),
                        e.y.toDouble(),
                        width.toDouble(),
                        height.toDouble()
                    )
                }
            })

            // Add mouse wheel listener
            addMouseWheelListener { e ->
                delegate?.onWheel(
                    0.0, // No horizontal scroll in basic MouseWheelEvent
                    e.preciseWheelRotation,
                    e.preciseWheelRotation
                )
            }

            // Add keyboard listeners
            addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    delegate?.onKeyDown(e.keyCode)
                }

                override fun keyReleased(e: KeyEvent) {
                    delegate?.onKeyUp(e.keyCode)
                }
            })

            // Add component listener for resize events
            addComponentListener(object : ComponentAdapter() {
                override fun componentResized(e: ComponentEvent) {
                    delegate?.onResize(width.toDouble(), height.toDouble())
                    repaint()
                }
            })
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2d = g as? Graphics2D ?: return

            // Enable anti-aliasing for better rendering
            g2d.setRenderingHint(
                java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON
            )
            g2d.setRenderingHint(
                java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            )

            val context = DrawingContext2DImpl(g2d)
            delegate?.draw(context)
        }
    }
}
