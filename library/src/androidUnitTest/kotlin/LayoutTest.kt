package com.lightningkite.kiteui

import android.os.Bundle
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.Routes
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.navigation.ScreenNavigator
import com.lightningkite.kiteui.navigation.render
import com.lightningkite.kiteui.views.direct.stack
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class LayoutTest {
    class TestActivity: KiteUiActivity() {
        companion object {
            val testId = 517238
        }

        override val mainNavigator: ScreenNavigator = ScreenNavigator { Routes(listOf(), mapOf(), Screen.Empty) }

        val s = LayoutsTestScreen()

        override val theme: ReactiveContext<*>.() -> Theme = { Theme(id = "unitTest") }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setTheme(R.style.Theme_Mppexample)
            with(viewWriter) {
                stack {
                    s.render(this)
                }
            }
        }
    }
    @Test fun test() {
        Robolectric.buildActivity(TestActivity::class.java).use { controller ->
            controller.setup()
            controller.get().s.checks.forEach { it() }
        }
    }
}