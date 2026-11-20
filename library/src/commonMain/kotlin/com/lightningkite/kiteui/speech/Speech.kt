package com.lightningkite.kiteui.speech

import com.lightningkite.kiteui.exceptions.PlainTextException
import com.lightningkite.kiteui.locale.LanguageCode
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.ReactiveValue
import com.lightningkite.reactive.core.Signal

/**
 * @property text What [TTS] will say
 * @property language The language `TTS` will use for this `Utterance`.
 *      When set to `null`, the device's default language wil be used.
 */
public data class Utterance(
    public val text: String,
    public val language: LanguageCode? = null,
) {
    public sealed interface Result {
        public object Completed : Result
        public object Cancelled : Result
    }

    public sealed interface Error : Result {
        public val message: String

        public object TTSUnavailable : Error {
            override val message: String = "Text-to-speech isn't available on this device."
        }

        public object LanguageUnavailable : Error {
            override val message: String = "No voice is installed for this language."
        }

        public data class Platform(override val message: String) : Error
    }
}

/**
 * The utterances [TTS] still owes a [Utterance.Result], in queue order, each identified by a platform [K]ey.
 * Call from the main thread.
 */
internal class OutstandingUtterances<K> {
    private val entries = ArrayList<Pair<K, (Utterance.Result) -> Unit>>()
    private val speaking = Signal(false)

    /** True while anything is outstanding. */
    val isSpeaking: ReactiveValue<Boolean> get() = speaking

    /** The oldest outstanding key. */
    val first: K? get() = entries.firstOrNull()?.first

    fun add(key: K, onComplete: (Utterance.Result) -> Unit) {
        entries += key to onComplete
        speaking.value = true
    }

    /** Reports [reason] for [key], unless it's already been reported or [cancelAll]ed. */
    fun finish(key: K, reason: Utterance.Result) {
        val index = entries.indexOfFirst { it.first == key }
        if (index == -1) return
        val onComplete = entries.removeAt(index).second
        speaking.value = entries.isNotEmpty()
        onComplete(reason)
    }

    /** Reports everything as [Utterance.Result.Cancelled].  Anything added from those callbacks is kept. */
    fun cancelAll() {
        val dropped = entries.map { it.second }
        entries.clear()
        speaking.value = false
        dropped.forEach { it(Utterance.Result.Cancelled) }
    }
}

/**
 * Text-to-speech using each platform's own engine.
 *
 * @see Utterance
 */
public expect object TTS {
    public val isAvailable: Boolean
    public val isSpeaking: ReactiveValue<Boolean>

    /**
     * Warm the TTS engine to prevent a delay on the first [Utterance].
     *
     * Calling [warm] on certain platforms (or when the engine is already warm) is a no-op.
     */
    public fun warm()

    /**
     * Queues [utterance] to be uttered by the TTS engine. If nothing is currently being uttered,
     * `utterance` will be uttered immediately. May call [warm].
     */
    public fun queue(
        utterance: Utterance,
        onComplete: (Utterance.Result) -> Unit = {
            if (it is Utterance.Error) Reactive.reportException(PlainTextException(it.message))
        },
    )

    /** Stops the current [Utterance] and clears the queue. */
    public fun clear()
}

public sealed interface ASRResult {
    public data class Transcript(public val text: String) : ASRResult
    public object Nothing : ASRResult
}

public sealed interface ASRError : ASRResult {
    public val message: String

    public object ASRUnavailable : ASRError {
        override val message: String = "Speech recognition isn't available on this device."
    }

    public object ASRListening : ASRError {
        override val message: String = "Already listening."
    }

    public object LanguageUnavailable : ASRError {
        override val message: String = "Speech recognition isn't available for this language."
    }

    public object NotAllowed : ASRError {
        override val message: String = "Allow microphone and speech recognition access to listen."
    }

    public object Network : ASRError {
        override val message: String = "Speech recognition needs an internet connection."
    }

    public class Platform(override val message: String) : ASRError
}

/** Automatic speech recognition using each platform's own engine. */
public expect object ASR {
    public val isAvailable: Boolean
    public val isListening: ReactiveValue<Boolean>

    /**
     * Listens for one utterance (ending after a pause in speech) and returns what was heard, if anything.
     *
     * May ask for microphone permission.
     *
     * @param language What language to listen for. If set to `null`, ASR will pick the device's default language.
     *
     * @see [ASRResult]
     * @see stop
     */
    public suspend fun listen(language: LanguageCode? = null): ASRResult

    /**
     * Ends any existing listening session early, keeping what was heard so far.
     *
     * @see listen
     */
    public fun stop()
}
