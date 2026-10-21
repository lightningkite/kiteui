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

public actual class WebView actual constructor(context: ElementContext) : NativeElement(context) {
    init { throw UnsupportedOperationException("WebView is not supported by the Compose backend") }

    public actual fun load(source: WebViewSource, permissions: Set<WebViewPermission>) {}
    public actual val source: WebViewSource? = null
    public actual val permissions: Set<WebViewPermission> = emptySet()

    @Composable
    override fun Content(modifier: Modifier) {}
}
