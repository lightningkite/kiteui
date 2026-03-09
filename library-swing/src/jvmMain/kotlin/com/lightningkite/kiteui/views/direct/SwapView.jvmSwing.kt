package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.models.toAwt
import com.lightningkite.kiteui.views.MouseTransparentPanel
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.reactive.context.CalculationContext
import java.awt.AlphaComposite
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.LayoutManager
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.Timer

actual class SwapView actual constructor(context: RContext) : RView(context) {
    // by Claude - only expose the current (last) child for AI driver snapshots and path resolution
    override val activeChildren: List<RView> get() = listOfNotNull(children.lastOrNull())
    // Track the current content size to prevent layout jumping during animations
    private var contentPreferredSize: Dimension = Dimension(0, 0)

    override val native = MouseTransparentPanel().apply {
        layout = SwapLayoutManager { contentPreferredSize }
    }

    private var animationTimer: Timer? = null
    private var setupTimer: Timer? = null
    private var currentAnimationPanel: AnimationPanel? = null

    actual fun swap(transition: ScreenTransition, createNewView: ViewWriter.() -> Unit?) {
        val animationDurationMs = theme.transitionDuration.inWholeMilliseconds
        // Cancel any ongoing animation and setup
        animationTimer?.stop()
        animationTimer = null
        setupTimer?.stop()
        setupTimer = null
        // Remove any existing animation panel
        currentAnimationPanel?.let { native.remove(it) }
        currentAnimationPanel = null

        // Clean up views from any interrupted swap:
        // Make all existing views visible and remove any extra views beyond the first
        // (there should only be one view after a complete swap)
        val existingChildren = this.children.toList()
        if (existingChildren.isNotEmpty()) {
            // Keep only the first child, remove any others (from interrupted swaps)
            existingChildren.forEachIndexed { index, child ->
                if (index == 0) {
                    child.native.isVisible = true
                } else {
                    removeChild(child)
                }
            }
        }

        val oldView = this.children.firstOrNull()
        var newViewHolder: RView? = null


        // Create a ViewWriter to capture the new view
        val writer = object : ViewWriter(), CalculationContext by this {
            override val representsView: RView? = this@SwapView
            override val context: RContext
                get() = this@SwapView.context

            override fun willAddChild(view: RView) {
                view.parent = this@SwapView
            }

            override fun addChild(view: RView) {
                newViewHolder = view
            }
        }

        // Create the new view
        writer.createNewView()
        val newView = newViewHolder

        // If no transition, no old view (first swap), or no new view (clearing), instant swap
        if (transition == ScreenTransition.None || oldView == null || newView == null) {
            oldView?.let { removeChild(it) }
            newView?.let { addChild(it) }
            // Update content size for layout
            contentPreferredSize = newView?.native?.preferredSize ?: Dimension(0, 0)
            // Schedule layout validation for after current event processing
            // This ensures parent layouts have been established first
            javax.swing.SwingUtilities.invokeLater {
                // Invalidate and revalidate up the hierarchy
                var c: java.awt.Container? = native
                while (c != null) {
                    c.invalidate()
                    c = c.parent
                }
                // Find root and validate
                var root: java.awt.Container? = native
                while (root?.parent != null) {
                    root = root.parent
                }
                root?.validate()
                native.repaint()
            }
            return
        }

        // Use the current container size as the animation target size
        val targetWidth = if (native.width > 0) native.width else oldView?.native?.width ?: 100
        val targetHeight = if (native.height > 0) native.height else oldView?.native?.height ?: 100
        contentPreferredSize = Dimension(targetWidth, targetHeight)

        // Determine animation type
        val animationType = when (transition) {
            ScreenTransition.Push -> AnimationType.SLIDE_LEFT
            ScreenTransition.Pop -> AnimationType.SLIDE_RIGHT
            ScreenTransition.PullUp -> AnimationType.SLIDE_UP
            ScreenTransition.PullDown -> AnimationType.SLIDE_DOWN
            ScreenTransition.Fade -> AnimationType.FADE
            ScreenTransition.GrowFade -> AnimationType.GROW_FADE
            ScreenTransition.ShrinkFade -> AnimationType.SHRINK_FADE
            else -> AnimationType.FADE
        }

        // Get background color from the theme
        val backgroundColor = themeAndBack.theme.background.closestColor().toAwt()

        // Capture old view to BufferedImage BEFORE adding new view
        val oldImage: BufferedImage? = captureComponent(oldView?.native as? JComponent, backgroundColor)

        // Hide old view immediately after capturing
        oldView?.native?.isVisible = false

        // Add new view (hidden initially so it doesn't flash) and let it layout
        newView?.native?.isVisible = false
        newView?.let { addChild(it) }

        // Force layout of the entire hierarchy including the new view
        var root: java.awt.Container? = native
        while (root?.parent != null) {
            root = root.parent
        }
        root?.validate()

        // Size the new view to match the target
        newView?.native?.let { comp ->
            comp.setBounds(0, 0, targetWidth, targetHeight)
            validateRecursively(comp)
        }

        // Now add animation panel showing old image (covers everything during wait period)
        val animationPanel = AnimationPanel(
            oldImage = oldImage,
            newImage = null,
            animationType = animationType,
            targetSize = contentPreferredSize,
            backgroundColor = backgroundColor
        )
        animationPanel.progress = 0f
        native.add(animationPanel, 0)
        native.revalidate()
        currentAnimationPanel = animationPanel

        // Make new view visible (behind animation panel) so it can paint
        newView?.native?.isVisible = true

        // Wait for the new view to fully render before starting animation
        setupTimer = Timer(50) {
            setupTimer?.stop()
            setupTimer = null

            // Force a paint of the new view
            (newView?.native as? JComponent)?.paintImmediately(0, 0, targetWidth, targetHeight)

            // Capture the new view and update animation panel
            animationPanel.newImage = captureComponent(newView?.native as? JComponent, backgroundColor)

            // Hide new view during animation (animation panel shows both images)
            newView?.native?.isVisible = false

            val startTime = System.currentTimeMillis()

            animationTimer = Timer(16) { _ ->
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toDouble() / animationDurationMs).coerceIn(0.0, 1.0)

                // Ease out cubic
                val eased = 1 - Math.pow(1 - progress, 3.0)

                animationPanel.progress = eased.toFloat()
                animationPanel.repaint()

                if (progress >= 1.0) {
                    animationTimer?.stop()
                    animationTimer = null

                    // Clean up animation
                    native.remove(animationPanel)
                    currentAnimationPanel = null
                    oldView?.let { removeChild(it) }
                    // Make new view visible now that animation is complete
                    newView?.native?.isVisible = true
                    contentPreferredSize = newView?.native?.preferredSize ?: Dimension(0, 0)
                    native.revalidate()
                    native.repaint()
                    // Force parent to re-layout since our preferred size may have changed
                    native.parent?.revalidate()
                    native.parent?.repaint()
                }
            }.also { animationTimer = it }.apply { start() }
        }.apply { start() }
    }

    private fun captureComponent(comp: JComponent?, backgroundColor: java.awt.Color): BufferedImage? {
        if (comp == null || comp.width <= 0 || comp.height <= 0) return null
        val img = BufferedImage(comp.width, comp.height, BufferedImage.TYPE_INT_ARGB)
        val g2 = img.createGraphics()
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2.color = backgroundColor
        g2.fillRect(0, 0, comp.width, comp.height)
        comp.paint(g2)
        g2.dispose()
        return img
    }

    private fun validateRecursively(comp: Component) {
        if (comp is Container) {
            comp.doLayout()
            for (i in 0 until comp.componentCount) {
                validateRecursively(comp.getComponent(i))
            }
        }
    }
}

