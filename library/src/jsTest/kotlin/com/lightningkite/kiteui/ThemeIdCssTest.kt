package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.Semantic
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.native
import com.lightningkite.kiteui.views.themed
import org.w3c.dom.HTMLElement
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A theme's id becomes its CSS class.  An id containing '.' (e.g. one built from a hue like 0.38)
 * used to go into selectors unescaped, so `.t-brand-0.38` matched nothing and the theme never painted.
 */
class ThemeIdCssTest {

    private object Red : Semantic("themeIdCssTestRed") {
        override fun default(theme: Theme): ThemeAndBack = theme.withBack(background = Color.red)
    }

    @Test
    fun anIdWithADecimalStillPaints() {
        lateinit var el: Element
        root(Theme(id = "brand-0.38 x_y", background = Color.white)) {
            col { themed(Red).col { el = this; text("x") } }
        }
        assertEquals("rgb(255, 0, 0)", kotlinx.browser.window.getComputedStyle(el.native.element as HTMLElement).backgroundColor)
    }
}
