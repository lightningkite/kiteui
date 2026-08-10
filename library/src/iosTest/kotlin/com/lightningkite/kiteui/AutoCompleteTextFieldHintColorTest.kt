package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.Semantic
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.render
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.direct.AutoCompleteTextField
import com.lightningkite.kiteui.views.direct.autoCompleteTextField
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.setup
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

// A second, contrasting foreground so the "recolors on theme change" half of the test can tell
// the pre-change and post-change placeholder colors apart.
private object AutoCompleteBlueForegroundSemantic : Semantic("autoCompleteHintColorTest") {
    override fun default(theme: Theme) =
        theme.withBack(foreground = Color(alpha = 1f, red = 0f, green = 0f, blue = 1f))
}

private class AutoCompleteHintColorPage : Page {
    lateinit var target: AutoCompleteTextField
    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        frame {
            target = autoCompleteTextField { hint = "Search..." }
        }
    }
}

/**
 * Locks in two fixes to `AutoCompleteTextField.ios.kt`'s hint rendering:
 *
 * 1. `updateHint()` used to set the plain `textField.placeholder`, which iOS always draws in a
 *    fixed system grey - close to invisible on a dark theme. It now builds an `NSAttributedString`
 *    colored from the theme's foreground, matching the fix already present in `TextField.ios.kt`.
 * 2. `nativeApplyTheme` did not call `updateHint()`, so an already-set placeholder never picked up
 *    a new theme's color - only the color active when `hint` was first assigned. `nativeApplyTheme`
 *    now calls `updateHint()` on every theme change.
 *
 * The assertions compare `attributedPlaceholder` structurally (`NSAttributedString.isEqual:`
 * compares string content and attribute runs) rather than decoding individual attribute values,
 * since reading attribute runs back out needs `NSForegroundColorAttributeName`, which this test
 * source set cannot resolve even though the identical symbol compiles fine in `iosMain` - a
 * pre-existing cinterop quirk unrelated to this fix.
 */
@OptIn(ExperimentalForeignApi::class)
class AutoCompleteTextFieldHintColorTest {
    @Test
    fun placeholderIsColoredFromTheThemeAndRecolorsWhenTheThemeChanges() {
        val page = AutoCompleteHintColorPage()
        val window = UIWindow(frame = CGRectMake(0.0, 0.0, 500.0, 500.0))
        val vc = UIViewController(null, null)
        window.rootViewController = vc
        vc.view.setFrame(CGRectMake(0.0, 0.0, 500.0, 500.0))
        val initialForeground = Color(alpha = 1f, red = 1f, green = 0f, blue = 0f)
        vc.setup(Theme(id = "unitTest", foreground = initialForeground)) {
            frame { page.render(this) }
        }

        val placeholder1 = page.target.textField.attributedPlaceholder
        assertNotNull(placeholder1, "hint should produce a colored attributedPlaceholder, not fall back to the plain grey placeholder")
        assertEquals("Search...", placeholder1.string, "the placeholder text itself should still be the assigned hint")

        // Changing the theme after the field is already showing a placeholder is exactly the
        // regression this guards: without calling updateHint() from nativeApplyTheme, updateHint()
        // would never re-run here, and placeholder2 would be the identical (color-stale) instance.
        page.target.themeChoice = AutoCompleteBlueForegroundSemantic

        val placeholder2 = page.target.textField.attributedPlaceholder
        assertNotNull(placeholder2, "attributedPlaceholder should still be set after a theme change")
        assertEquals("Search...", placeholder2.string, "the placeholder text should be unaffected by the theme change")
        assertNotEquals(placeholder1, placeholder2, "the placeholder should have been rebuilt with the new theme's color - if it's unchanged, updateHint() did not re-run on the theme change")
    }
}