private enum class AnimationType {
    FADE,
    SLIDE_LEFT,
    SLIDE_RIGHT,
    SLIDE_UP,
    SLIDE_DOWN,
    GROW_FADE,
    SHRINK_FADE
}

/**
 * A panel that renders the animation between two views.
 * Uses pre-captured BufferedImages of both views to ensure correct rendering.
 */
private class AnimationPanel(
    private val oldImage: BufferedImage?,
    newImage: BufferedImage?,
    private val animationType: AnimationType,
    private val targetSize: Dimension,
    private val backgroundColor: java.awt.Color
) : JPanel() {
    var newImage: BufferedImage? = newImage
    var progress: Float = 0f

    init {
        isOpaque = true  // Make opaque to cover content beneath
        background = backgroundColor
    }

    override fun getPreferredSize(): Dimension = targetSize
    override fun getMinimumSize(): Dimension = targetSize

    // Use targetSize dimensions for painting to handle cases where layout hasn't settled
    private val paintWidth: Int get() = if (width > 0) width else targetSize.width
    private val paintHeight: Int get() = if (height > 0) height else targetSize.height

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)

        try {
            when (animationType) {
                AnimationType.FADE -> paintFade(g2)
                AnimationType.SLIDE_LEFT -> paintSlideLeft(g2)
                AnimationType.SLIDE_RIGHT -> paintSlideRight(g2)
                AnimationType.SLIDE_UP -> paintSlideUp(g2)
                AnimationType.SLIDE_DOWN -> paintSlideDown(g2)
                AnimationType.GROW_FADE -> paintGrowFade(g2)
                AnimationType.SHRINK_FADE -> paintShrinkFade(g2)
            }
        } finally {
            g2.dispose()
        }
    }

    private fun paintFade(g2: Graphics2D) {
        // Paint new image at full opacity first (underneath)
        newImage?.let { img ->
            g2.drawImage(img, 0, 0, null)
        }
        // Paint old image fading out on top
        oldImage?.let { img ->
            val oldAlpha = (1f - progress).coerceIn(0f, 1f)
            g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, oldAlpha)
            g2.drawImage(img, 0, 0, null)
        }
    }

    private fun paintSlideLeft(g2: Graphics2D) {
        val offset = (paintWidth * progress).toInt()

        // Paint new image sliding in from the right
        newImage?.let { img ->
            val transform = g2.transform
            g2.translate(paintWidth - offset, 0)
            g2.drawImage(img, 0, 0, null)
            g2.transform = transform
        }

        // Paint old image sliding out to the left
        oldImage?.let { img ->
            val transform = g2.transform
            g2.translate(-offset, 0)
            g2.drawImage(img, 0, 0, null)
            g2.transform = transform
        }
    }

    private fun paintSlideRight(g2: Graphics2D) {
        val offset = (paintWidth * progress).toInt()

        // Paint new image sliding in from the left
        newImage?.let { img ->
            val transform = g2.transform
            g2.translate(-paintWidth + offset, 0)
            g2.drawImage(img, 0, 0, null)
            g2.transform = transform
        }

        // Paint old image sliding out to the right
        oldImage?.let { img ->
            val transform = g2.transform
            g2.translate(offset, 0)
            g2.drawImage(img, 0, 0, null)
            g2.transform = transform
        }
    }

    private fun paintSlideUp(g2: Graphics2D) {
        val offset = (paintHeight * progress).toInt()

        // Paint new image sliding in from the bottom
        newImage?.let { img ->
            val transform = g2.transform
            g2.translate(0, paintHeight - offset)
            g2.drawImage(img, 0, 0, null)
            g2.transform = transform
        }

        // Paint old image sliding out to the top
        oldImage?.let { img ->
            val transform = g2.transform
            g2.translate(0, -offset)
            g2.drawImage(img, 0, 0, null)
            g2.transform = transform
        }
    }

    private fun paintSlideDown(g2: Graphics2D) {
        val offset = (paintHeight * progress).toInt()

        // Paint new image sliding in from the top
        newImage?.let { img ->
            val transform = g2.transform
            g2.translate(0, -paintHeight + offset)
            g2.drawImage(img, 0, 0, null)
            g2.transform = transform
        }

        // Paint old image sliding out to the bottom
        oldImage?.let { img ->
            val transform = g2.transform
            g2.translate(0, offset)
            g2.drawImage(img, 0, 0, null)
            g2.transform = transform
        }
    }

    private fun paintGrowFade(g2: Graphics2D) {
        // Paint new image growing in (scale from 0.9 to 1.0) with fade
        newImage?.let { img ->
            val newAlpha = progress.coerceIn(0f, 1f)
            val scale = 0.9 + (0.1 * progress)  // Scale from 0.9 to 1.0

            g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, newAlpha)

            val centerX = paintWidth / 2.0
            val centerY = paintHeight / 2.0

            val transform = g2.transform
            g2.translate(centerX, centerY)
            g2.scale(scale, scale)
            g2.translate(-centerX, -centerY)

            g2.drawImage(img, 0, 0, null)
            g2.transform = transform
        }

        // Paint old image fading out on top
        g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f)
        oldImage?.let { img ->
            val oldAlpha = (1f - progress).coerceIn(0f, 1f)
            g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, oldAlpha)
            g2.drawImage(img, 0, 0, null)
        }
    }

    private fun paintShrinkFade(g2: Graphics2D) {
        // Paint new image at full opacity first (underneath)
        newImage?.let { img ->
            g2.drawImage(img, 0, 0, null)
        }

        // Paint old image fading out while shrinking on top
        oldImage?.let { img ->
            val oldAlpha = (1f - progress).coerceIn(0f, 1f)
            val scale = 1.0 - (0.1 * progress)  // Scale from 1.0 to 0.9

            g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, oldAlpha)

            val centerX = paintWidth / 2.0
            val centerY = paintHeight / 2.0

            val transform = g2.transform
            g2.translate(centerX, centerY)
            g2.scale(scale, scale)
            g2.translate(-centerX, -centerY)

            g2.drawImage(img, 0, 0, null)
            g2.transform = transform
        }
    }
}

