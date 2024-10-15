package com.lightningkite.kiteui

import com.lightningkite.kiteui.utils.formatUSPhoneNumber
import kotlin.test.Test
import kotlin.test.assertEquals

class PhoneNumberFormatTest {
    val tests = listOf(
        "1" to "(1",
        "123" to "(123",
        "1234" to "(123) 4",
        "123456" to "(123) 456",
        "1234567" to "(123) 456-7",
        "1234567890" to "(123) 456-7890",
        "123456789012345" to "(123) 456-7890",
        "1a2b3c4d" to "(123) 4",
        "abcd" to "",
        "" to "",
    )

    @Test fun runTestCases() {
        for ((input, output) in tests) {
            assertEquals(input.formatUSPhoneNumber(), output)
        }
    }
}