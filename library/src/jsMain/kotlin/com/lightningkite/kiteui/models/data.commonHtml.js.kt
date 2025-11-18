package com.lightningkite.kiteui.models

import com.lightningkite.kiteui.Blob
import org.w3c.dom.url.URL

internal actual fun createObjectUrl(data: Blob): String = URL.createObjectURL(data)
internal actual fun revokeObjectUrl(url: String) = URL.revokeObjectURL(url)
