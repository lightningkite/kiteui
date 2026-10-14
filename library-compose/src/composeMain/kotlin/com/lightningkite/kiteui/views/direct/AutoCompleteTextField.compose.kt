package com.lightningkite.kiteui.views.direct

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*
import kotlinx.datetime.*
import kotlin.time.Duration

public actual class AutoCompleteTextField actual constructor(context: ElementContext) : NativeElementWithAction(context) {
    init { throw NotImplementedError("Compose backend: AutoCompleteTextField is not implemented yet") }

    public actual val content: MutableReactiveValue<String> = Signal("")
    public actual var keyboardHints: KeyboardHints = KeyboardHints()
    public actual var hint: String = ""
    public actual var suggestions: List<String> = emptyList()

    @Composable
    override fun Content(modifier: Modifier) {}
}
