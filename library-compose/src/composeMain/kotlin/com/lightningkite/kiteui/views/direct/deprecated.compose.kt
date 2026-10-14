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

@Deprecated("Use coordinatorFrame instead.")
public actual fun ElementContext.openBottomSheet(halfScreenRatio: Float, dim: Boolean, view: ElementWriter.CanAddTheme.() -> Unit): Unit =
    TODO("Compose backend: openBottomSheet")
