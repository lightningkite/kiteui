package com.lightningkite.kiteui.speech

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.content.ContextCompat
import com.lightningkite.kiteui.exceptions.PlainTextException
import com.lightningkite.kiteui.locale.LanguageCode
import com.lightningkite.kiteui.views.AndroidAppContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.uuid.Uuid

internal var textToSpeech: TextToSpeech? = null

private suspend fun textToSpeech(): TextToSpeech = textToSpeech ?: withContext(Dispatchers.Main) {
    val ready = CompletableDeferred<Int>()
    val created = TextToSpeech(AndroidAppContext.applicationCtx) { status -> ready.complete(status) }
    if (ready.await() != TextToSpeech.SUCCESS) {
        created.shutdown()
        throw PlainTextException("Text-to-speech isn't available on this device.", "Can't speak")
    }
    textToSpeech = created
    created
}

internal actual suspend fun platformSpeak(text: String, language: LanguageCode?) {
    val engine = textToSpeech()
    val locale = language?.let { Locale.forLanguageTag(it.asString) } ?: Locale.getDefault()
    when (engine.setLanguage(locale)) {
        TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED ->
            throw PlainTextException("This device can't speak ${locale.displayName}.", "Can't speak")
    }
    val id = Uuid.random().toString()
    suspendCancellableCoroutine { continuation ->
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {}
            override fun onDone(utteranceId: String) {
                if (utteranceId == id) continuation.resume(Unit)
            }

            @Deprecated("Superseded by onError(String, Int), but still abstract")
            override fun onError(utteranceId: String) = onError(utteranceId, TextToSpeech.ERROR)
            override fun onError(utteranceId: String, errorCode: Int) {
                if (utteranceId == id) continuation.resumeWithException(PlainTextException("Text-to-speech failed (error $errorCode).", "Can't speak"))
            }
        })
        continuation.invokeOnCancellation { engine.stop() }
        engine.speak(text, TextToSpeech.QUEUE_ADD, null, id)
    }
}

internal actual val platformCanListen: Boolean get() = SpeechRecognizer.isRecognitionAvailable(AndroidAppContext.applicationCtx)

internal actual suspend fun platformListen(language: LanguageCode?): String? {
    val context = AndroidAppContext.applicationCtx
    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED ||
            AndroidAppContext.requestPermissions(Manifest.permission.RECORD_AUDIO).accepted
    if (!granted) throw PlainTextException("Allow microphone access to use speech recognition.", "Can't listen")
    return withContext(Dispatchers.Main) {
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        try {
            suspendCancellableCoroutine { continuation ->
                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onResults(results: Bundle) {
                        val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        continuation.resume(text?.takeIf { it.isNotBlank() })
                    }

                    override fun onError(error: Int) {
                        // Some recognizers report a spurious error after delivering results.
                        if (!continuation.isActive) return
                        when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> continuation.resume(null)
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                                continuation.resumeWithException(PlainTextException("Speech recognition needs an internet connection.", "Can't listen"))
                            else -> continuation.resumeWithException(PlainTextException("Speech recognition failed (error $error).", "Can't listen"))
                        }
                    }

                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                if (language != null) intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.asString)
                recognizer.startListening(intent)
            }
        } finally {
            recognizer.destroy()
        }
    }
}