/**
 * Layout manager for SwapView that makes each child fill the entire container.
 * Uses a tracked content size to prevent layout jumping during animations.
 */
private class SwapLayoutManager(
    private val getContentSize: () -> Dimension
) : LayoutManager {
    override fun addLayoutComponent(name: String?, comp: Component?) {
        // No-op
    }

    override fun removeLayoutComponent(comp: Component?) {
        // No-op
    }

    override fun preferredLayoutSize(parent: Container): Dimension {
        // Use tracked content size if available (during animations)
        val trackedSize = getContentSize()
        if (trackedSize.width > 0 || trackedSize.height > 0) {
            val insets = parent.insets
            return Dimension(
                trackedSize.width + insets.left + insets.right,
                trackedSize.height + insets.top + insets.bottom
            )
        }

        // Fallback: return the maximum preferred size among ALL children (visible or not)
        var maxWidth = 0
        var maxHeight = 0

        for (i in 0 until parent.componentCount) {
            val child = parent.getComponent(i)
            // Consider all children, not just visible ones, to prevent size changes during animation
            val childSize = child.preferredSize
            maxWidth = maxOf(maxWidth, childSize.width)
            maxHeight = maxOf(maxHeight, childSize.height)
        }

        val insets = parent.insets
        return Dimension(
            maxWidth + insets.left + insets.right,
            maxHeight + insets.top + insets.bottom
        )
    }

    override fun minimumLayoutSize(parent: Container): Dimension {
        // Use tracked content size if available
        val trackedSize = getContentSize()
        if (trackedSize.width > 0 || trackedSize.height > 0) {
            val insets = parent.insets
            return Dimension(
                trackedSize.width + insets.left + insets.right,
                trackedSize.height + insets.top + insets.bottom
            )
        }

        // Fallback: return the maximum minimum size among ALL children
        var maxWidth = 0
        var maxHeight = 0

        for (i in 0 until parent.componentCount) {
            val child = parent.getComponent(i)
            val childSize = child.minimumSize
            maxWidth = maxOf(maxWidth, childSize.width)
            maxHeight = maxOf(maxHeight, childSize.height)
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

        // Lay out all children to fill the entire space (visible or not)
        for (i in 0 until parent.componentCount) {
            val child = parent.getComponent(i)
            child.setBounds(insets.left, insets.top, availableWidth, availableHeight)
        }
    }
}
