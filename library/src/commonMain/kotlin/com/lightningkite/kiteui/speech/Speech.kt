package com.lightningkite.kiteui.speech

import com.lightningkite.kiteui.exceptions.PlainTextException
import com.lightningkite.kiteui.locale.LanguageCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Text-to-speech and speech recognition using each platform's own engine.
 *
 * Failures throw [PlainTextException] with a message fit to show the user.
 *
 * App setup for [listen]:
 * - Android: declare `RECORD_AUDIO`, and add `<queries>` intents for `android.speech.RecognitionService` and
 *   `android.intent.action.TTS_SERVICE` so the engines are visible on Android 11+.
 * - iOS: `NSMicrophoneUsageDescription` and `NSSpeechRecognitionUsageDescription` in Info.plist.
 */
public object Speech {
    // Engines handle one utterance at a time; on the web, stopping one would also drop everything queued behind it.
    private val speaking = Mutex()
    private val listening = Mutex()

    /**
     * Speaks [text] aloud, returning once it's finished.  Cancelling stops it.  Overlapping calls take turns.
     * [language] picks the voice; null uses the device's default.
     */
    public suspend fun speak(text: String, language: LanguageCode? = null): Unit = speaking.withLock {
        platformSpeak(text, language)
    }

    /**
     * Listens for one utterance and returns what was said, or null if nothing was heard.
     * Asks for microphone permission first.  Cancelling stops listening.
     */
    public suspend fun listen(language: LanguageCode? = null): String? = listening.withLock {
        if (!platformCanListen) throw PlainTextException("Speech recognition isn't available on this device.", "Can't listen")
        platformListen(language)
    }

    /** False where [listen] can't work, such as Firefox, desktop, or server rendering. */
    public val canListen: Boolean get() = platformCanListen
}

internal expect suspend fun platformSpeak(text: String, language: LanguageCode?)
internal expect suspend fun platformListen(language: LanguageCode?): String?
internal expect val platformCanListen: Boolean
