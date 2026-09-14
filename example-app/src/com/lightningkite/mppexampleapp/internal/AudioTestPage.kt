// by Claude
package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.audio.AudioCapture
import com.lightningkite.kiteui.audio.AudioFormat
import com.lightningkite.kiteui.audio.AudioPlayback
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.card
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*

/**
 * Example page demonstrating audio capture and playback using library-voice.
 * Captures microphone audio and plays it back in real-time (loopback test).
 */
@Routable("/internal/audio-test")
object AudioTestPage : Page {
    override val title: Reactive<String> = Constant("Audio Test")

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        val format = AudioFormat.VOICE  // 24kHz mono PCM16
        val capture = AudioCapture(format)
        val playback = AudioPlayback(format)

        // Connect capture output to playback input for loopback testing
        capture.onAudioData { pcmData ->
            playback.enqueue(pcmData)
        }

        onRemove {
            capture.release()
            playback.release()
        }

        scrolling.col {
            h1 { content = "Audio Capture & Playback Test" }

            text { content = "This page demonstrates real-time audio capture and streaming playback. Press Record to start capturing audio from your microphone and play it back through your speakers (loopback test)." }

            separator()

            // Status section
            card.col {
                h2 { content = "Status" }

                row {
                    text { content = "Microphone Permission: " }
                    text {
                        ::content { if (capture.hasPermission()) "Granted" else "Not Granted" }
                    }
                }

                row {
                    text { content = "Capturing: " }
                    text {
                        ::content { if (capture.isCapturing()) "Yes" else "No" }
                    }
                }

                row {
                    text { content = "Playing: " }
                    text {
                        ::content { if (playback.isPlaying()) "Yes" else "No" }
                    }
                }

                row {
                    text { content = "Buffered Audio: " }
                    text {
                        ::content { "${playback.bufferedDurationMs()} ms" }
                    }
                }
            }

            separator()

            // Audio level meter
            card.col {
                h2 { content = "Audio Level" }
                progressBar {
                    ::ratio { capture.level() }
                }
            }

            separator()

            // Control buttons
            card.col {
                h2 { content = "Controls" }

                row {
                    button {
                        text { content = "Start Recording (Loopback)" }
                        onClick {
                            val started = capture.start()
                            if (started) {
                                playback.start()
                            }
                        }
                    }

                    button {
                        text { content = "Stop" }
                        onClick {
                            capture.stop()
                            playback.stop()
                        }
                    }
                }

                separator()

                // Volume control
                val volumeSignal = Signal(1f)
                row {
                    text { content = "Playback Volume: " }
                    slider {
                        range(0f, 1f)
                        value bind volumeSignal
                    }
                    text {
                        ::content { "${(volumeSignal() * 100).toInt()}%" }
                    }
                }
                reactiveSuspending {
                    playback.volume = volumeSignal()
                }
            }

            separator()

            // Audio format info
            card.col {
                h2 { content = "Audio Format" }
                text { content = "Sample Rate: ${format.sampleRate} Hz" }
                text { content = "Channels: ${format.channels}" }
                text { content = "Bits Per Sample: ${format.bitsPerSample}" }
                text { content = "Bytes Per Second: ${format.bytesPerSecond}" }
            }
        }
    }
}
