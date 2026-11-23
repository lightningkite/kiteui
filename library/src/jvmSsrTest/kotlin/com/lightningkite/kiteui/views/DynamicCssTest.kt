package com.lightningkite.kiteui.views

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DynamicCssTest {
    @Test
    fun eachFlushWritesOnlyWhatWasAddedSinceTheLastOne() {
        val css = DynamicCss("/")
        css.add(".t-a", "color", "red")
        css.flush()
        css.add(".t-b", "color", "blue")
        css.flush()
        css.flush()

        val output = css.emit()
        assertEquals(1, Regex("""\.t-a\{""").findAll(output).count(), output)
        assertEquals(1, Regex("""\.t-b\{""").findAll(output).count(), output)
    }

    @Test
    fun aLaterValueForTheSameSelectorComesLaterSoItWins() {
        val css = DynamicCss("/")
        css.add(".t-a", "color", "red")
        css.flush()
        css.add(".t-a", "color", "blue")
        css.flush()

        val output = css.emit()
        assertTrue(output.indexOf("color:red") < output.indexOf("color:blue"), output)
    }
}
