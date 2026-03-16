package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import java.awt.Color as AwtColor
import java.awt.Font as AwtFont
import java.awt.Component
import javax.swing.*

actual class Select actual constructor(context: RContext) : RView(context) {
    private var _driverSelectedDisplay: String? = null
    private var _driverSelectSetValue: (suspend (String) -> Unit)? = null
    override val driverValue: String? get() = _driverSelectedDisplay
    override val driverActions get() = super.driverActions + buildMap {
        _driverSelectSetValue?.let { setter ->
            put("setValue") { args: List<String> -> setter(args.joinToString(" ")); "OK" }
        }
    }
    override val native = JComboBox<Any>().apply {
        // Ensure dropdown has reasonable minimum size
        minimumSize = java.awt.Dimension(80, 24)
    }

    actual var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }

    actual fun <T> bind(edits: MutableReactive<T>, data: Reactive<List<T>>, render: (T) -> String) {
        var suppressChange = false
        var list: List<T> = listOf()

        val model = DefaultComboBoxModel<Any>()
        native.model = model

        // Custom renderer to display items using the render function
        native.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value != null && component is JLabel) {
                    @Suppress("UNCHECKED_CAST")
                    component.text = render(value as T)
                }
                return component
            }
        }

        // Listen for selection changes from user interaction
        native.addActionListener {
            if (!suppressChange && native.selectedItem != null) {
                val selectedIndex = native.selectedIndex
                if (selectedIndex >= 0 && selectedIndex < list.size) {
                    val selectedItem = list[selectedIndex]
                    val setAction = Action("Set Value", com.lightningkite.kiteui.models.Icon.send, frequencyCap = null, ignoreRetryWhileRunning = false) {
                        edits set selectedItem
                    }
                    setAction.startAction(this@Select)
                }
            }
        }

        // React to data changes
        reactiveScope {
            list = data()
            suppressChange = true
            model.removeAllElements()
            list.forEach { item ->
                model.addElement(item as Any)
            }

            // Set initial selection
            val currentlySelected = edits.once()
            val index = list.indexOf(currentlySelected)
            if (index != -1) {
                native.selectedIndex = index
            }
            suppressChange = false
        }

        // React to edits (selected value) changes
        reactiveScope {
            val currentlySelected = edits()
            val index = list.indexOf(currentlySelected)
            if (index != -1 && !suppressChange) {
                suppressChange = true
                native.selectedIndex = index
                suppressChange = false
            }
        }

        // Driver support: track selected display and allow setValue
        reactiveScope {
            _driverSelectedDisplay = render(edits())
        }
        _driverSelectSetValue = { displayText ->
            val item = list.firstOrNull { render(it) == displayText }
                ?: throw com.lightningkite.kiteui.views.DriverActionException("No option matching '$displayText'")
            edits.set(item)
        }
    }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme[ClickableSemantic]
        if (!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        val t = theme.theme

        // Set foreground color for text
        val foregroundColor = t.foreground.closestColor()
        native.foreground = AwtColor(
            (foregroundColor.red * 255).toInt().coerceIn(0, 255),
            (foregroundColor.green * 255).toInt().coerceIn(0, 255),
            (foregroundColor.blue * 255).toInt().coerceIn(0, 255),
            (foregroundColor.alpha * 255).toInt().coerceIn(0, 255)
        )

        // Set background color
        val backgroundColor = t.background.closestColor()
        native.background = AwtColor(
            (backgroundColor.red * 255).toInt().coerceIn(0, 255),
            (backgroundColor.green * 255).toInt().coerceIn(0, 255),
            (backgroundColor.blue * 255).toInt().coerceIn(0, 255),
            (backgroundColor.alpha * 255).toInt().coerceIn(0, 255)
        )

        // Apply font
        val fontStyle = t.font
        var awtFontStyle = AwtFont.PLAIN
        if (fontStyle.italic) {
            awtFontStyle = awtFontStyle or AwtFont.ITALIC
        }
        if (fontStyle.weight >= 700) {
            awtFontStyle = awtFontStyle or AwtFont.BOLD
        }

        val fontSize = fontStyle.size.value.toFloat()
        native.font = fontStyle.font.deriveFont(awtFontStyle, fontSize)

        // Make background visible
        native.isOpaque = theme.drawBackground
    }
}
