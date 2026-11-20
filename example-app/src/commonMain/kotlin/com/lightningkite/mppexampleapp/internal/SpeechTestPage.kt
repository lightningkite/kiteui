package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.locale.LanguageCode
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.speech.ASR
import com.lightningkite.kiteui.speech.ASRError
import com.lightningkite.kiteui.speech.TTS
import com.lightningkite.kiteui.speech.ASRResult
import com.lightningkite.kiteui.speech.Utterance
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.field
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal

@Routable("/internal/speech-test")
object SpeechTestPage : Page {
    override val title: Reactive<String> = Constant("Speech Test")

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        val text = Signal("Hello from KiteUI.")
        val language = Signal("")
        val heard = Signal("")
        fun languageCode() = language.value.trim().takeIf { it.isNotEmpty() }?.let(::LanguageCode)

        scrolling.col {
            h1("Speech")
            field("Language (blank for the device default, e.g. fr-FR)") {
                textInput { content bind language }
            }
            card.col {
                h2("Text to speech")
                field("Text") {
                    textInput { content bind text }
                }

                row {
                    important.button {
                        row {
                            icon(Icon.microwave, "Warm")
                            centered.text("Warm")
                        }
                        onClick { TTS.warm() }
                    }

                    important.button {
                        row {
                            icon(Icon.queueMusic, "Queue")
                            centered.text("Queue")
                        }

                        onClick { TTS.queue(Utterance(text.value, languageCode())) }
                    }

                    button {
                        centered.text("Clear")
                        onClick { TTS.clear() }
                    }

                    expanding.space()

                    centered.activityIndicator { ::opacity { if (TTS.isSpeaking()) 1.0 else 0.0 } }
                }
            }
            card.col {
                h2("Speech recognition")
                if (ASR.isAvailable) {
                    row {
                        important.button {
                            text("Listen")
                            onClick {
                                heard.value = when (val result = ASR.listen(languageCode())) {
                                    is ASRError -> "Error ${result.message}"
                                    ASRResult.Nothing -> "(nothing heard)"
                                    is ASRResult.Transcript -> result.text
                                }
                            }
                        }
                        button {
                            text("Stop")
                            onClick { ASR.stop() }
                        }
                    }
                    text { ::content { if (ASR.isListening()) "Listening…" else heard() } }
                } else {
                    text("Speech recognition isn't available here.")
                }
            }
        }
    }
}

val Icon.Companion.microwave
    get() = Icon(
        1.5.rem,
        1.5.rem,
        0,
        -960,
        960,
        960,
        listOf("M140-160q-24 0-42-18t-18-42v-520q0-24 18-42t42-18h680q24 0 42 18t18 42v520q0 24-18 42t-42 18H140Zm0-60h440v-520H140v520Zm500 0h180v-520H640v520Zm60-400h60v-60h-60v60Zm30 170q13 0 21.5-9t8.5-21q0-13-8.5-21.5T730-510q-12 0-21 8.5t-9 21.5q0 12 9 21t21 9Zm0 170q13 0 21.5-9t8.5-21q0-13-8.5-21.5T730-340q-12 0-21 8.5t-9 21.5q0 12 9 21t21 9Zm-320-50q-22 0-38-8t-28-16q-8-5-16.5-10.5T310-370q-11 0-20.5 6.5T272-350l-43-42q12-12 33-25t48-13q22 0 38.5 8.5T377-405q11 7 18 11t15 4q11 0 20.5-6.5T448-410l43 42q-12 12-33 25t-48 13Zm0-200q-22 0-38-8t-28-16q-8-5-16.5-10.5T310-570q-11 0-20.5 6.5T272-550l-43-42q12-12 33-25t48-13q22 0 38.5 8.5T377-605q11 7 18 11t15 4q11 0 20.5-6.5T448-610l43 42q-12 12-33 25t-48 13Zm230 310v-520 520Z")
    )

val Icon.Companion.queueMusic
    get() = Icon(
        1.5.rem,
        1.5.rem,
        0,
        -960,
        960,
        960,
        listOf("M642.94-160q-47.94 0-81.44-33.56t-33.5-81.5q0-47.94 32.67-81.44Q593.33-390 640-390q15.97 0 30.48 3Q685-384 698-377v-343h182v71H758v375q0 47.5-33.56 80.75T642.94-160ZM120-320v-60h306v60H120Zm0-170v-60h473v60H120Zm0-170v-60h473v60H120Z")
    )