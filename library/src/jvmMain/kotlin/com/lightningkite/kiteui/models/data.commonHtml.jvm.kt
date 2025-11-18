package com.lightningkite.kiteui.models

import com.lightningkite.kiteui.Blob
import io.ktor.util.encodeBase64

internal actual fun createObjectUrl(data: Blob): String = "data:${data.type};base64,${data.data.encodeBase64()}"
internal actual fun revokeObjectUrl(url: String) = Unit