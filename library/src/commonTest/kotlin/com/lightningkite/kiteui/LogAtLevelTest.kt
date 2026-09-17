package com.lightningkite.kiteui

import com.lightningkite.kiteui.testing.BaseUiTest
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * [Log.atLevel] - asking a log, before doing the work, whether it would record the result.
 *
 * The point is callers that pay for a message beyond formatting it: a diagnostic that has to be
 * *tracked* to exist at all, like [com.lightningkite.kiteui.views.KiteUiCss] retaining a Theme per
 * generated CSS class so it can spot an id collision. Those callers need the answer up front, and
 * it has to stay honest through the filtering wrappers and through tagging.
 */
class LogAtLevelTest : BaseUiTest() {

    private fun collecting(block: () -> Unit): List<Pair<LogLevel, String>> {
        val seen = mutableListOf<Pair<LogLevel, String>>()
        val interceptor = LogInterceptor { level, _, entries -> seen.add(level to entries.joinToString(" ")) }
        Log.interceptors.add(interceptor)
        try {
            block()
        } finally {
            Log.interceptors.remove(interceptor)
        }
        return seen
    }

    @Test
    fun anUnfilteredLogRecordsEveryLevel() = runTest {
        for (level in LogLevel.entries) {
            assertTrue(Log.atLevel(level), "an unfiltered log must accept $level")
        }
    }

    @Test
    fun aTaggedLogRecordsEveryLevel() = runTest {
        val tagged = Log.tag("atLevelTest")
        for (level in LogLevel.entries) {
            assertTrue(tagged.atLevel(level), "tagging must not narrow what a log accepts - $level")
        }
    }

    @Test
    fun infoOrAbove_dropsOnlyTheLogLevel() = runTest {
        val filtered = Log.tag("atLevelTest").infoOrAbove()
        assertFalse(filtered.atLevel(LogLevel.LOG), "infoOrAbove drops LOG")
        assertTrue(filtered.atLevel(LogLevel.INFO), "infoOrAbove keeps INFO")
        assertTrue(filtered.atLevel(LogLevel.WARN), "infoOrAbove keeps WARN")
        assertTrue(filtered.atLevel(LogLevel.ERROR), "infoOrAbove keeps ERROR")
    }

    @Test
    fun warnOrAbove_dropsLogAndInfo() = runTest {
        val filtered = Log.tag("atLevelTest").warnOrAbove()
        assertFalse(filtered.atLevel(LogLevel.LOG), "warnOrAbove drops LOG")
        assertFalse(filtered.atLevel(LogLevel.INFO), "warnOrAbove drops INFO")
        assertTrue(filtered.atLevel(LogLevel.WARN), "warnOrAbove keeps WARN")
        assertTrue(filtered.atLevel(LogLevel.ERROR), "warnOrAbove keeps ERROR")
    }

    @Test
    fun atLevelAgreesWithWhatTheLogActuallyRecords() = runTest {
        // The whole value of atLevel is that it does not drift from the filtering it describes.
        for (filtered in listOf(Log.tag("atLevelTest").infoOrAbove(), Log.tag("atLevelTest").warnOrAbove())) {
            val emitted = collecting {
                filtered.log("at-log")
                filtered.info("at-info")
                filtered.warn("at-warn")
                filtered.error("at-error")
            }.map { it.first }.toSet()
            for (level in LogLevel.entries) {
                assertEquals(
                    filtered.atLevel(level), level in emitted,
                    "atLevel($level) must match whether a $level message actually got through",
                )
            }
        }
    }

    @Test
    fun filtersCompose_soNarrowingATwiceFilteredLogStaysNarrow() = runTest {
        val narrowedThenWidened = Log.tag("atLevelTest").warnOrAbove().infoOrAbove()
        assertFalse(narrowedThenWidened.atLevel(LogLevel.LOG), "still drops LOG")
        assertFalse(
            narrowedThenWidened.atLevel(LogLevel.INFO),
            "applying infoOrAbove to a warnOrAbove log must not widen it back to INFO",
        )
        assertTrue(narrowedThenWidened.atLevel(LogLevel.WARN), "WARN still gets through")
    }

    @Test
    fun filtersCompose_inTheOtherOrderToo() = runTest {
        val doubleNarrowed = Log.tag("atLevelTest").infoOrAbove().warnOrAbove()
        assertFalse(doubleNarrowed.atLevel(LogLevel.LOG), "drops LOG")
        assertFalse(doubleNarrowed.atLevel(LogLevel.INFO), "drops INFO")
        assertTrue(doubleNarrowed.atLevel(LogLevel.WARN), "keeps WARN")
    }
}
