package com.lightningkite.kiteui.speech

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import com.lightningkite.kiteui.exceptions.PlainTextException
import com.lightningkite.kiteui.locale.LanguageCode
import com.lightningkite.kiteui.views.AndroidAppContext
import com.lightningkite.reactive.core.Reactive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowSpeechRecognizer
import org.robolectric.shadows.ShadowTextToSpeech
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class SpeechTest {
    private val application get() = RuntimeEnvironment.getApplication()

    private val reported = ArrayList<Throwable>()
    private val originalReport = Reactive.reportException

    @Before
    fun setUp() {
        AndroidAppContext.applicationCtx = application
        TTS.clear()
        TTS.engine = null
        Reactive.reportException = { reported += it }
    }

    @After
    fun tearDown() {
        Reactive.reportException = originalReport
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()
    private fun <T> start(block: suspend () -> T): Deferred<T> = CoroutineScope(Dispatchers.Main).async { block() }
    private val isSpeaking get() = TTS.isSpeaking.value

    private fun startEngineWith(begin: () -> Unit): ShadowTextToSpeech {
        begin()
        return startEngine()
    }

    private fun startEngine(status: Int = TextToSpeech.SUCCESS): ShadowTextToSpeech {
        idle()
        val engine = shadowOf(ShadowTextToSpeech.getLastTextToSpeechInstance())
        engine.onInitListener.onInit(status)
        idle()
        return engine
    }

    @Test
    fun queuedUtterancesAreSpokenOnceTheEngineStarts() {
        ShadowTextToSpeech.addLanguageAvailability(Locale.getDefault())
        TTS.queue(Utterance("First"))
        TTS.queue(Utterance("Second"))
        assertTrue(isSpeaking)
        val engine = startEngine()
        assertEquals(listOf("First", "Second"), engine.spokenTextList)
        assertEquals(TextToSpeech.QUEUE_ADD, engine.queueMode)
        assertFalse(isSpeaking)
        assertEquals(emptyList(), reported)
    }

    @Test
    fun queueUsesTheRequestedLanguage() {
        ShadowTextToSpeech.addLanguageAvailability(Locale.forLanguageTag("fr-FR"))
        TTS.queue(Utterance("Bonjour", LanguageCode("fr-FR")))
        val engine = startEngine()
        assertEquals(Locale.forLanguageTag("fr-FR"), engine.currentLanguage)
        assertEquals("Bonjour", engine.lastSpokenText)
    }

    @Test
    fun spokenUtterancesCompleteAsCompleted() {
        ShadowTextToSpeech.addLanguageAvailability(Locale.getDefault())
        val completions = ArrayList<Utterance.Result>()
        TTS.queue(Utterance("First")) { completions += it }
        TTS.queue(Utterance("Second")) { completions += it }
        startEngine()
        assertEquals(listOf<Utterance.Result>(Utterance.Result.Completed, Utterance.Result.Completed), completions)
    }

    @Test
    fun clearCompletesEverythingAsCancelled() {
        ShadowTextToSpeech.addLanguageAvailability(Locale.getDefault())
        val completions = ArrayList<Pair<String, Utterance.Result>>()
        TTS.queue(Utterance("Sent")) { completions += "Sent" to it }
        val engine = startEngine()
        // Not yet idle, so the engine hasn't reported "Sent" as done.
        TTS.queue(Utterance("Speaking")) { completions += "Speaking" to it }
        TTS.clear()
        idle()
        assertEquals(listOf("Sent" to Utterance.Result.Completed, "Speaking" to Utterance.Result.Cancelled), completions)
        assertTrue(engine.isStopped)
    }

    @Test
    fun clearCancelsWhatWasWaitingForTheEngine() {
        val completions = ArrayList<Utterance.Result>()
        TTS.queue(Utterance("Hello")) { completions += it }
        TTS.clear()
        assertEquals(listOf<Utterance.Result>(Utterance.Result.Cancelled), completions)
        startEngine()
        assertEquals(1, completions.size)
    }

    @Test
    fun aCallbackCanQueueAfterClear() {
        ShadowTextToSpeech.addLanguageAvailability(Locale.getDefault())
        val engine = startEngineWith { TTS.warm() }
        TTS.queue(Utterance("Interrupted")) { TTS.queue(Utterance("Replacement")) }
        TTS.clear()
        idle()
        assertEquals("Replacement", engine.lastSpokenText)
    }

    @Test
    fun anUnavailableLanguageGoesToOnComplete() {
        val completions = ArrayList<Utterance.Result>()
        TTS.queue(Utterance("Hallo", LanguageCode("de-DE"))) { completions += it }
        val engine = startEngine()
        assertEquals(listOf<Utterance.Result>(Utterance.Error.LanguageUnavailable), completions)
        assertEquals(emptyList(), reported)
        assertNull(engine.lastSpokenText)
        assertFalse(isSpeaking)
    }

    @Test
    fun failuresWithoutOnErrorAreReported() {
        TTS.queue(Utterance("Hallo", LanguageCode("de-DE")))
        startEngine()
        assertIs<PlainTextException>(reported.single())
    }

    @Test
    fun anEngineThatFailsToStartFailsEveryWaitingUtterance() {
        val completions = ArrayList<Utterance.Result>()
        TTS.queue(Utterance("Hello")) { completions += it }
        TTS.queue(Utterance("There")) { completions += it }
        startEngine(TextToSpeech.ERROR)
        assertEquals(listOf<Utterance.Result>(Utterance.Error.TTSUnavailable, Utterance.Error.TTSUnavailable), completions)
        assertFalse(isSpeaking)
    }

    @Test
    fun textTooLongForTheEngineFails() {
        ShadowTextToSpeech.addLanguageAvailability(Locale.getDefault())
        val completions = ArrayList<Utterance.Result>()
        TTS.queue(Utterance("a".repeat(TextToSpeech.getMaxSpeechInputLength() + 1))) { completions += it }
        val engine = startEngine()
        assertIs<Utterance.Error.Platform>(completions.single())
        assertNull(engine.lastSpokenText)
        assertFalse(isSpeaking)
    }

    @Test
    fun warmStartsTheEngineWithoutSpeaking() {
        ShadowTextToSpeech.addLanguageAvailability(Locale.getDefault())
        TTS.warm()
        TTS.warm()
        assertFalse(isSpeaking)
        val engine = startEngine()
        assertNull(engine.lastSpokenText)
        assertFalse(isSpeaking)
        TTS.queue(Utterance("Hello"))
        idle()
        assertEquals("Hello", engine.lastSpokenText)
        assertEquals(engine, shadowOf(ShadowTextToSpeech.getLastTextToSpeechInstance()))
    }

    @Test
    fun clearStopsTheEngine() {
        ShadowTextToSpeech.addLanguageAvailability(Locale.getDefault())
        TTS.queue(Utterance("First"))
        val engine = startEngine()
        TTS.queue(Utterance("Second"))
        TTS.clear()
        idle()
        assertTrue(engine.isStopped)
        assertFalse(isSpeaking)
    }

    @Test
    fun clearDropsWhatWasWaitingForTheEngine() {
        ShadowTextToSpeech.addLanguageAvailability(Locale.getDefault())
        TTS.queue(Utterance("Hello"))
        TTS.clear()
        assertFalse(isSpeaking)
        val engine = startEngine()
        assertNull(engine.lastSpokenText)
        assertFalse(isSpeaking)
    }

    private fun installRecognizer() {
        val service = ComponentName(application, RecognitionService::class.java)
        shadowOf(application.packageManager).addServiceIfNotPresent(service)
        shadowOf(application.packageManager).addIntentFilterForService(service, android.content.IntentFilter(RecognitionService.SERVICE_INTERFACE))
        shadowOf(application).grantPermissions(Manifest.permission.RECORD_AUDIO)
    }

    private fun recognizer() = shadowOf(ShadowSpeechRecognizer.getLatestSpeechRecognizer())

    private fun results(vararg heard: String) = Bundle().apply {
        putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf(*heard))
    }

    @Test
    fun listenReturnsTheBestResult() {
        installRecognizer()
        val listening = start { ASR.listen(LanguageCode("en-US")) }
        idle()
        assertTrue(ASR.isListening.value)
        assertEquals("en-US", recognizer().lastRecognizerIntent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE))
        recognizer().triggerOnResults(results("turn on the lights", "turn on the light"))
        idle()
        assertEquals("turn on the lights", assertIs<ASRResult.Transcript>(listening.getCompleted()).text)
        assertTrue(recognizer().isDestroyed)
        assertFalse(ASR.isListening.value)
    }

    @Test
    fun listenReturnsNothingWhenNothingWasHeard() {
        installRecognizer()
        val listening = start { ASR.listen() }
        idle()
        recognizer().triggerOnError(SpeechRecognizer.ERROR_NO_MATCH)
        idle()
        assertEquals(ASRResult.Nothing, listening.getCompleted())
    }

    @Test
    fun listenReturnsRecognizerErrors() {
        installRecognizer()
        val listening = start { ASR.listen() }
        idle()
        recognizer().triggerOnError(SpeechRecognizer.ERROR_NETWORK)
        idle()
        assertEquals(ASRError.Network, listening.getCompleted())
        assertFalse(ASR.isListening.value)
    }

    @Test
    fun aDisconnectedRecognizerIsNotANetworkError() {
        installRecognizer()
        val listening = start { ASR.listen() }
        idle()
        recognizer().triggerOnError(SpeechRecognizer.ERROR_SERVER_DISCONNECTED)
        idle()
        assertIs<ASRError.Platform>(listening.getCompleted())
    }

    @Test
    fun stopKeepsWhatWasHeard() {
        installRecognizer()
        val listening = start { ASR.listen() }
        idle()
        ASR.stop()
        recognizer().triggerOnResults(results("hello"))
        idle()
        assertEquals("hello", assertIs<ASRResult.Transcript>(listening.getCompleted()).text)
    }

    @Test
    fun listeningAgainReturnsASRListening() {
        installRecognizer()
        val first = start { ASR.listen() }
        idle()
        val firstRecognizer = recognizer()
        val second = start { ASR.listen() }
        idle()
        assertEquals(ASRError.ASRListening, second.getCompleted())
        assertFalse(first.isCompleted)
        assertFalse(firstRecognizer.isDestroyed)
        assertTrue(ASR.isListening.value)
        firstRecognizer.triggerOnResults(results("first"))
        idle()
        assertEquals("first", assertIs<ASRResult.Transcript>(first.getCompleted()).text)
        assertFalse(ASR.isListening.value)
    }

    @Test
    fun cancellingListenDestroysTheRecognizer() {
        installRecognizer()
        val listening = start { ASR.listen() }
        idle()
        listening.cancel()
        idle()
        assertTrue(recognizer().isDestroyed)
        assertFalse(ASR.isListening.value)
    }

    @Test
    fun listenIsUnavailableWithoutARecognitionService() {
        assertFalse(ASR.isAvailable)
        val listening = start { ASR.listen() }
        idle()
        assertEquals(ASRError.ASRUnavailable, listening.getCompleted())
        assertFalse(ASR.isListening.value)
    }
}
