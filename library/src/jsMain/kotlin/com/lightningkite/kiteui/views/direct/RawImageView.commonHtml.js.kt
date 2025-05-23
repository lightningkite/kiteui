package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.src
import com.lightningkite.signal.ReadableState
import org.w3c.dom.HTMLImageElement

actual fun RawImageViewLike.nativeLoad(url: String?) {
    native.onElement {
        it as HTMLImageElement
        it.onerror = { dyn, msg, a, b, c -> _state.state = ReadableState.exception(Exception("Failed to load image: $msg")) }
        it.onload = label@{ _state.state = ReadableState(Unit) }
        it.src = url ?: ""
    }
}