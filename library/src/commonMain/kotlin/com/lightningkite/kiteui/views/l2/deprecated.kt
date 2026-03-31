package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.direct.icon as correctIcon

@Deprecated("Import has moved", ReplaceWith("this.icon(icon, description)", "com.lightningkite.kiteui.views.direct.icon"))
fun ElementWriter.icon(icon: Icon, description: String) = correctIcon(icon, description)