package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.setup
import com.lightningkite.kiteui.views.toUiColor
import com.lightningkite.reactive.core.Signal
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.*
import platform.UIKit.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Regression coverage for the iOS "colored hint" fix. Plain [platform.UIKit.UITextField.placeholder]
 * renders in a fixed iOS system gray that is near-invisible on dark themes; [NumberInput] and
 * [FormattedTextInput] now build an [NSAttributedString] placeholder colored from the theme
 * (mirroring [TextInput]'s existing approach), and both re-derive it whenever the theme changes
 * (previously only [TextInput] did this, so the other two never re-colored after their first theme
 * application). [TextArea] has no native placeholder at all (`UITextView` doesn't support one), so
 * it uses an overlay [platform.UIKit.UILabel] instead — this file also covers that label's color
 * and its empty/non-empty visibility toggling, including for programmatic content changes.
 */
@OptIn(ExperimentalForeignApi::class)
class HintColorTest {

    @Test
    fun numberInputHintRecolorsOnThemeChange() {
        val themeA = Theme(id = "hint-color-test-number-a", foreground = Color.black)
        val themeB = Theme(id = "hint-color-test-number-b", foreground = Color.white)
        val themeSignal = Signal<Theme>(themeA)

        val window = UIWindow(frame = CGRectMake(0.0, 0.0, 300.0, 100.0))
        val vc = UIViewController(null, null)
        window.rootViewController = vc
        vc.view.setFrame(CGRectMake(0.0, 0.0, 300.0, 100.0))
        lateinit var field: NumberInput
        vc.setup(themeSignal) {
            frame {
                numberInput {
                    hint = "Enter a number"
                    field = this
                }
            }
        }
        vc.view.setNeedsLayout()
        vc.view.layoutIfNeeded()

        val placeholderA = field.textField.attributedPlaceholder
        assertNotNull(placeholderA, "attributedPlaceholder should be set once a hint is applied")
        assertEquals("Enter a number", placeholderA.string)
        val colorA = placeholderA.attribute(NSForegroundColorAttributeName, atIndex = 0u, effectiveRange = null) as? UIColor
        assertNotNull(colorA, "attributedPlaceholder should carry a foreground color attribute")
        assertTrue(colorA.isEqual(themeA.foreground.closestColor().withAlpha(0.5f).toUiColor()), "hint should be colored from the initial theme's foreground")

        // Simulate a runtime theme change (e.g. light/dark toggle) and confirm the hint re-colors.
        themeSignal.value = themeB
        vc.view.setNeedsLayout()
        vc.view.layoutIfNeeded()

        val placeholderB = field.textField.attributedPlaceholder
        assertNotNull(placeholderB)
        val colorB = placeholderB.attribute(NSForegroundColorAttributeName, atIndex = 0u, effectiveRange = null) as? UIColor
        assertNotNull(colorB, "attributedPlaceholder should carry a foreground color attribute")
        assertTrue(colorB.isEqual(themeB.foreground.closestColor().withAlpha(0.5f).toUiColor()), "hint should re-color after a theme change")
        assertFalse(colorB.isEqual(themeA.foreground.closestColor().withAlpha(0.5f).toUiColor()), "hint should no longer match the old theme's color")
    }

    @Test
    fun formattedTextInputHintRecolorsOnThemeChange() {
        val themeA = Theme(id = "hint-color-test-formatted-a", foreground = Color.black)
        val themeB = Theme(id = "hint-color-test-formatted-b", foreground = Color.white)
        val themeSignal = Signal<Theme>(themeA)

        val window = UIWindow(frame = CGRectMake(0.0, 0.0, 300.0, 100.0))
        val vc = UIViewController(null, null)
        window.rootViewController = vc
        vc.view.setFrame(CGRectMake(0.0, 0.0, 300.0, 100.0))
        lateinit var field: FormattedTextInput
        vc.setup(themeSignal) {
            frame {
                formattedTextInput {
                    hint = "555-1234"
                    field = this
                }
            }
        }
        vc.view.setNeedsLayout()
        vc.view.layoutIfNeeded()

        val placeholderA = field.textField.attributedPlaceholder
        assertNotNull(placeholderA)
        val colorA = placeholderA.attribute(NSForegroundColorAttributeName, atIndex = 0u, effectiveRange = null) as? UIColor
        assertNotNull(colorA)
        assertTrue(colorA.isEqual(themeA.foreground.closestColor().withAlpha(0.5f).toUiColor()))

        themeSignal.value = themeB
        vc.view.setNeedsLayout()
        vc.view.layoutIfNeeded()

        val placeholderB = field.textField.attributedPlaceholder
        assertNotNull(placeholderB)
        val colorB = placeholderB.attribute(NSForegroundColorAttributeName, atIndex = 0u, effectiveRange = null) as? UIColor
        assertNotNull(colorB)
        assertTrue(colorB.isEqual(themeB.foreground.closestColor().withAlpha(0.5f).toUiColor()), "hint should re-color after a theme change")
    }

    @Test
    fun textAreaHintIsColoredAndTracksEmptinessIncludingProgrammaticChanges() {
        val theme = Theme(id = "hint-color-test-textarea", foreground = Color.white)

        val window = UIWindow(frame = CGRectMake(0.0, 0.0, 300.0, 150.0))
        val vc = UIViewController(null, null)
        window.rootViewController = vc
        vc.view.setFrame(CGRectMake(0.0, 0.0, 300.0, 150.0))
        lateinit var field: TextArea
        vc.setup(theme) {
            frame {
                textArea {
                    hint = "Write something"
                    field = this
                }
            }
        }
        vc.view.setNeedsLayout()
        vc.view.layoutIfNeeded()

        val expectedColor = theme.foreground.closestColor().withAlpha(0.5f).toUiColor()
        assertEquals("Write something", field.textField.hintLabel.text)
        assertTrue(field.textField.hintLabel.textColor.isEqual(expectedColor), "hint label should be colored from the theme")
        assertFalse(field.textField.hintLabel.hidden, "hint should be visible while the text area is empty")

        // UITextView never calls its delegate for programmatic text changes (only typing), so the
        // overlay must be re-evaluated through the same listener list that `content.value = ...`
        // fires for reactive listeners, not solely from UITextViewDelegate.textViewDidChange.
        field.content.value = "typed by a test, not the keyboard"
        assertTrue(field.textField.hintLabel.hidden, "hint should hide once there is programmatic content")

        field.content.value = ""
        assertFalse(field.textField.hintLabel.hidden, "hint should reappear once content is cleared programmatically")
    }
}
