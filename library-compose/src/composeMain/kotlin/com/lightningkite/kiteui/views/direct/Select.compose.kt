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

public actual class Select actual constructor(context: ElementContext) : NativeInteractiveElement(context) {
    init { throw NotImplementedError("Compose backend: Select is not implemented yet") }

    public actual fun <T> bind(edits: MutableReactive<T>, data: Reactive<List<T>>, render: (T) -> String) {}

    @Composable
    override fun Content(modifier: Modifier) {}
}
