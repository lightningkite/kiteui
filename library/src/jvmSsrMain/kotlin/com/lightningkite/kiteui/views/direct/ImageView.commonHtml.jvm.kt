package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.Blob
import com.lightningkite.kiteui.FileReference
import com.lightningkite.kiteui.views.FutureElement
import com.lightningkite.kiteui.views.src

actual fun createObjectURL(blob: Blob): String = ""

actual fun createObjectURL(fileReference: FileReference): String = ""

// by Claude - no-op on JVM SSR since blob URLs don't exist
actual fun revokeObjectURL(url: String) {}
