package com.lightningkite.kiteui.speech

import com.lightningkite.kiteui.exceptions.PlainTextException
import com.lightningkite.kiteui.locale.LanguageCode
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// The Web Speech API has no Kotlin declarations, so it's reached dynamically.

internal actual suspend fun platformSpeak(text: String, language: LanguageCode?): Unit = suspendCancellableCoroutine { continuation ->
    if (js("typeof speechSynthesis === 'undefined'") as Boolean)
        throw PlainTextException("This browser can't speak.", "Can't speak")
    val utterance: dynamic = js("new SpeechSynthesisUtterance()")
    utterance.text = text
    if (language != null) utterance.lang = language.asString
    utterance.onend = { _: dynamic -> continuation.resume(Unit) }
    utterance.onerror = { event: dynamic ->
        continuation.resumeWithException(PlainTextException("Text-to-speech failed (${event.error}).", "Can't speak"))
    }
    continuation.invokeOnCancellation { js("speechSynthesis.cancel()") }
    js("speechSynthesis").speak(utterance)
}

internal actual val platformCanListen: Boolean
    get() = js("typeof window !== 'undefined' && !!(window.SpeechRecognition || window.webkitSpeechRecognition)") as Boolean

internal actual suspend fun platformListen(language: LanguageCode?): String? = suspendCancellableCoroutine { continuation ->
    val recognition: dynamic = js("new (window.SpeechRecognition || window.webkitSpeechRecognition)()")
    if (language != null) recognition.lang = language.asString
    var heard: String? = null
    var failure: Exception? = null
    recognition.onresult = { event: dynamic ->
        heard = (event.results[0][0].transcript as String).takeIf { it.isNotBlank() }
    }
    recognition.onerror = { event: dynamic ->
        failure = when (event.error as String) {
            "no-speech" -> null
            "not-allowed", "service-not-allowed" -> PlainTextException("Allow microphone access to use speech recognition.", "Can't listen")
            "network" -> PlainTextException("Speech recognition needs an internet connection.", "Can't listen")
            else -> PlainTextException("Speech recognition failed (${event.error}).", "Can't listen")
        }
    }
    // Always fires last, after any result or error.
    recognition.onend = { _: dynamic ->
        val error = failure
        if (error != null) continuation.resumeWithException(error) else continuation.resume(heard)
    }
    continuation.invokeOnCancellation { recognition.abort() }
    recognition.start()
}
