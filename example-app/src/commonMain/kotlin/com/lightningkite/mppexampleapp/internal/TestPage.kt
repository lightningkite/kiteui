package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.row

@Routable("test")
class TestPage: Page {
    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        col {
            row {
            }
        }
    }
}