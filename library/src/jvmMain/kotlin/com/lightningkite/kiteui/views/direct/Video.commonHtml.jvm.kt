package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.autoplay
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

@InternalKiteUi
public actual val Video.nativeTime: MutableReactive<Double> get() = Signal(0.0)
@InternalKiteUi
public actual val Video.nativePlaying: MutableReactive<Boolean> get() = Signal(false).also {
    it.addListener {
        native.attributes.autoplay = it.value
    }
}
public actual val Video.nativeVolume: MutableReactive<Float> get() = Signal(1.0f)