package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal

@Routable("experiment")
object ExperimentPage : Page {
    override val title: Reactive<String>
        get() = super.title

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        col {
            val prop = Signal(true)
            shownWhen { prop() }.text("A")
            shownWhen { !prop() }.text("B")
            button {
                text("Toggle")
                onClick { prop.value = !prop.value }
            }
        }
    }
}
