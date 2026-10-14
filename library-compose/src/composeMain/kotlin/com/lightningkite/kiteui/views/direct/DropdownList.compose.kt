package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.lightningkite.kiteui.models.toDp
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.theme
import com.lightningkite.kiteui.views.toBrush
import com.lightningkite.kiteui.views.toCompose
import com.lightningkite.kiteui.views.toTextStyle

/** The list of choices that [Select] and [AutoCompleteTextField] open below themselves, at least as wide as [owner]. */
@Composable
internal fun DropdownList(owner: NativeElement, options: List<String>, focusable: Boolean, onChoose: (index: Int) -> Unit, onDismiss: () -> Unit) {
    // Styled like other popovers, from the surroundings rather than the owner, whose theme follows its hover and focus.
    val theme = (owner.parent?.theme ?: owner.theme)[PopoverSemantic].theme
    // Compose anchors a popup to the composable that declares it, which here is the owner's padded content;
    // the list belongs against the owner's outer edge instead.
    val ownerBounds = checkNotNull(owner.screenRectangle()) { "A dropdown can only open from an element that has been laid out" }
    val density = LocalDensity.current.density
    val position = object : PopupPositionProvider {
        override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
            val top = (ownerBounds.top * density).toInt()
            val bottom = (ownerBounds.bottom * density).toInt()
            val fitsBelow = bottom + popupContentSize.height <= windowSize.height
            return IntOffset(
                x = (ownerBounds.left * density).toInt(),
                y = if (fitsBelow) bottom else (top - popupContentSize.height).coerceAtLeast(0),
            )
        }
    }
    // A focusable popup also swallows the outside click that dismisses it, so clicking the owner closes the list rather
    // than reopening it.  A text field's list must not take focus, though, or typing would stop.
    Popup(popupPositionProvider = position, onDismissRequest = onDismiss, properties = PopupProperties(focusable = focusable)) {
        val shape = RoundedCornerShape(8.dp)
        Column(
            Modifier
                .widthIn(min = (ownerBounds.right - ownerBounds.left).dp)
                .width(IntrinsicSize.Max)
                .shadow(theme.elevation.toDp(), shape)
                .background(theme.background.toBrush(), shape)
                .border(theme.outlineWidth.toDp(), theme.outline.toBrush(), shape)
                .clip(shape)
                .verticalScroll(rememberScrollState())
        ) {
            for ((index, option) in options.withIndex()) {
                val hover = remember { MutableInteractionSource() }
                val hovered by hover.collectIsHoveredAsState()
                val optionTheme = if (hovered) theme[HoverSemantic].theme else theme
                BasicText(
                    option,
                    style = optionTheme.font.toTextStyle(optionTheme.foreground.closestColor().toCompose()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (hovered) Modifier.background(optionTheme.background.toBrush()) else Modifier)
                        .hoverable(hover)
                        .clickable(interactionSource = hover, indication = null) { onChoose(index) }
                        .padding(start = theme.padding.left.toDp(), top = theme.padding.top.toDp(), end = theme.padding.right.toDp(), bottom = theme.padding.bottom.toDp()),
                )
            }
        }
    }
}
