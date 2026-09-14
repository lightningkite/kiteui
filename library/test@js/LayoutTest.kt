package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.render
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.direct.stack
import kotlin.test.Test

class LayoutTest {
    @Test
    fun test() {
        val s = LayoutsTestPage()
        lateinit var root: Element
        root(Theme(id = "unitTest")) {
            frame {
                s.render(this)
            }.also { root = it }
        }
        s.checks.forEach { it() }
    }
}
