@file:OptIn(DelicateCoroutinesApi::class)

package com.lightningkite.kiteui.speech

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertTrue

class SpeechTest {
    @Test
    fun chromeCanListen() {
        assertTrue(ASR.isAvailable)
    }

    @Test
    fun queuedSpeechFinishes() = GlobalScope.promise {
        TTS.queue(Utterance("Hi."))
        assertTrue(TTS.isSpeaking.value)
        withTimeout(10_000) {
            while (TTS.isSpeaking.value) delay(50)
        }
    }
}
