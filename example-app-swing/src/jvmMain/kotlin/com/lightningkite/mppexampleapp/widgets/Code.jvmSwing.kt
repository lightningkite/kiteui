package com.lightningkite.mppexampleapp.widgets

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.WordBreak
import com.lightningkite.kiteui.models.toAwt
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import javax.swing.JTextArea
import javax.swing.JScrollPane
import java.awt.Font as AwtFont

actual class Code actual constructor(context: RContext) : RView(context) {
    private val textArea = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        font = AwtFont("Monospaced", AwtFont.PLAIN, 12)
    }
    private val scrollPane = JScrollPane(textArea)
    override val native: java.awt.Component = scrollPane

    actual var content: String
        get() = textArea.text
        set(value) {
            textArea.text = value
        }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        textArea.foreground = theme.theme.foreground.closestColor().toAwt()
        textArea.background = theme.theme.background.closestColor().toAwt()
    }
}
