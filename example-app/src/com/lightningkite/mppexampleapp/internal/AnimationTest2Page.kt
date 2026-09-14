package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.fetch
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.navigation.mainPageNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.time.Duration.Companion.milliseconds

@Routable("animation-test2")
object AnimationTest2Page : Page {
    override fun ElementWriter.CanAddTheme.render(): Unit {
        scrolling.col {
            val a = Signal(true)
            val s = Signal(true)
            val d = Signal(true)
            val f = Signal(true)
            val map = mapOf("A" to a, "S" to s, "D" to d, "F" to f)
            val items = remember {
                map.entries.mapNotNull { if (it.value()) it.key else null }
            }.debounce(10.milliseconds)
            button {
                text("Alternate")
                action = Action("Alternate", frequencyCap = null) {
                    a.value = !a.value
                    s.value = !s.value
                    d.value = !d.value
                    f.value = !f.value
                }
            }
            for ((key, prop) in map) {
                onRemove(AppState.onUniversalKeyboard {
                    val isMe = it.code == KeyCodes.letter(key.first())
                    if (isMe) prop.value = !prop.value
                    isMe
                })
            }
            onRemove(AppState.onUniversalKeyboard {
                val isMe = it.code == KeyCodes.letter('R')
                if (isMe) {
                    s.value = false
                    d.value = true
                    f.value = true
                }
                isMe
            })
            onRemove(AppState.onUniversalKeyboard {
                val isMe = it.code == KeyCodes.letter('T')
                if (isMe) {
                    d.value = !d.value
                    f.value = !f.value
                }
                isMe
            })
            text {
                ::content {
                    "Should see " + map.entries.joinToString { if (it.value()) it.key else "-" }
                }
            }
            expanding.col {
                map.forEach {
                    shownWhen { it.value() }.card.button {
                        text {
                            content = it.key
                            debugName = content
                        }
                    }
                }
                expanding.space()
            }

            text("Below")
        }
    }

    private suspend fun ViewWriter.fakeLogin(email: Signal<String>) {
        fetch("fake-login/${email.await()}")
        context.pageNavigator.navigate(ControlsPage)
    }
}