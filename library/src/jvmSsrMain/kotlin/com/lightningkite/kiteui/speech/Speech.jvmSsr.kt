package com.lightningkite.kiteui.speech

import com.lightningkite.kiteui.locale.LanguageCode
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.ReactiveValue

// Nothing is spoken during server-side rendering; event handlers that would queue speech don't run there.
// Queued utterances are reported as cancelled rather than failed, so the default callback doesn't log an error.
public actual object TTS {
    public actual val isAvailable: Boolean get() = false
    public actual val isSpeaking: ReactiveValue<Boolean> = Constant(false)
    public actual fun queue(utterance: Utterance, onComplete: (Utterance.Result) -> Unit) {
        onComplete(Utterance.Result.Cancelled)
    }
    public actual fun clear() {}
    public actual fun warm() {}
}

public actual object ASR {
    public actual val isAvailable: Boolean get() = false
    public actual val isListening: ReactiveValue<Boolean> = Constant(false)
    public actual suspend fun listen(language: LanguageCode?): ASRResult = ASRError.ASRUnavailable
    public actual fun stop() {}
}
