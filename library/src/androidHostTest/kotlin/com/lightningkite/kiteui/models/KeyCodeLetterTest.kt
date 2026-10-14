package com.lightningkite.kiteui.models

import android.view.KeyEvent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

/**
 * Locks in the fix from 94f1ebe6b: `KeyCodes.letter(char)` used to compute
 * `KEYCODE_A + char.code`, adding the raw Unicode code point (65 for 'A', 97 for 'a') to
 * KEYCODE_A (29) instead of an offset from 'A'. That produced codes far outside the valid
 * KEYCODE_A..KEYCODE_Z range (e.g. 'a' -> 29 + 97 = 126, which is KEYCODE_TAB, not KEYCODE_A).
 * The fix computes the offset as `char.uppercaseChar() - 'A'`, so codes land on the real
 * Android KEYCODE_A..KEYCODE_Z values regardless of case.
 */
@RunWith(RobolectricTestRunner::class)
class KeyCodeLetterTest {
    @Test
    fun uppercaseLettersMapToTheRealAndroidKeyCodes() {
        assertEquals(KeyEvent.KEYCODE_A, KeyCodes.letter('A'))
        assertEquals(KeyEvent.KEYCODE_B, KeyCodes.letter('B'))
        assertEquals(KeyEvent.KEYCODE_Z, KeyCodes.letter('Z'))
    }

    @Test
    fun lowercaseLettersMapToTheSameCodesAsUppercase() {
        // The old `KEYCODE_A + char.code` formula was also case-sensitive in a way the real
        // KeyEvent API isn't - 'a' and 'A' must resolve to the same physical key.
        assertEquals(KeyEvent.KEYCODE_A, KeyCodes.letter('a'))
        assertEquals(KeyEvent.KEYCODE_M, KeyCodes.letter('m'))
        assertEquals(KeyEvent.KEYCODE_Z, KeyCodes.letter('z'))
    }

    @Test
    fun everyLetterCodeFallsWithinTheValidAToZRange() {
        // Regression guard for the "produced codes outside the valid range entirely" defect:
        // under the old formula, most letters overflowed past KEYCODE_Z into unrelated keys.
        for (c in 'A'..'Z') {
            val code = KeyCodes.letter(c)
            assertEquals(true, code in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z, "letter('$c') = $code is outside KEYCODE_A..KEYCODE_Z")
        }
    }
}
