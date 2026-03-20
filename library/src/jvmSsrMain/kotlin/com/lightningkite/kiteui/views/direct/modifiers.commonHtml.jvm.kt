package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.ContainerElement
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.hidden
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*


internal actual fun ContainerElement.nativeAnimateShow() {}
internal actual fun ContainerElement.nativeAnimateHide() {}
// by Claude - no-op for SSR (no animation runtime)
internal actual fun ContainerElement.nativeAnimateWeight(fromWeight: Float, toWeight: Float) {}