package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.Blob
import com.lightningkite.kiteui.FileReference
import com.lightningkite.kiteui.clockMillis
import com.lightningkite.kiteui.views.*
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.get
import org.w3c.dom.url.URL

@JsName("createObjectURLBlob")
public actual fun createObjectURL(blob: Blob): String {
    return URL.Companion.createObjectURL(blob)
}

@JsName("createObjectURLFileReference")
public actual fun createObjectURL(fileReference: FileReference): String {
    return URL.createObjectURL(fileReference)
}
