package com.lightningkite.kiteui.views.direct

import android.os.Bundle
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import com.lightningkite.kiteui.KiteUiActivity
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.Routes
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.reactive.context.ReactiveContext
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class TextViewHtmlLinkTest {
    class TestActivity : KiteUiActivity() {
        val product = Page.Direct("Product") {}
        override val mainNavigator: PageNavigator = PageNavigator {
            Routes(listOf { path -> product.takeIf { path.segments == listOf("Product", "1") } }, mapOf(), Page.Empty)
        }
        override val theme: ReactiveContext.() -> Theme = { Theme(id = "unitTest") }
        lateinit var view: TextView

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            with(viewWriter) {
                context.pageNavigator = mainNavigator
                frame {
                    text {
                        view = this
                        setBasicHtmlContent("See (<a href=\"/Product/1\">the product</a>) or <a href=\"https://example.com\">elsewhere</a>")
                    }
                }
            }
        }
    }

    @Test
    fun aLinkIntoTheAppNavigatesInsteadOfGoingToTheOs() {
        Robolectric.buildActivity(TestActivity::class.java).use { controller ->
            controller.setup()
            val activity = controller.get()
            val text = activity.view.native.text as Spanned

            val inApp = text.getSpans(0, text.length, ClickableSpan::class.java).single { it !is URLSpan }
            assertEquals("the product", text.subSequence(text.getSpanStart(inApp), text.getSpanEnd(inApp)).toString())
            inApp.onClick(activity.view.native)
            assertEquals(activity.product, activity.mainNavigator.stack.value.last())

            assertEquals(listOf("https://example.com"), text.getSpans(0, text.length, URLSpan::class.java).map { it.url })
        }
    }
}
