package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.lightningkite.kiteui.models.toDp
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.AiDriver
import com.lightningkite.kiteui.views.DriverActionException
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeInteractiveElement
import com.lightningkite.kiteui.views.toCompose
import com.lightningkite.kiteui.views.toTextStyle
import com.lightningkite.kiteui.views.cased
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.MutableReactive
import com.lightningkite.reactive.core.Reactive

public actual class Select actual constructor(context: ElementContext) : NativeInteractiveElement(context) {
    override val driverValue: String? get() = selected
    override val driverActions: AiDriver.Actions get() = super.driverActions + buildMap {
        val choose = choose ?: return@buildMap
        put("setValue") { args ->
            val index = options.indexOf(args.joinToString(" "))
            if (index == -1) throw DriverActionException("No option matching '${args.joinToString(" ")}'")
            choose(index)
            "OK"
        }
    }

    private var selected: String? by mutableStateOf(null)
    private var options: List<String> by mutableStateOf(emptyList())
    private var choose: ((index: Int) -> Unit)? = null
    private var open by mutableStateOf(false)

    public actual fun <T> bind(edits: MutableReactive<T>, data: Reactive<List<T>>, render: (T) -> String) {
        var items: List<T> = emptyList()
        reactive {
            items = data()
            options = items.map(render)
        }
        reactive { selected = render(edits()) }
        choose = { index ->
            val item = items[index]
            Action("Set Value", frequencyCap = null) { edits set item }.startAction(this)
        }
    }

    override fun inputModifier(takesInput: Boolean): Modifier = Modifier.clickable(
        interactionSource = interactions.source,
        indication = null,
        enabled = enabled && takesInput,
        onClick = { open = !open },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val theme = currentTheme.theme
        val color = theme.foreground.closestColor().toCompose()
        // Min constraints pass through so that, when the select is stretched, the arrow sits at its end.
        Box(modifier, propagateMinConstraints = true) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    theme.font.cased(selected ?: ""),
                    Modifier.weight(1f, fill = false).padding(end = theme.gap.toDp()),
                    style = theme.font.toTextStyle(color),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
                Canvas(Modifier.size(10.dp, 5.dp)) {
                    drawPath(Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width / 2, size.height)
                        close()
                    }, color)
                }
            }
            if (open) remember(options) {
                DropdownList(
                    options = options,
                    highlighted = options.indexOf(selected),
                    onChoose = {
                        open = false
                        choose?.invoke(it)
                    },
                    onDismiss = { open = false },
                )
            }.Render(owner = this@Select, focusable = true)
        }
    }
}
