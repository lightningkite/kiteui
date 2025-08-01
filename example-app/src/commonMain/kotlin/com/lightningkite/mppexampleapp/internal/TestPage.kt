package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.row

@Routable("test")
public class TestPage: Page {
    public override fun ViewWriter.render(): ViewModifiable = run {
        col {
            row {
            }
        }
    }
}