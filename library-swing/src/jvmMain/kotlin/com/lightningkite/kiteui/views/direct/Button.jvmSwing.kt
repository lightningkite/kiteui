package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.toAwt
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RViewWithSecondaryAction
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.LayoutManager
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

actual class Button actual constructor(context: RContext) : RViewWithSecondaryAction(context) {
    private val button = JButton()
    private val progress = JProgressBar().apply {
        isIndeterminate = true
        preferredSize = Dimension(16, 16)
        minimumSize = Dimension(16, 16)
        maximumSize = Dimension(16, 16)
        isVisible = false
    }

    override val native = JPanel().apply {
        layout = ButtonLayoutManager()
        add(button)
        add(progress)
        isOpaque = false
    }

    actual var enabled: Boolean
        get() = button.isEnabled
        set(value) {
            button.isEnabled = value
            refreshTheming()
        }

    override fun postSetup() {
        super.postSetup()
        // Listen to working state to show/hide progress
        working.addListener {
            progress.isVisible = working.value
            native.revalidate()
            native.repaint()
        }
    }

    init {
        // Primary action on click
        button.addActionListener {
            if (enabled) {
                action?.startAction(this)
            }
        }

        // Secondary action on right-click or long-press
        var pressTime: Long = 0
        val longPressThreshold = 500L // 500ms for long press
        var longPressTimer: Timer? = null

        button.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (e.button == MouseEvent.BUTTON3) {
                    // Right-click
                    if (enabled) {
                        secondaryAction?.startAction(this@Button)
                    }
                } else if (e.button == MouseEvent.BUTTON1) {
                    // Left-click: start long-press timer
                    pressTime = System.currentTimeMillis()
                    longPressTimer?.stop()
                    longPressTimer = Timer(longPressThreshold.toInt()) {
                        if (enabled && secondaryAction != null) {
                            secondaryAction?.startAction(this@Button)
                        }
                    }.apply {
                        isRepeats = false
                        start()
                    }
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                // Cancel long-press if released before threshold
                longPressTimer?.stop()
                longPressTimer = null
            }
        })
    }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        val t = theme.theme

        // Keep the wrapper transparent - we apply backgrounds to the button itself
        native.isOpaque = false

        // Apply foreground color
        val foregroundColor = t.foreground.closestColor()
        button.foreground = foregroundColor.toAwt()

        // Apply background color
        val backgroundColor = t.background.closestColor()
        button.background = backgroundColor.toAwt()

        // Apply progress bar color
        progress.foreground = foregroundColor.toAwt()

        // Apply font styling
        val font = button.font
        val style = when {
            t.font.bold && t.font.italic -> java.awt.Font.BOLD or java.awt.Font.ITALIC
            t.font.bold -> java.awt.Font.BOLD
            t.font.italic -> java.awt.Font.ITALIC
            else -> java.awt.Font.PLAIN
        }
        button.font = font.deriveFont(style, t.font.size.value.toFloat())

        // Apply border for outline if needed
        if (theme.drawBackground) {
            val outlineColor = t.outline.closestColor()
            val borderColor = outlineColor.toAwt()
            val borderThickness = t.outlineWidth.value.toInt().coerceAtLeast(0)
            if (borderThickness > 0) {
                button.border = BorderFactory.createLineBorder(borderColor, borderThickness)
            } else {
                button.border = null
            }
        } else {
            button.border = null
        }

        button.isOpaque = theme.drawBackground
    }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme[ClickableSemantic]
        if (!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }
}

/**
 * Custom layout manager for Button that overlays the progress indicator
 * on top of the button, similar to Android's FrameLayout approach.
 */
private class ButtonLayoutManager : LayoutManager {
    override fun addLayoutComponent(name: String?, comp: Component?) {}
    override fun removeLayoutComponent(comp: Component?) {}

    override fun preferredLayoutSize(parent: Container): Dimension {
        // Use the button's preferred size (first component)
        if (parent.componentCount > 0) {
            return parent.getComponent(0).preferredSize
        }
        return Dimension(0, 0)
    }

    override fun minimumLayoutSize(parent: Container): Dimension {
        if (parent.componentCount > 0) {
            return parent.getComponent(0).minimumSize
        }
        return Dimension(0, 0)
    }

    override fun layoutContainer(parent: Container) {
        val insets = parent.insets
        val width = parent.width - insets.left - insets.right
        val height = parent.height - insets.top - insets.bottom

        if (parent.componentCount > 0) {
            // Layout button to fill
            val button = parent.getComponent(0)
            button.setBounds(insets.left, insets.top, width, height)

            // Layout progress indicator centered on top
            if (parent.componentCount > 1) {
                val progress = parent.getComponent(1)
                if (progress.isVisible) {
                    val progressSize = progress.preferredSize
                    val x = insets.left + (width - progressSize.width) / 2
                    val y = insets.top + (height - progressSize.height) / 2
                    progress.setBounds(x, y, progressSize.width, progressSize.height)
                }
            }
        }
    }
}
