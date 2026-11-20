package com.lightningkite.kiteui.speech

import com.lightningkite.kiteui.locale.LanguageCode
import com.lightningkite.reactive.core.ReactiveValue
import com.lightningkite.reactive.core.Signal
import kotlinx.coroutines.CompletableDeferred

// The Web Speech API has no Kotlin declarations, so it's reached dynamically.
public actual object TTS {
    // Also keeps Chrome from garbage-collecting an utterance mid-speech and never firing its events.
    private val outstanding = OutstandingUtterances<dynamic>()
    public actual val isSpeaking: ReactiveValue<Boolean> get() = outstanding.isSpeaking
    public actual val isAvailable: Boolean get() = synthesis != null

    private val synthesis: dynamic get() = js("globalThis.speechSynthesis")

    public actual fun warm() {}

    public actual fun queue(utterance: Utterance, onComplete: (Utterance.Result) -> Unit) {
        val synthesis = synthesis
        if (synthesis == null) return onComplete(Utterance.Error.TTSUnavailable)
        val spoken: dynamic = js("new SpeechSynthesisUtterance()")
        spoken.text = utterance.text
        utterance.language?.let { spoken.lang = it.asString }
        spoken.onend = { _: dynamic -> outstanding.finish(spoken, Utterance.Result.Completed) }
        spoken.onerror = { event: dynamic ->
            val error = event.error as String
            outstanding.finish(spoken, when (error) {
                "canceled", "interrupted" -> Utterance.Result.Cancelled
                "synthesis-unavailable" -> Utterance.Error.TTSUnavailable
                "language-unavailable", "voice-unavailable" -> Utterance.Error.LanguageUnavailable
                else -> Utterance.Error.Platform("Text-to-speech failed ($error).")
            })
        }
        outstanding.add(spoken, onComplete)
        synthesis.speak(spoken)
    }

    public actual fun clear() {
        synthesis?.cancel()
        outstanding.cancelAll()
    }
}

public actual object ASR {
    private val listening = Signal(false)
    public actual val isListening: ReactiveValue<Boolean> get() = listening
    public actual val isAvailable: Boolean
        get() = js("typeof window !== 'undefined' && !!(window.SpeechRecognition || window.webkitSpeechRecognition)") as Boolean

    private var recognition: dynamic = null

    public actual suspend fun listen(language: LanguageCode?): ASRResult {
        if (listening.value) return ASRError.ASRListening
        if (!isAvailable) return ASRError.ASRUnavailable
        val result = CompletableDeferred<ASRResult>()
        val created: dynamic = js("new (window.SpeechRecognition || window.webkitSpeechRecognition)()")
        recognition = created
        listening.value = true
        if (language != null) created.lang = language.asString
        var heard: String? = null
        var failure: ASRError? = null
        created.onresult = { event: dynamic ->
            heard = (event.results[0][0].transcript as String).takeIf { it.isNotBlank() }
        }
        created.onerror = { event: dynamic -> failure = listenFailure(event.error as String) }
        // Always fires last, after any result or error.
        created.onend = { _: dynamic ->
            val error = failure
            result.complete(error ?: heard?.let { ASRResult.Transcript(it) } ?: ASRResult.Nothing)
        }
        try {
            try {
                created.start()
            } catch (e: Throwable) {
                return ASRError.Platform("Speech recognition failed: ${e.message}")
            }
            return result.await()
        } finally {
            created.abort()
            recognition = null
            listening.value = false
        }
    }

    public actual fun stop() {
        recognition?.stop()
    }

    // Null when nothing was heard, or for "aborted", which only follows our own abort() after the call has ended.
    private fun listenFailure(code: String): ASRError? = when (code) {
        "no-speech", "aborted" -> null
        "not-allowed", "service-not-allowed" -> ASRError.NotAllowed
        "network" -> ASRError.Network
        "language-not-supported" -> ASRError.LanguageUnavailable
        "audio-capture" -> ASRError.Platform("No microphone was found.")
        else -> ASRError.Platform("Speech recognition failed ($code).")
    }
}
