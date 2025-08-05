package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.views.src

@InternalKiteUi
public actual fun RawImageViewLike.nativeLoad(url: String?) {
    native.attributes.src = url
}