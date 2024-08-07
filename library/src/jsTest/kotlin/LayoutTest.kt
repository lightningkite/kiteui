package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.render
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.direct.stack
import kotlin.test.Test

class LayoutTest {
    @Test
    fun test() {
        val s = LayoutsTestScreen()
        lateinit var root: RView
        root(Theme(id = "unitTest")) {
            root = stack {
                s.render(this)
            }
        }
        println(root.screenRectangle())
        s.checks.forEach { it() }
    }
}
