package com.lightningkite.kiteui.speech

import com.lightningkite.kiteui.exceptions.PlainTextException
import com.lightningkite.kiteui.locale.LanguageCode
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.*
import platform.Foundation.NSLocale
import platform.Speech.*
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

private val synthesizer = AVSpeechSynthesizer()
private var finishing: Pair<AVSpeechUtterance, CancellableContinuation<Unit>>? = null

// Only didFinish matters: a cancelled utterance's continuation is already cancelled.
private val synthesizerDelegate = object : NSObject(), AVSpeechSynthesizerDelegateProtocol {
    override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didFinishSpeechUtterance: AVSpeechUtterance) {
        val (utterance, continuation) = finishing ?: return
        if (utterance == didFinishSpeechUtterance) {
            finishing = null
            continuation.resume(Unit)
        }
    }
}

internal actual suspend fun platformSpeak(text: String, language: LanguageCode?) {
    val utterance = AVSpeechUtterance(string = text)
    if (language != null) {
        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage(language.asString)
            ?: throw PlainTextException("This device can't speak ${language.asString}.", "Can't speak")
    }
    synthesizer.delegate = synthesizerDelegate
    suspendCancellableCoroutine { continuation ->
        finishing = utterance to continuation
        continuation.invokeOnCancellation { synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate) }
        synthesizer.speakUtterance(utterance)
    }
}

internal actual val platformCanListen: Boolean get() = true

@OptIn(ExperimentalForeignApi::class)
internal actual suspend fun platformListen(language: LanguageCode?): String? {
    val authorized = suspendCancellableCoroutine { continuation ->
        SFSpeechRecognizer.requestAuthorization { status -> continuation.resume(status == SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized) }
    }
    val microphone = suspendCancellableCoroutine { continuation ->
        AVAudioSession.sharedInstance().requestRecordPermission { granted -> continuation.resume(granted) }
    }
    if (!authorized || !microphone) throw PlainTextException("Allow microphone and speech recognition access to use speech recognition.", "Can't listen")

    val recognizer = (if (language == null) SFSpeechRecognizer() else SFSpeechRecognizer(locale = NSLocale(localeIdentifier = language.asString)))
    if (!recognizer.isAvailable()) throw PlainTextException("Speech recognition isn't available right now.", "Can't listen")

    val session = AVAudioSession.sharedInstance()
    session.setCategory(AVAudioSessionCategoryPlayAndRecord, withOptions = AVAudioSessionCategoryOptionDefaultToSpeaker, error = null)
    session.setActive(true, null)
    val engine = AVAudioEngine()
    val input = engine.inputNode
    val request = SFSpeechAudioBufferRecognitionRequest()
    request.shouldReportPartialResults = true
    input.installTapOnBus(0u, bufferSize = 1024u, format = input.outputFormatForBus(0u)) { buffer, _ ->
        if (buffer != null) request.appendAudioPCMBuffer(buffer)
    }
    engine.prepare()
    if (!engine.startAndReturnError(null)) {
        input.removeTapOnBus(0u)
        throw PlainTextException("Couldn't start the microphone.", "Can't listen")
    }

    val heardSoFar = MutableStateFlow<String?>(null)
    val result = CompletableDeferred<String?>()
    val task = recognizer.recognitionTaskWithRequest(request) { recognized, error ->
        if (recognized != null) {
            val text = recognized.bestTranscription.formattedString.takeIf { it.isNotBlank() }
            heardSoFar.value = text
            if (recognized.isFinal()) result.complete(text)
        } else if (error != null) {
            // 1110 is "No speech detected".
            if (error.code == 1110L) result.complete(null)
            else result.completeExceptionally(PlainTextException("Speech recognition failed: ${error.localizedDescription}", "Can't listen"))
        }
    }
    return try {
        coroutineScope {
            // iOS keeps listening until told to stop, so stop after a pause in speech (or a longer wait for any at all).
            val endOnSilence = launch {
                heardSoFar.collectLatest { heard ->
                    delay(if (heard == null) 8.seconds else 1.5.seconds)
                    request.endAudio()
                }
            }
            result.await().also { endOnSilence.cancel() }
        }
    } finally {
        task.cancel()
        engine.stop()
        input.removeTapOnBus(0u)
        session.setActive(false, withOptions = AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation, error = null)
    }
}
