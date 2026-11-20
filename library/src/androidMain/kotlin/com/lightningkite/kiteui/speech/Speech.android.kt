package com.lightningkite.kiteui.speech

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.content.ContextCompat
import com.lightningkite.kiteui.locale.LanguageCode
import com.lightningkite.kiteui.views.AndroidAppContext
import com.lightningkite.reactive.core.ReactiveValue
import com.lightningkite.reactive.core.Signal
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.uuid.Uuid

public actual object TTS {
    // Keyed by utterance ID, from when each is queued, including while the engine starts.
    private val outstanding = OutstandingUtterances<String>()
    public actual val isSpeaking: ReactiveValue<Boolean> get() = outstanding.isSpeaking

    // An engine is installed; it can still fail to start.
    public actual val isAvailable: Boolean
        get() = AndroidAppContext.applicationCtx.packageManager
            .queryIntentServices(Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE), 0).isNotEmpty()

    internal var engine: TextToSpeech? = null
    private var ready = false
    // Queued before the engine started.
    private val waiting = ArrayList<Pair<String, Utterance>>()
    private val main = Handler(Looper.getMainLooper())

    public actual fun warm() {
        if (engine != null) return
        ready = false
        lateinit var created: TextToSpeech
        created = TextToSpeech(AndroidAppContext.applicationCtx) { status -> main.post { started(created, status == TextToSpeech.SUCCESS) } }
        created.setOnUtteranceProgressListener(listener)
        engine = created
    }

    public actual fun queue(utterance: Utterance, onComplete: (Utterance.Result) -> Unit) {
        val id = Uuid.random().toString()
        outstanding.add(id, onComplete)
        warm()
        if (ready) speak(engine!!, id, utterance) else waiting += id to utterance
    }

    public actual fun clear() {
        waiting.clear()
        engine?.stop()
        outstanding.cancelAll()
    }

    private fun started(created: TextToSpeech, success: Boolean) {
        if (created !== engine) return
        ready = success
        if (!success) {
            created.shutdown()
            engine = null
        }
        val queued = waiting.toList()
        waiting.clear()
        for ((id, utterance) in queued) {
            if (success) speak(created, id, utterance)
            else outstanding.finish(id, Utterance.Error.TTSUnavailable)
        }
    }

    private fun speak(engine: TextToSpeech, id: String, utterance: Utterance) {
        val locale = utterance.language?.let { Locale.forLanguageTag(it.asString) } ?: Locale.getDefault()
        val language = engine.setLanguage(locale)
        if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
            outstanding.finish(id, Utterance.Error.LanguageUnavailable)
        } else if (utterance.text.length > TextToSpeech.getMaxSpeechInputLength()) {
            outstanding.finish(id, Utterance.Error.Platform("This text is too long to say; the limit is ${TextToSpeech.getMaxSpeechInputLength()} characters."))
        } else if (engine.speak(utterance.text, TextToSpeech.QUEUE_ADD, null, id) == TextToSpeech.ERROR) {
            outstanding.finish(id, Utterance.Error.Platform("Text-to-speech failed."))
        }
    }

    // Called on the engine's thread.
    private val listener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String) {}
        override fun onDone(utteranceId: String) = finish(utteranceId, Utterance.Result.Completed)
        override fun onStop(utteranceId: String, interrupted: Boolean) = finish(utteranceId, Utterance.Result.Cancelled)
        @Deprecated("Superseded by onError(String, Int), but still abstract")
        override fun onError(utteranceId: String) = onError(utteranceId, TextToSpeech.ERROR)
        override fun onError(utteranceId: String, errorCode: Int) = finish(
            utteranceId,
            if (errorCode == TextToSpeech.ERROR_NOT_INSTALLED_YET) Utterance.Error.LanguageUnavailable
            else Utterance.Error.Platform("Text-to-speech failed (error $errorCode)."),
        )
    }

    private fun finish(id: String, reason: Utterance.Result) {
        main.post { outstanding.finish(id, reason) }
    }
}

public actual object ASR {
    private val listening = Signal(false)
    public actual val isListening: ReactiveValue<Boolean> get() = listening
    public actual val isAvailable: Boolean get() = SpeechRecognizer.isRecognitionAvailable(AndroidAppContext.applicationCtx)

    private var recognizer: SpeechRecognizer? = null

    public actual suspend fun listen(language: LanguageCode?): ASRResult = withContext(Dispatchers.Main) {
        if (listening.value) return@withContext ASRError.ASRListening
        val result = CompletableDeferred<ASRResult>()
        listening.value = true
        try {
            if (!isAvailable) return@withContext ASRError.ASRUnavailable
            val context = AndroidAppContext.applicationCtx
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED ||
                    AndroidAppContext.requestPermissions(Manifest.permission.RECORD_AUDIO).accepted
            if (!granted) return@withContext ASRError.NotAllowed
            val created = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer = created
            try {
                created.setRecognitionListener(listenerFor(result))
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                if (language != null) intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.asString)
                created.startListening(intent)
                result.await()
            } finally {
                created.destroy()
                recognizer = null
            }
        } finally {
            listening.value = false
        }
    }

    public actual fun stop() {
        recognizer?.stopListening()
    }

    private fun listenerFor(result: CompletableDeferred<ASRResult>) = object : RecognitionListener {
        override fun onResults(results: Bundle) {
            val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            result.complete(if (text.isNullOrBlank()) ASRResult.Nothing else ASRResult.Transcript(text))
        }

        override fun onError(error: Int) {
            // Some recognizers report a spurious error after delivering results.
            if (result.isCompleted) return
            result.complete(failure(error))
        }

        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun failure(error: Int): ASRResult = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> ASRResult.Nothing
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> ASRError.Network
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> ASRError.NotAllowed
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> ASRError.LanguageUnavailable
        else -> ASRError.Platform("Speech recognition failed (error $error).")
    }
}
