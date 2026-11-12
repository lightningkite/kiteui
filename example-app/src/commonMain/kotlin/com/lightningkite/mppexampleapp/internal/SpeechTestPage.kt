package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.locale.LanguageCode
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.speech.Speech
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.field
import com.lightningkite.reactive.context.invoke
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
                important.button {
                    text("Speak")
                    onClick { Speech.speak(text.value, languageCode()) }
                }
            }
            card.col {
                h2("Speech recognition")
                if (Speech.canListen) {
                    important.button {
                        text("Listen")
                        onClick { heard.value = Speech.listen(languageCode()) ?: "(nothing heard)" }
                    }
                    text { ::content { heard() } }
                } else {
                    text("Speech recognition isn't available here.")
                }
            }
        }
    }
}
