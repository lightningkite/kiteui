package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.testing.uiTest
import com.lightningkite.kiteui.views.direct.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression tests for the driver-value redaction fix (commit 94f1ebe6b).
 *
 * Password and new-password fields used to report their plaintext content through
 * `driverValue`, which feeds `snapshot()`/`find()` in the text-based driver protocol used
 * by test automation and MCP-connected agents. That bypassed the UI's visual masking
 * entirely. `redactedDriverValue()` in DriverOverrides.kt now swaps the real value for
 * "•".repeat(actual.length) whenever `KeyboardHints.autocomplete` is Password or
 * NewPassword, for TextInput, TextArea, AutoCompleteTextField and FormattedTextInput.
 *
 * Every "password"/"newPassword" test here would have failed before the fix (the snapshot
 * would have contained the plaintext instead of bullets). The "plain field" tests are the
 * negative case: they guard against over-redaction breaking the driver for every other field.
 */
class DriverRedactionTest {

    @Test
    fun textInputPasswordIsRedacted() = uiTest(
        content = {
            col {
                textInput {
                    debugName = "pw"
                    keyboardHints = KeyboardHints.password
                }
            }
        }
    ) {
        setValue("pw", "hunter2")
        val snap = snapshot("pw")
        assertTrue(snap.contains("•••••••"), "Should show 7 bullets for 'hunter2': $snap")
        assertTrue(!snap.contains("hunter2"), "Plaintext must never reach the snapshot: $snap")
    }

    @Test
    fun textInputNewPasswordIsRedacted() = uiTest(
        content = {
            col {
                textInput {
                    debugName = "newPw"
                    keyboardHints = KeyboardHints(autocomplete = com.lightningkite.kiteui.models.AutoComplete.NewPassword)
                }
            }
        }
    ) {
        setValue("newPw", "correcthorse")
        val snap = snapshot("newPw")
        assertTrue(!snap.contains("correcthorse"), "New-password plaintext must never reach the snapshot: $snap")
        assertTrue(snap.contains("•".repeat("correcthorse".length)), "Should redact to matching bullet count: $snap")
    }

    @Test
    fun textInputPlainFieldIsNotRedacted() = uiTest(
        content = {
            col {
                textInput { debugName = "plain" }
            }
        }
    ) {
        // Negative case: a normal field must still report its real value through the driver.
        setValue("plain", "hello world")
        assertValue("plain", "hello world")
    }

    @Test
    fun textAreaPasswordIsRedacted() = uiTest(
        content = {
            col {
                textArea {
                    debugName = "pwArea"
                    keyboardHints = KeyboardHints.password
                }
            }
        }
    ) {
        setValue("pwArea", "supersecret")
        val snap = snapshot("pwArea")
        assertTrue(!snap.contains("supersecret"), "TextArea password plaintext must never reach the snapshot: $snap")
        assertTrue(snap.contains("•".repeat("supersecret".length)), "Should redact to matching bullet count: $snap")
    }

    @Test
    fun textAreaPlainFieldIsNotRedacted() = uiTest(
        content = {
            col {
                textArea { debugName = "notes" }
            }
        }
    ) {
        // Negative case: TextArea's redaction check must not catch non-secret fields.
        setValue("notes", "just some notes")
        assertValue("notes", "just some notes")
    }

    @Test
    fun autoCompleteTextFieldPasswordIsRedacted() = uiTest(
        content = {
            col {
                autoCompleteTextField {
                    debugName = "pwAuto"
                    keyboardHints = KeyboardHints.password
                }
            }
        }
    ) {
        setValue("pwAuto", "abc12345")
        val snap = snapshot("pwAuto")
        assertTrue(!snap.contains("abc12345"), "AutoCompleteTextField password plaintext must never reach the snapshot: $snap")
        assertTrue(snap.contains("•".repeat("abc12345".length)), "Should redact to matching bullet count: $snap")
    }

    @Test
    fun autoCompleteTextFieldPlainFieldIsNotRedacted() = uiTest(
        content = {
            col {
                autoCompleteTextField { debugName = "city" }
            }
        }
    ) {
        setValue("city", "Denver")
        assertValue("city", "Denver")
    }

    @Test
    fun formattedTextInputPasswordIsRedacted() = uiTest(
        content = {
            col {
                formattedTextInput {
                    debugName = "pwFormatted"
                    keyboardHints = KeyboardHints.password
                    format(isRawData = { true }, formatter = { it })
                }
            }
        }
    ) {
        setValue("pwFormatted", "p4ssw0rd")
        val snap = snapshot("pwFormatted")
        assertTrue(!snap.contains("p4ssw0rd"), "FormattedTextInput password plaintext must never reach the snapshot: $snap")
        assertTrue(snap.contains("•".repeat("p4ssw0rd".length)), "Should redact to matching bullet count: $snap")
    }

    @Test
    fun formattedTextInputPlainFieldIsNotRedacted() = uiTest(
        content = {
            col {
                formattedTextInput {
                    debugName = "phone"
                    format(isRawData = { it.isDigit() }, formatter = { it })
                }
            }
        }
    ) {
        setValue("phone", "5551234")
        assertValue("phone", "5551234")
    }

    @Test
    fun redactedLengthMatchesActualValueLength() = uiTest(
        content = {
            col {
                textInput {
                    debugName = "pw"
                    keyboardHints = KeyboardHints.password
                }
            }
        }
    ) {
        // Bullet count must track the real value's length exactly - too few would suggest
        // truncation, too many would suggest padding, either could leak information.
        setValue("pw", "1234567890")
        assertValue("pw", "•".repeat(10))
    }

    @Test
    fun emptyPasswordValueRedactsToEmptyString() = uiTest(
        content = {
            col {
                textInput {
                    debugName = "pw"
                    keyboardHints = KeyboardHints.password
                }
            }
        }
    ) {
        // No value set yet - content.value is "", so redaction should yield "" too, not
        // some placeholder that would falsely imply a non-empty password exists.
        assertValue("pw", "")
    }
}
