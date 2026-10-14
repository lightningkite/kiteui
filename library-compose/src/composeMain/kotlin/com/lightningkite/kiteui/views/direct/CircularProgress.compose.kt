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

public actual class CircularProgress actual constructor(context: ElementContext) : NativeElement(context) {
    init { throw NotImplementedError("Compose backend: CircularProgress is not implemented yet") }

    public actual var ratio: Float = 0f

    @Composable
    override fun Content(modifier: Modifier) {}
}
