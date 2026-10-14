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

public actual class ActivityIndicator actual constructor(context: ElementContext) : NativeElement(context) {
    init { throw NotImplementedError("Compose backend: ActivityIndicator is not implemented yet") }

    @Composable
    override fun Content(modifier: Modifier) {}
}
