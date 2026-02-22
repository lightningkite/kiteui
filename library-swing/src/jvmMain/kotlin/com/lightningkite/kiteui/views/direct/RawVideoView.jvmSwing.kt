package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.models.toAwt
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.core.*
import java.awt.*
import javax.swing.*
import kotlin.time.Duration

/*
 * JVM Swing Video Player Implementation Notes:
 *
 * Swing does not have native video playback support. To implement full video playback,
 * you would need to integrate one of the following third-party libraries:
 *
 * Option 1: JavaFX MediaView (recommended for modern JVM)
 *   - Add dependency: org.openjfx:javafx-media
 *   - Embed JavaFX MediaPlayer in Swing via JFXPanel
 *   - Supports common video formats (MP4, WebM, etc.)
 *   - Example:
 *     val jfxPanel = JFXPanel()
 *     Platform.runLater {
 *       val mediaPlayer = MediaPlayer(Media(url))
 *       val mediaView = MediaView(mediaPlayer)
 *       val scene = Scene(StackPane(mediaView))
 *       jfxPanel.scene = scene
 *     }
 *
 * Option 2: VLC-J (VLCJ)
 *   - Add dependency: uk.co.caprica:vlcj
 *   - Requires VLC media player installed on the system
 *   - Professional-grade video playback with extensive format support
 *   - Can embed VLC player in Swing canvas
 *
 * Option 3: JMF (Java Media Framework) - Legacy, not recommended
 *   - Deprecated and no longer maintained
 *   - Limited format support
 *
 * Current Implementation:
 * This is a stub implementation that provides the reactive interface but displays
 * a placeholder panel instead of actual video playback. All reactive properties are
 * functional, allowing the rest of the UI to work correctly.
 */

actual class RawVideoView actual constructor(
    context: RContext,
    source: VideoSource,
    description: String,
    scaleType: ImageScaleType,
    preloadHint: PreloadHint,
) : RView(context) {
    private val videoPanel = VideoPlaceholderPanel(source, description, scaleType)
    override val native: JPanel = videoPanel

    actual val source: VideoSource = source
    actual val description: String = description
    actual val scaleType: ImageScaleType = scaleType
    actual val preloadHint: PreloadHint = preloadHint

    // Reactive state - ready immediately for stub implementation
    actual val state: Reactive<Unit> = Constant(Unit)
    actual val seekableTimeRanges: List<ClosedFloatingPointRange<Double>> = emptyList()

    @Deprecated("Use currentTime instead")
    actual val time: MutableReactive<Double> = Signal(0.0)
    actual val currentTime: MutableReactive<Duration> = Signal(Duration.ZERO)

    actual val sourceDuration: Reactive<Double?> = Constant(null)
    actual val playing: MutableReactive<Boolean> = Signal(false)
    actual val volume: MutableReactive<Float> = Signal(1.0f)
    actual var showControls: Boolean = true
    actual var loop: Boolean = false

    // Stub listenable - never fires since no video actually plays
    actual val completedPlay: Listenable = object : Listenable {
        override fun addListener(action: () -> Unit): () -> Unit = {}
    }

    init {
        native.toolTipText = description.ifEmpty { "Video playback not implemented (JVM Swing)" }
    }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        videoPanel.updateTheme(theme)
    }
}

/**
 * Placeholder panel that displays information about the video source
 * and explains that video playback requires additional dependencies.
 */
internal class VideoPlaceholderPanel(
    private val source: VideoSource,
    private val description: String,
    private val scaleType: ImageScaleType
) : JPanel() {
    private var currentTheme: ThemeAndBack? = null

    init {
        layout = GridBagLayout()
        preferredSize = java.awt.Dimension(320, 240)
        minimumSize = java.awt.Dimension(160, 120)
    }

    fun updateTheme(theme: ThemeAndBack) {
        currentTheme = theme
        val bgColor: com.lightningkite.kiteui.models.Color = theme.theme.background.closestColor()
        background = bgColor.toAwt()
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        val theme = currentTheme
        val fgColor: com.lightningkite.kiteui.models.Color? = theme?.theme?.foreground?.closestColor()
        val foregroundColor: java.awt.Color = fgColor?.toAwt() ?: java.awt.Color.DARK_GRAY
        val mutedColor = java.awt.Color(
            foregroundColor.red,
            foregroundColor.green,
            foregroundColor.blue,
            (foregroundColor.alpha * 0.6).toInt().coerceIn(0, 255)
        )

        // Draw video icon
        val iconSize = 48
        val iconX = (width - iconSize) / 2
        val iconY = (height - iconSize) / 2 - 40

        g2d.color = mutedColor
        g2d.fillRoundRect(iconX, iconY, iconSize, iconSize, 8, 8)

        // Draw play triangle
        g2d.color = background
        val triangleSize = 20
        val triangleX = iconX + (iconSize - triangleSize) / 2 + 3
        val triangleY = iconY + (iconSize - triangleSize) / 2
        val xPoints = intArrayOf(triangleX, triangleX + triangleSize, triangleX)
        val yPoints = intArrayOf(triangleY, triangleY + triangleSize / 2, triangleY + triangleSize)
        g2d.fillPolygon(xPoints, yPoints, 3)

        // Draw text
        g2d.color = foregroundColor
        g2d.font = java.awt.Font("SansSerif", java.awt.Font.BOLD, 14)
        val message = "Video Playback Not Available"
        val fm = g2d.fontMetrics
        val messageWidth = fm.stringWidth(message)
        g2d.drawString(message, (width - messageWidth) / 2, iconY + iconSize + 30)

        // Draw smaller info text
        g2d.font = java.awt.Font("SansSerif", java.awt.Font.PLAIN, 11)
        g2d.color = mutedColor
        val info = "Requires JavaFX or VLCJ library"
        val infoWidth = g2d.fontMetrics.stringWidth(info)
        g2d.drawString(info, (width - infoWidth) / 2, iconY + iconSize + 50)

        // Draw source info
        val sourceText = when (source) {
            is VideoRemote -> "URL: ${source.url.take(40)}${if (source.url.length > 40) "..." else ""}"
            is VideoLocal -> "File: ${source.file.file.name}"
            is VideoRaw -> "Raw data: ${source.data.data.size} bytes"
            is VideoResource -> "Resource: ${(source as? VideoResource)}"
            else -> "Source: $source"
        }
        g2d.font = java.awt.Font("Monospaced", java.awt.Font.PLAIN, 10)
        val sourceWidth = g2d.fontMetrics.stringWidth(sourceText)
        g2d.drawString(sourceText, (width - sourceWidth) / 2, iconY + iconSize + 70)
    }
}
