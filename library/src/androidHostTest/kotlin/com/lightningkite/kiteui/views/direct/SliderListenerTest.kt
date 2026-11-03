package com.lightningkite.kiteui.views.direct

import android.os.Bundle
import android.widget.SeekBar
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
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals

/**
 * Locks in the fix from 94f1ebe6b: `Slider.value` used to be a computed property that returned a
 * brand new `MutableReactiveValue` (and a brand new `SeekBar.OnSeekBarChangeListener`) on every
 * access. `SeekBar.setOnSeekBarChangeListener` only holds a single listener slot, so calling
 * `value.addListener {}` a second time silently evicted the first subscriber - only the most
 * recently added listener ever fired. `value` is now a stable `val` backed by `BaseListenable`,
 * which multicasts every SeekBar callback to all subscribers off of one shared listener.
 */
@RunWith(RobolectricTestRunner::class)
class SliderListenerTest {
    class TestActivity : KiteUiActivity() {
        override val mainNavigator: PageNavigator = PageNavigator { Routes(listOf(), mapOf(), Page.Empty) }
        override val theme: ReactiveContext.() -> Theme = { Theme(id = "unitTest") }
        lateinit var slider: Slider

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            with(viewWriter) {
                frame {
                    slider = slider {}
                }
            }
        }
    }

    @Test
    fun bothSubscribersReceiveTheSameUserDrivenUpdate() {
        Robolectric.buildActivity(TestActivity::class.java).use { controller ->
            controller.setup()
            val slider = controller.get().slider

            var firstCount = 0
            var secondCount = 0
            slider.value.addListener { firstCount++ }
            slider.value.addListener { secondCount++ }

            // Drive the exact listener SeekBar currently holds, the way a real user drag would:
            // grabbing it through the Robolectric shadow (rather than reaching into Slider's
            // private fields) exercises the real installed production listener.
            val seekBarListener: SeekBar.OnSeekBarChangeListener = shadowOf(slider.native).onSeekBarChangeListener
                ?: error("Slider did not install a SeekBar listener")
            // Not 500, which is the starting value: listeners hear changes only.
            seekBarListener.onProgressChanged(slider.native, 700, true)

            assertEquals(1, firstCount, "first subscriber should have fired")
            assertEquals(1, secondCount, "second subscriber should have fired too - this is exactly what the fresh-object-per-access bug broke, since adding it used to evict the first subscriber's listener")
        }
    }

    @Test
    fun nonUserProgressChangesDoNotNotifyListeners() {
        // Guards the `if (fromUser)` gate: programmatic progress changes (e.g. from min/max/step
        // adjusting the SeekBar to stay in range) must not fire reactive listeners.
        Robolectric.buildActivity(TestActivity::class.java).use { controller ->
            controller.setup()
            val slider = controller.get().slider

            var count = 0
            slider.value.addListener { count++ }

            val seekBarListener: SeekBar.OnSeekBarChangeListener = shadowOf(slider.native).onSeekBarChangeListener
                ?: error("Slider did not install a SeekBar listener")
            seekBarListener.onProgressChanged(slider.native, 500, false)

            assertEquals(0, count)
        }
    }

    // The same rules as the other platforms (see SliderValueTest), and the value is exact rather than one of the
    // SeekBar's 1000 positions.
    @Test
    fun holdsTheSameValuesAsTheOtherPlatforms() {
        Robolectric.buildActivity(TestActivity::class.java).use { controller ->
            controller.setup()
            val slider = controller.get().slider
            var count = 0
            slider.value.addListener { count++ }

            slider.range(18f, 100f)
            assertEquals(18f, slider.value.value)
            assertEquals(1, count, "a range that moves the value tells listeners, as elsewhere")

            slider.value.value = 33.3337f
            assertEquals(33.3337f, slider.value.value)

            slider.step = 2f
            assertEquals(34f, slider.value.value)
            slider.step = 0f
            assertEquals(1f, slider.step)
        }
    }
}
