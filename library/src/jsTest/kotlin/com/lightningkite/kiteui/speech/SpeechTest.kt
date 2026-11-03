@file:OptIn(DelicateCoroutinesApi::class)

package com.lightningkite.kiteui.speech

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertTrue

class SpeechTest {
    @Test
    fun chromeCanListen() {
        assertTrue(Speech.canListen)
    }

    @Test
    fun speakFinishes() = GlobalScope.promise {
        val outcome = runCatching { withTimeout(10_000) { Speech.speak("Hi.") } }
        console.log("speak outcome: $outcome")
        outcome.getOrThrow()
    }
}
