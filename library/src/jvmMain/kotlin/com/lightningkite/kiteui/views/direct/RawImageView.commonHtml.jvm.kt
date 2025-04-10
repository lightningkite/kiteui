package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.src

actual fun RawImageViewLike.nativeLoad(url: String?) {
    native.attributes.src = url
}