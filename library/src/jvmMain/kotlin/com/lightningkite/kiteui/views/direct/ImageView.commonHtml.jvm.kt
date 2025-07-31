package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.Blob
import com.lightningkite.kiteui.FileReference
import com.lightningkite.kiteui.views.FutureElement
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.src

public actual fun createObjectURL(blob: Blob): String = ""

public actual fun createObjectURL(fileReference: FileReference): String = ""
