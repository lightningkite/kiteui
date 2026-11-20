package com.lightningkite.kiteui.speech

import com.lightningkite.kiteui.locale.LanguageCode
import com.lightningkite.reactive.core.ReactiveValue
import com.lightningkite.reactive.core.Signal
import kotlinx.cinterop.ExperimentalForeignApi
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
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

public actual object TTS {
    private val outstanding = OutstandingUtterances<AVSpeechUtterance>()
    public actual val isSpeaking: ReactiveValue<Boolean> get() = outstanding.isSpeaking
    public actual val isAvailable: Boolean get() = true

    private val synthesizer = AVSpeechSynthesizer()

    private val delegate = object : NSObject(), AVSpeechSynthesizerDelegateProtocol {
        override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didFinishSpeechUtterance: AVSpeechUtterance) {
            dispatch_async(dispatch_get_main_queue()) { outstanding.finish(didFinishSpeechUtterance, Utterance.Result.Completed) }
        }
    }.also { synthesizer.delegate = it }

    public actual fun warm() {}

    public actual fun queue(utterance: Utterance, onComplete: (Utterance.Result) -> Unit) {
        val spoken = AVSpeechUtterance(string = utterance.text)
        utterance.language?.let { language ->
            spoken.voice = AVSpeechSynthesisVoice.voiceWithLanguage(language.asString)
                ?: return onComplete(Utterance.Error.LanguageUnavailable)
        }
        outstanding.add(spoken, onComplete)
        synthesizer.speakUtterance(spoken)
    }

    public actual fun clear() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        outstanding.cancelAll()
    }
}

public actual object ASR {
    private val listening = Signal(false)
    public actual val isListening: ReactiveValue<Boolean> get() = listening
    public actual val isAvailable: Boolean get() = true

    private var request: SFSpeechAudioBufferRecognitionRequest? = null

    @OptIn(ExperimentalForeignApi::class)
    public actual suspend fun listen(language: LanguageCode?): ASRResult {
        if (listening.value) return ASRError.ASRListening
        val result = CompletableDeferred<ASRResult>()
        listening.value = true
        try {
            val authorized = suspendCancellableCoroutine { continuation ->
                SFSpeechRecognizer.requestAuthorization { status -> continuation.resume(status == SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized) }
            }
            val microphone = suspendCancellableCoroutine { continuation ->
                AVAudioSession.sharedInstance().requestRecordPermission { granted -> continuation.resume(granted) }
            }
            if (!authorized || !microphone) return ASRError.NotAllowed

            val recognizer = (if (language == null) SFSpeechRecognizer() else SFSpeechRecognizer(locale = NSLocale(localeIdentifier = language.asString)))
            if (!recognizer.isAvailable()) return ASRError.ASRUnavailable

            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayAndRecord, withOptions = AVAudioSessionCategoryOptionDefaultToSpeaker, error = null)
            session.setActive(true, null)
            val engine = AVAudioEngine()
            val input = engine.inputNode
            val created = SFSpeechAudioBufferRecognitionRequest()
            created.shouldReportPartialResults = true
            input.installTapOnBus(0u, bufferSize = 1024u, format = input.outputFormatForBus(0u)) { buffer, _ ->
                if (buffer != null) created.appendAudioPCMBuffer(buffer)
            }
            engine.prepare()
            if (!engine.startAndReturnError(null)) {
                input.removeTapOnBus(0u)
                session.setActive(false, withOptions = AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation, error = null)
                return ASRError.Platform("Couldn't start the microphone.")
            }
            request = created

            val heardSoFar = MutableStateFlow<String?>(null)
            val task = recognizer.recognitionTaskWithRequest(created) { recognized, error ->
                if (recognized != null) {
                    val text = recognized.bestTranscription.formattedString.takeIf { it.isNotBlank() }
                    heardSoFar.value = text
                    if (recognized.isFinal()) result.complete(if (text == null) ASRResult.Nothing else ASRResult.Transcript(text))
                } else if (error != null) {
                    // 1110 is "No speech detected".
                    result.complete(
                        if (error.code == 1110L) ASRResult.Nothing
                        else ASRError.Platform("Speech recognition failed: ${error.localizedDescription}")
                    )
                }
            }
            try {
                return coroutineScope {
                    // iOS keeps listening until told to stop, so stop after a pause in speech (or a longer wait for any at all).
                    val endOnSilence = launch {
                        heardSoFar.collectLatest { heard ->
                            delay(if (heard == null) 8.seconds else 1.5.seconds)
                            created.endAudio()
                        }
                    }
                    result.await().also { endOnSilence.cancel() }
                }
            } finally {
                task.cancel()
                engine.stop()
                input.removeTapOnBus(0u)
                session.setActive(false, withOptions = AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation, error = null)
                request = null
            }
        } finally {
            listening.value = false
        }
    }

    public actual fun stop() {
        request?.endAudio()
    }
}
