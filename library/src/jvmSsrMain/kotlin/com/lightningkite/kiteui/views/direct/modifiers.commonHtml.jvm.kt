package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.hidden
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*


internal actual fun RView.nativeAnimateShow() {}
internal actual fun RView.nativeAnimateHide() {}
// by Claude - no-op for SSR (no animation runtime)
internal actual fun RView.nativeAnimateWeight(fromWeight: Float, toWeight: Float) {}
@PublishedApi
internal actual fun RView.nativeSetupPullToRefresh(refreshAction: Action) {
}