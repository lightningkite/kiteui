package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.lightningkite.kiteui.models.HoverSemantic
import com.lightningkite.kiteui.models.PopoverSemantic
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.models.toDp
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.shape
import com.lightningkite.kiteui.views.theme
import com.lightningkite.kiteui.views.toBrush
import com.lightningkite.kiteui.views.toCompose
import com.lightningkite.kiteui.views.toTextStyle

/**
 * The list of choices that [Select] and [AutoCompleteTextField] open below themselves, at least as wide as the owner.
 * Like a native <select> or <datalist> list, it answers the arrow keys, Enter and Escape through [onKeyEvent].
 */
internal class DropdownList(
    private val options: List<String>,
    highlighted: Int,
    private val onChoose: (index: Int) -> Unit,
    private val onDismiss: () -> Unit,
) {
    private var highlighted by mutableIntStateOf(highlighted)

    fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        when (event.key) {
            Key.DirectionDown -> highlighted = (highlighted + 1).coerceAtMost(options.lastIndex)
            Key.DirectionUp -> highlighted = (highlighted - 1).coerceAtLeast(0)
            Key.Enter, Key.NumPadEnter -> if (highlighted in options.indices) onChoose(highlighted) else return false
            Key.Escape -> onDismiss()
            else -> return false
        }
        return true
    }

    // A focusable popup also swallows the outside click that dismisses it, so clicking the owner closes the list rather
    // than reopening it.  A text field's list must not take focus, though, or typing would stop; the field passes
    // its keys to onKeyEvent instead.
    @Composable
    fun Render(owner: NativeElement, focusable: Boolean) {
        // Styled like other popovers, from the surroundings rather than the owner, whose theme follows its hover and focus.
        val theme = (owner.parent?.theme ?: owner.theme)[PopoverSemantic].theme
        val density = LocalDensity.current.density
        // Compose anchors a popup to the composable that declares it, which here is the owner's padded content;
        // the list belongs against the owner's outer edge instead.
        val position = object : PopupPositionProvider {
            override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
                val ownerBounds = checkNotNull(owner.screenRectangle()) { "A dropdown can only open from an element that is on screen" }
                val top = (ownerBounds.top * density).toInt()
                val bottom = (ownerBounds.bottom * density).toInt()
                val fitsBelow = bottom + popupContentSize.height <= windowSize.height
                return IntOffset(
                    x = (ownerBounds.left * density).toInt(),
                    y = if (fitsBelow) bottom else (top - popupContentSize.height).coerceAtLeast(0),
                )
            }
        }
        val ownerWidth = checkNotNull(owner.screenRectangle()) { "A dropdown can only open from an element that is on screen" }.width
        Popup(popupPositionProvider = position, onDismissRequest = onDismiss, properties = PopupProperties(focusable = focusable)) {
            @Suppress("DEPRECATION")
            val shape = theme.shape(owner.parent?.spacingForChildCornerRadii ?: 0.px)
            val focus = remember { FocusRequester() }
            Column(
                Modifier
                    .widthIn(min = ownerWidth.dp)
                    .width(IntrinsicSize.Max)
                    .shadow(theme.elevation.toDp(), shape)
                    .background(theme.background.toBrush(), shape)
                    .border(theme.outlineWidth.toDp(), theme.outline.toBrush(), shape)
                    .clip(shape)
                    .verticalScroll(rememberScrollState())
                    .then(if (focusable) Modifier.focusRequester(focus).onKeyEvent(::onKeyEvent).focusable() else Modifier)
            ) {
                for ((index, option) in options.withIndex()) {
                    val optionTheme = if (index == highlighted) theme[HoverSemantic].theme else theme
                    BasicText(
                        option,
                        style = optionTheme.font.toTextStyle(optionTheme.foreground.closestColor().toCompose()),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (index == highlighted) Modifier.background(optionTheme.background.toBrush()) else Modifier)
                            .pointerInput(index) {
                                awaitPointerEventScope {
                                    while (true) if (awaitPointerEvent().type == PointerEventType.Enter) highlighted = index
                                }
                            }
                            .clickable(indication = null, interactionSource = null) { onChoose(index) }
                            .padding(start = theme.padding.left.toDp(), top = theme.padding.top.toDp(), end = theme.padding.right.toDp(), bottom = theme.padding.bottom.toDp()),
                    )
                }
            }
            if (focusable) LaunchedEffect(Unit) { focus.requestFocus() }
        }
    }
}
