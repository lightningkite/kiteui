package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.CornerRadii
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.DownSemantic
import com.lightningkite.kiteui.models.HoverSemantic
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.toAwt
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.px
import com.lightningkite.kiteui.views.runOnUiThread
import kotlinx.coroutines.launch
import java.awt.Component
import java.awt.Container
import java.awt.Cursor
import java.awt.Desktop
import java.awt.Dimension
import java.awt.LayoutManager
import java.net.URI
import javax.swing.JButton
import javax.swing.JPanel

actual class ExternalLink actual constructor(context: RContext) : RView(context) {
    override val driverActions get() = super.driverActions + externalLinkDriverActions()
    // Content panel where children are placed - mouse transparent so clicks reach JButton
    private val contentPanel = object : JPanel() {
        init {
            layout = ExternalLinkContentLayout()
            isOpaque = false
        }
        // Make mouse transparent so clicks pass through to parent JButton
        override fun contains(x: Int, y: Int): Boolean = false
    }

    // The native component is a custom JButton that contains the content panel
    override val native = object : JButton() {
        init {
            isContentAreaFilled = false
            isBorderPainted = false
            isFocusPainted = true
            isOpaque = false
            isRolloverEnabled = true  // Enable rollover tracking for hover states
            // Remove all internal margins/borders so contentPanel gets full space
            margin = java.awt.Insets(0, 0, 0, 0)
            border = null
            layout = ExternalLinkButtonLayout()
            add(contentPanel)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        }

        override fun getPreferredSize(): Dimension {
            return contentPanel.preferredSize
        }
    }

    // Override child management to add to contentPanel instead of native
    override fun internalAddChild(index: Int, view: RView) {
        runOnUiThread {
            contentPanel.add(view.native, index)
            contentPanel.revalidate()
            contentPanel.repaint()
        }
    }

    override fun internalRemoveChild(index: Int) {
        runOnUiThread {
            if (index < contentPanel.componentCount) {
                contentPanel.remove(index)
                contentPanel.revalidate()
                contentPanel.repaint()
            }
        }
    }

    override fun internalClearChildren() {
        runOnUiThread {
            contentPanel.removeAll()
            contentPanel.revalidate()
            contentPanel.repaint()
        }
    }

    actual var enabled: Boolean = true
        set(value) {
            field = value
            native.isEnabled = value
            refreshTheming()
        }

    // TODO: Once ExternalLink is migrated to extend NativeContainerElementWithSecondaryAction,
    // this becomes an override of the base class nativeSetAction.
    fun nativeSetAction(action: Action?) {
        native.accessibleContext?.accessibleName = accessibleLabel ?: action?.title
    }

    actual var to: String? = null

    actual var newTab: Boolean = false

    private var onNavigate: suspend () -> Unit = {}
    actual fun onNavigate(action: suspend () -> Unit) {
        onNavigate = action
    }

    init {
        // Use ButtonModel's ChangeListener for hover/pressed state
        native.model.addChangeListener {
            refreshTheming()
        }

        // Handle click via ActionListener
        native.addActionListener {
            if (enabled) {
                launch {
                    onNavigate.invoke()
                    to?.let { url ->
                        try {
                            if (Desktop.isDesktopSupported()) {
                                Desktop.getDesktop().browse(URI(url))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    override fun applyTheme(theme: ThemeAndBack) {
        // Don't call super.applyTheme() - we handle everything ourselves for ExternalLink's custom architecture
        val t = theme.theme

        // Calculate corner radius
        val cornerRadiusPx = when (val cr = t.cornerRadii) {
            is CornerRadii.RatioOfSpacing -> (cr.value * t.gap.value).toInt()
            is CornerRadii.Fixed -> cr.value.value.toInt()
            is CornerRadii.AdaptiveToSpacing -> cr.value.value.toInt()
            is CornerRadii.RatioOfSize -> (cr.ratio * minOf(contentPanel.width, contentPanel.height)).toInt()
            is CornerRadii.PerCorner -> cr.value.value.toInt()
        }.coerceAtLeast(0)

        // Get padding from theme to prevent size changes between states
        val paddingInsets = if (paddingByEdge != null) {
            val p = paddingByEdge!!
            java.awt.Insets(p.top.px.toInt(), p.left.px.toInt(), p.bottom.px.toInt(), p.right.px.toInt())
        } else if (theme.padding) {
            val p = t.padding
            java.awt.Insets(p.top.px.toInt(), p.left.px.toInt(), p.bottom.px.toInt(), p.right.px.toInt())
        } else {
            java.awt.Insets(0, 0, 0, 0)
        }

        // Apply background to content panel for proper rendering (JButton's isContentAreaFilled is false)
        if (theme.drawBackground) {
            val backgroundColor = t.background.closestColor()
            contentPanel.background = backgroundColor.toAwt()
            contentPanel.isOpaque = false // Make transparent so we can paint rounded background

            val outlineColor = t.outline.closestColor()
            val borderColor = outlineColor.toAwt()
            val borderThickness = t.outlineWidth.value.toInt().coerceAtLeast(0)

            // Create custom border for rounded corners
            if (cornerRadiusPx > 0 || borderThickness > 0) {
                contentPanel.border = RoundedBorder(borderColor, borderThickness, cornerRadiusPx, backgroundColor.toAwt(), paddingInsets)
            } else if (paddingInsets.top > 0 || paddingInsets.left > 0 || paddingInsets.bottom > 0 || paddingInsets.right > 0) {
                contentPanel.border = javax.swing.BorderFactory.createEmptyBorder(
                    paddingInsets.top, paddingInsets.left, paddingInsets.bottom, paddingInsets.right
                )
            } else {
                contentPanel.border = null
            }
        } else {
            contentPanel.isOpaque = false
            // Apply padding even without background (for clickable state management)
            if (paddingInsets.top > 0 || paddingInsets.left > 0 || paddingInsets.bottom > 0 || paddingInsets.right > 0) {
                contentPanel.border = javax.swing.BorderFactory.createEmptyBorder(
                    paddingInsets.top, paddingInsets.left, paddingInsets.bottom, paddingInsets.right
                )
            } else {
                contentPanel.border = null
            }
        }

        // Apply foreground color
        val foregroundColor = t.foreground.closestColor()
        native.foreground = foregroundColor.toAwt()

        // Apply font styling
        val font = native.font
        val style = when {
            t.font.bold && t.font.italic -> java.awt.Font.BOLD or java.awt.Font.ITALIC
            t.font.bold -> java.awt.Font.BOLD
            t.font.italic -> java.awt.Font.ITALIC
            else -> java.awt.Font.PLAIN
        }
        native.font = font.deriveFont(style, t.font.size.value.toFloat())
    }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme[ClickableSemantic]
        val model = native.model
        if (!enabled) t = t[DisabledSemantic]
        else if (model.isPressed) t = t[DownSemantic]
        else if (model.isRollover) t = t[HoverSemantic]
        return super.applyState(t)
    }
}

/**
 * Layout manager for the JButton - makes content panel fill the button.
 */
private class ExternalLinkButtonLayout : LayoutManager {
    override fun addLayoutComponent(name: String?, comp: Component?) {}
    override fun removeLayoutComponent(comp: Component?) {}

    override fun preferredLayoutSize(parent: Container): Dimension {
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
        val availableWidth = parent.width - insets.left - insets.right
        val availableHeight = parent.height - insets.top - insets.bottom

        for (i in 0 until parent.componentCount) {
            val child = parent.getComponent(i)
            if (child.isVisible) {
                layoutChildWithAlignment(child, insets, availableWidth, availableHeight)
            }
        }
    }
}

/**
 * Layout manager for the content panel - stacks children.
 */
private class ExternalLinkContentLayout : LayoutManager {
    override fun addLayoutComponent(name: String?, comp: Component?) {}
    override fun removeLayoutComponent(comp: Component?) {}

    override fun preferredLayoutSize(parent: Container): Dimension {
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
        return Dimension(
            maxWidth + insets.left + insets.right,
            maxHeight + insets.top + insets.bottom
        )
    }

    override fun minimumLayoutSize(parent: Container): Dimension {
        var maxWidth = 0
        var maxHeight = 0
        for (i in 0 until parent.componentCount) {
            val child = parent.getComponent(i)
            if (child.isVisible) {
                val childSize = child.minimumSize
                maxWidth = maxOf(maxWidth, childSize.width)
                maxHeight = maxOf(maxHeight, childSize.height)
            }
        }
        val insets = parent.insets
        return Dimension(
            maxWidth + insets.left + insets.right,
            maxHeight + insets.top + insets.bottom
        )
    }

    override fun layoutContainer(parent: Container) {
        val insets = parent.insets
        val availableWidth = parent.width - insets.left - insets.right
        val availableHeight = parent.height - insets.top - insets.bottom

        for (i in 0 until parent.componentCount) {
            val child = parent.getComponent(i)
            if (child.isVisible) {
                layoutChildWithAlignment(child, insets, availableWidth, availableHeight)
            }
        }
    }
}
