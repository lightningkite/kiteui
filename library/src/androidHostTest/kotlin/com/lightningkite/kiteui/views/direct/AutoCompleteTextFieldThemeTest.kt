package com.lightningkite.kiteui.views.direct

import android.os.Bundle
import com.lightningkite.kiteui.KiteUiActivity
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.Routes
import com.lightningkite.reactive.context.ReactiveContext
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `AutoCompleteTextField` takes its colours from the theme on Android.
 *
 * It is an `EditText` underneath but does not extend [TextInput], so it inherited none of that
 * class's theming and sat on the platform defaults - black text and a black hint, unreadable on a
 * dark theme and wrong on every other one. The assertions below are relative rather than absolute
 * (hint is the text colour at half alpha) so they pin the *relationship* without freezing whatever
 * the unit-test theme's palette happens to be.
 */
@RunWith(RobolectricTestRunner::class)
class AutoCompleteTextFieldThemeTest {

    class TestActivity : KiteUiActivity() {
        override val mainNavigator: PageNavigator = PageNavigator { Routes(listOf(), mapOf(), Page.Empty) }
        override val theme: ReactiveContext.() -> Theme = { Theme(id = "unitTest") }
        lateinit var field: AutoCompleteTextField
        lateinit var plain: TextField

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            with(viewWriter) {
                col {
                    field = autoCompleteTextField { hint = "Search" }
                    plain = textField { hint = "Search" }
                }
            }
        }
    }

    private fun <T> withActivity(block: (TestActivity) -> T): T =
        Robolectric.buildActivity(TestActivity::class.java).use { controller ->
            controller.setup()
            block(controller.get())
        }

    @Test
    fun theHintIsTheThemeForegroundAtHalfAlpha() = withActivity { activity ->
        val text = activity.field.native.currentTextColor
        val hint = activity.field.native.hintTextColors.defaultColor

        assertEquals(
            android.graphics.Color.red(text), android.graphics.Color.red(hint),
            "the hint must be derived from the theme foreground, not a platform default",
        )
        assertEquals(android.graphics.Color.green(text), android.graphics.Color.green(hint))
        assertEquals(android.graphics.Color.blue(text), android.graphics.Color.blue(hint))
        assertTrue(
            android.graphics.Color.alpha(hint) in 120..136,
            "the hint should be about half-strength, was alpha ${android.graphics.Color.alpha(hint)}",
        )
    }

    @Test
    fun itMatchesThePlainTextFieldItSitsNextTo() = withActivity { activity ->
        // The two are used interchangeably on a form, so a visible difference between them is the
        // bug even if neither colour is objectively wrong.
        assertEquals(
            activity.plain.native.currentTextColor,
            activity.field.native.currentTextColor,
            "autocomplete and plain text fields must share a text colour",
        )
        assertEquals(
            activity.plain.native.hintTextColors.defaultColor,
            activity.field.native.hintTextColors.defaultColor,
            "autocomplete and plain text fields must share a hint colour",
        )
    }

    @Test
    fun theTextSizeComesFromTheTheme() = withActivity { activity ->
        assertEquals(
            activity.plain.native.textSize,
            activity.field.native.textSize,
            0.5f,
            "an unthemed field keeps the platform default size, which differs from the theme's",
        )
    }
}
