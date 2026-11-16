package com.lightningkite.kiteui.views.direct

import android.os.Bundle
import com.lightningkite.kiteui.KiteUiActivity
import com.lightningkite.kiteui.R
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.Routes
import com.lightningkite.kiteui.views.closePopovers
import com.lightningkite.kiteui.views.dismissTopDialog
import com.lightningkite.kiteui.views.l2.overlayFrame
import com.lightningkite.reactive.context.ReactiveContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MenuButtonBackTest {
    class TestActivity : KiteUiActivity() {
        override val mainNavigator: PageNavigator = PageNavigator { Routes(listOf(), mapOf(), Page.Empty) }
        override val theme: ReactiveContext.() -> Theme = { Theme(id = "unitTest") }
        lateinit var menu: MenuButton

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setTheme(R.style.Theme_Mppexample)
            with(viewWriter) {
                frame {
                    context.overlayFrame = this
                    menu = menuButton {
                        text("Open")
                        opensMenu { text("Item") }
                    }
                }
            }
        }
    }

    @Test
    fun systemBackClosesOpenMenu() {
        Robolectric.buildActivity(TestActivity::class.java).use { controller ->
            controller.setup()
            val activity = controller.get()
            val overlayFrame = activity.viewWriter.context.overlayFrame
            val before = overlayFrame.children.size

            activity.menu.native.performClick()
            assertEquals(before + 1, overlayFrame.children.size, "menu should be open")

            activity.onBackPressedDispatcher.onBackPressed()
            assertEquals(before, overlayFrame.children.size, "back should close the menu")
            assertFalse(activity.isFinishing, "back should not also leave the page")
        }
    }

    @Test
    fun menuClosedOtherwiseLeavesNothingForBack() {
        Robolectric.buildActivity(TestActivity::class.java).use { controller ->
            controller.setup()
            val activity = controller.get()

            activity.menu.native.performClick()
            activity.menu.context.closePopovers()

            assertFalse(activity.viewWriter.context.dismissTopDialog())
        }
    }
}
