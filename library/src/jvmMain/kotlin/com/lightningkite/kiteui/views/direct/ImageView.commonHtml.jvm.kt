package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.Blob
import com.lightningkite.kiteui.FileReference
import com.lightningkite.kiteui.views.FutureElement
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.src

actual fun createObjectURL(blob: Blob): String = ""

actual fun createObjectURL(fileReference: FileReference): String = ""

actual fun RView.nativeSetSrc(url: String?) {
    native.clearChildren()
    native.appendChild(FutureElement().apply {
        tag = "img"
        attributes.src = url
    })
}
