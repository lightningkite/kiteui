package com.lightningkite.mppexampleapp.widgets

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.toCompose
import com.lightningkite.kiteui.views.toTextStyle

// TODO: syntax highlighting, as the web version has.
actual class Code actual constructor(context: ElementContext) : NativeElement(context) {
    actual var content: String by mutableStateOf("")

    @Composable
    override fun Content(modifier: Modifier) {
        val theme = currentTheme.theme
        BasicText(
            content,
            modifier,
            style = theme.font.toTextStyle(theme.foreground.closestColor().toCompose()).copy(fontFamily = FontFamily.Monospace),
        )
    }
}
