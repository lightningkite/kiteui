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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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

    @Before
    fun setUp() {
        AndroidAppContext.applicationCtx = application
        textToSpeech = null
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()
    private fun <T> start(block: suspend () -> T): Deferred<T> = CoroutineScope(Dispatchers.Main).async { block() }

    private fun startEngine(): ShadowTextToSpeech {
        idle()
        val engine = shadowOf(ShadowTextToSpeech.getLastTextToSpeechInstance())
        engine.onInitListener.onInit(TextToSpeech.SUCCESS)
        idle()
        return engine
    }

    @Test
    fun speakReturnsOnceSpoken() {
        ShadowTextToSpeech.addLanguageAvailability(Locale.getDefault())
        val speaking = start { Speech.speak("Hello there") }
        val engine = startEngine()
        assertEquals("Hello there", engine.lastSpokenText)
        assertTrue(speaking.isCompleted)
        speaking.getCompleted()
    }

    @Test
    fun speakUsesTheRequestedLanguage() {
        ShadowTextToSpeech.addLanguageAvailability(Locale.forLanguageTag("fr-FR"))
        val speaking = start { Speech.speak("Bonjour", LanguageCode("fr-FR")) }
        val engine = startEngine()
        assertEquals(Locale.forLanguageTag("fr-FR"), engine.currentLanguage)
        speaking.getCompleted()
    }

    @Test
    fun speakRejectsAnUnavailableLanguage() {
        val speaking = start { Speech.speak("Hallo", LanguageCode("de-DE")) }
        val engine = startEngine()
        assertIs<PlainTextException>(speaking.getCompletionExceptionOrNull())
        assertEquals(null, engine.lastSpokenText)
    }

    @Test
    fun speakFailsWhenTheEngineDoesNot() {
        val speaking = start { Speech.speak("Hello") }
        idle()
        shadowOf(ShadowTextToSpeech.getLastTextToSpeechInstance()).onInitListener.onInit(TextToSpeech.ERROR)
        idle()
        assertIs<PlainTextException>(speaking.getCompletionExceptionOrNull())
    }

    private fun installRecognizer() {
        val service = ComponentName(application, RecognitionService::class.java)
        shadowOf(application.packageManager).addServiceIfNotPresent(service)
        shadowOf(application.packageManager).addIntentFilterForService(service, android.content.IntentFilter(RecognitionService.SERVICE_INTERFACE))
        shadowOf(application).grantPermissions(Manifest.permission.RECORD_AUDIO)
    }

    private fun recognizer() = shadowOf(ShadowSpeechRecognizer.getLatestSpeechRecognizer())

    @Test
    fun listenReturnsTheBestResult() {
        installRecognizer()
        val listening = start { Speech.listen(LanguageCode("en-US")) }
        idle()
        assertEquals("en-US", recognizer().lastRecognizerIntent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE))
        recognizer().triggerOnResults(Bundle().apply {
            putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf("turn on the lights", "turn on the light"))
        })
        idle()
        assertEquals("turn on the lights", listening.getCompleted())
        assertTrue(recognizer().isDestroyed)
    }

    @Test
    fun listenReturnsNullWhenNothingWasHeard() {
        installRecognizer()
        val listening = start { Speech.listen() }
        idle()
        recognizer().triggerOnError(SpeechRecognizer.ERROR_NO_MATCH)
        idle()
        assertNull(listening.getCompleted())
    }

    @Test
    fun listenThrowsOnRecognizerErrors() {
        installRecognizer()
        val listening = start { Speech.listen() }
        idle()
        recognizer().triggerOnError(SpeechRecognizer.ERROR_SERVER)
        idle()
        assertIs<PlainTextException>(listening.getCompletionExceptionOrNull())
    }

    @Test
    fun cancellingListenDestroysTheRecognizer() {
        installRecognizer()
        val listening = start { Speech.listen() }
        idle()
        listening.cancel()
        idle()
        assertTrue(recognizer().isDestroyed)
    }

    @Test
    fun listenIsUnavailableWithoutARecognitionService() {
        assertFalse(Speech.canListen)
        val listening = start { Speech.listen() }
        idle()
        assertIs<PlainTextException>(listening.getCompletionExceptionOrNull())
    }
}
