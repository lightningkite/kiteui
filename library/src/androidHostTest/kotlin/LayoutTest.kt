package com.lightningkite.kiteui

import android.os.Bundle
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.Routes
import com.lightningkite.kiteui.navigation.render
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import kotlin.test.Ignore

@RunWith(RobolectricTestRunner::class)
class LayoutTest {
    class TestActivity: KiteUiActivity() {
        companion object {
            val testId = 517238
        }

        override val mainNavigator: PageNavigator = PageNavigator { Routes(listOf(), mapOf(), Page.Empty) }

        val s = LayoutsTestPage()

        override val theme: ReactiveContext.() -> Theme = { Theme(id = "unitTest") }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setTheme(R.style.Theme_Mppexample)
            with(viewWriter) {
                frame {
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