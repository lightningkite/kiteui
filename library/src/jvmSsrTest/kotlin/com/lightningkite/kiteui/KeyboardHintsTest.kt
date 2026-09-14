package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.Frame
import com.lightningkite.kiteui.views.direct.textInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies the web `autocomplete` attribute ("autocomplete tokens") emitted for text inputs.
 *
 * See KeyboardHints.ext.kt `applyKeyboardHints`. Fields with no explicit autocomplete hint must let
 * the browser default apply (not `autocomplete="off"`), and the one-time-code token must be emitted.
 */
class KeyboardHintsTest {
    init {
        Dispatchers.setMain(Dispatchers.IO)
    }

    private fun renderInput(hints: KeyboardHints = KeyboardHints()): String {
        val context = ElementContext("/")
        val root = Frame(context)
        with(root) {
            textInput { keyboardHints = hints }
        }
        return buildString { root.native.render(this) }
    }

    @Test
    fun noHintsDoesNotDisableAutocomplete() {
        val html = renderInput() // no KeyboardHints set
        assertFalse(html.contains("autocomplete='off'"), "plain text input must not force autocomplete='off'")
    }

    @Test
    fun passwordEmitsCurrentPasswordToken() {
        val html = renderInput(KeyboardHints.password)
        assertTrue(html.contains("current-password"), "password field should emit current-password token")
    }

    @Test
    fun newPasswordEmitsNewPasswordToken() {
        val html = renderInput(KeyboardHints.newPassword)
        assertTrue(html.contains("new-password"), "new password field should emit new-password token")
    }

    @Test
    fun oneTimeCodeEmitsOneTimeCodeToken() {
        val html = renderInput(KeyboardHints.oneTimeCode)
        assertFalse(html.contains("autocomplete='off'"), "one-time-code field must not force autocomplete='off'")
        assertTrue(html.contains("one-time-code"), "one-time-code field should emit one-time-code token")
    }

    @Test
    fun emailEmitsEmailToken() {
        val html = renderInput(KeyboardHints.email)
        assertTrue(html.contains("autocomplete='email'"), "email field should emit email token")
    }
}
