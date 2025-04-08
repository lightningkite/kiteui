package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.render
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.direct.stack
import kotlin.test.Test

class LayoutTest {
    @Test
    fun test() {
        val s = LayoutsTestPage()
        lateinit var root: RView
        root(Theme(id = "unitTest")) {
            frame {
                s.render(this)
            }.also { root = it }
        }
        println(root.screenRectangle())
        s.checks.forEach { it() }
    }
}
