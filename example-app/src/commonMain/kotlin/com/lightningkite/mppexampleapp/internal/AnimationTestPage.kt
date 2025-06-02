package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.fetch
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.readable.Property
import com.lightningkite.readable.await
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.readable.debounce
import com.lightningkite.readable.onRemove
import com.lightningkite.readable.shared
import kotlin.time.Duration.Companion.milliseconds

@Routable("animation-test")
object AnimationTestPage : Page {
    override fun ViewWriter.render(): ViewModifiable = scrolling - col {
        val a = Property(true)
        val s = Property(true)
        val d = Property(true)
        val f = Property(true)
        val map = mapOf("A" to a, "S" to s, "D" to d, "F" to f)
        button {
            text("Alternate")
            action = Action("Alternate", frequencyCap = null) {
                a.value = !a.value
                s.value = !s.value
                d.value = !d.value
                f.value = !f.value
            }
        }
        for((key, prop) in map) {
            onRemove(AppState.onUniversalKeyboard {
                val isMe = it.code == KeyCodes.letter(key.first())
                if(isMe) prop.value = !prop.value
                isMe
            })
        }
        onRemove(AppState.onUniversalKeyboard {
            val isMe = it.code == KeyCodes.letter('R')
            if(isMe) {
                s.value = false
                d.value = true
                f.value = true
            }
            isMe
        })
        onRemove(AppState.onUniversalKeyboard {
            val isMe = it.code == KeyCodes.letter('T')
            if(isMe) {
                d.value = !d.value
                f.value = !f.value
            }
            isMe
        })
        text {
            ::content {
                "Should see " + map.entries.joinToString { if(it.value()) it.key else "-" }
            }
        }
        sizeConstraints(height = 30.rem) - row {
            expanding - card - col {
                h2("forEachAnimated Weighted Vertical")
                expanding - col {
                    forEachAnimated(shared {
                        map.entries.mapNotNull { if(it.value()) it.key else null }
                    }.debounce(10.milliseconds), preHidingModifiers = { expanding }) {
                        card - text {
                            content = it
                            debugName = content
                        }
                    }
                }
            }
            expanding - card - col {
                h2("Weighted Vertical")
                expanding - col {
                    for((key, prop) in map) {
                        expanding - shownWhen { prop() } - card - text { content = key; debugName = content }
                    }
                }
            }
            expanding - card - col {
                h2("Vertical")
                expanding - col {
                    for((key, prop) in map) {
                        shownWhen { prop() } - card - text { content = key; debugName = content }
                    }
                }
            }
        }
        card - col {
            h2("forEachAnimated Weighted Horizontal")
            row {
                forEachAnimated(shared {
                    map.entries.mapNotNull { if(it.value()) it.key else null }
                }.debounce(10.milliseconds), preHidingModifiers = { expanding }) {
                    card - text {
                        content = it
                        debugName = content
                    }
                }
            }
        }



        card - col {
            h2("Weighted Horizontal")
            row {
                for((key, prop) in map) {
                    expanding - shownWhen { prop() } - card - text { content = key; debugName = content }
                }
            }
        }

        card - col {
            h2("Horizontal")
            row {
                for((key, prop) in map) {
                    shownWhen { prop() } - card - text { content = key; debugName = content }
                }
            }
        }
    }

    private suspend fun ViewWriter.fakeLogin(email: Property<String>) {
        fetch("fake-login/${email.await()}")
        pageNavigator.navigate(ControlsPage)
    }
}