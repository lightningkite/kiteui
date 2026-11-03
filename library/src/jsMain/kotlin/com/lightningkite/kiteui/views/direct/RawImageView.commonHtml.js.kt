package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.src
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import org.w3c.dom.HTMLImageElement

public actual fun RawImageViewLike.nativeLoad(url: String?) {
    native.onElement {
        it as HTMLImageElement
        it.alt = description  // by Claude - SEO alt attribute on actual DOM element
        it.onerror = { dyn, msg, a, b, c -> _state.state = ReactiveState.exception(Exception("Failed to load image: $msg")) }
        it.onload = label@{ _state.state = ReactiveState(Unit) }
        it.src = url ?: ""
    }
}