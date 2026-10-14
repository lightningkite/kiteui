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

/**
 * `suggestions` used to be backed by `(native.adapter as KiteUiStringAdapter).items` on read,
 * which threw a `NullPointerException`/`ClassCastException` if read before any write - `native.adapter`
 * is null until the first `setAdapter` call. It is now backed by a plain field, so a read before any
 * write is safe, and the underlying `AutoCompleteTextView` still gets a real adapter wired up on write.
 */
@RunWith(RobolectricTestRunner::class)
class AutoCompleteTextFieldSuggestionsTest {
    class TestActivity : KiteUiActivity() {
        override val mainNavigator: PageNavigator = PageNavigator { Routes(listOf(), mapOf(), Page.Empty) }
        override val theme: ReactiveContext.() -> Theme = { Theme(id = "unitTest") }
        lateinit var field: AutoCompleteTextField

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            with(viewWriter) {
                frame {
                    field = autoCompleteTextField {}
                }
            }
        }
    }

    @Test
    fun readingSuggestionsBeforeAnyWriteDoesNotThrow() {
        Robolectric.buildActivity(TestActivity::class.java).use { controller ->
            controller.setup()
            val field = controller.get().field

            assertEquals(emptyList(), field.suggestions)
        }
    }

    @Test
    fun writtenSuggestionsAreReadBackAndReachTheAndroidAdapter() {
        Robolectric.buildActivity(TestActivity::class.java).use { controller ->
            controller.setup()
            val field = controller.get().field

            field.suggestions = listOf("Apple", "Banana", "Cherry")

            assertEquals(listOf("Apple", "Banana", "Cherry"), field.suggestions)
            val adapter = field.native.adapter
                ?: error("setting suggestions should install an adapter on the AutoCompleteTextView")
            assertEquals(3, adapter.count)
            assertEquals("Apple", adapter.getItem(0))
            assertEquals("Banana", adapter.getItem(1))
            assertEquals("Cherry", adapter.getItem(2))
        }
    }

    @Test
    fun reassigningSuggestionsReplacesThePreviousAdapterContents() {
        Robolectric.buildActivity(TestActivity::class.java).use { controller ->
            controller.setup()
            val field = controller.get().field

            field.suggestions = listOf("First")
            field.suggestions = listOf("Second", "Third")

            assertEquals(listOf("Second", "Third"), field.suggestions)
            val adapter = field.native.adapter ?: error("adapter should still be installed")
            assertEquals(2, adapter.count)
        }
    }
}
