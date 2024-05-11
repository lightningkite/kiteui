package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.Writable

actual val Video.nativeTime: Writable<Double> get() = Property(0.0)
actual val Video.nativePlaying: Writable<Boolean> get() = Property(false).also {
    it.addListener {
        native.attributes.autoplay = it.value
    }
}
actual val Video.nativeVolume: Writable<Float> get() = Property(1.0f)