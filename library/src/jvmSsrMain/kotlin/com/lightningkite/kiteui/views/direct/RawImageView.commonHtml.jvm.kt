package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.alt
import com.lightningkite.kiteui.views.src

public actual fun RawImageViewLike.nativeLoad(url: String?) {
    native.attributes.src = url
    native.attributes.alt = description  // by Claude - SEO alt attribute for SSR
}