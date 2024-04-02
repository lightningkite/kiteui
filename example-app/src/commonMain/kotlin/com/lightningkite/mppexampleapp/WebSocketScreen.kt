package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.navigation.KiteUiScreen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.card
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.launch
import com.lightningkite.kiteui.views.reactiveScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

@Routable("sample/websockets")
object WebSocketScreen : KiteUiScreen {
    override fun ViewWriter.render() {
        val socket = shared {
            retryWebsocket("wss://socketsbay.com/wss/v2/1/demo/").also { use(it) }
        }
        val mostRecent = shared { socket.await().mostRecentMessage }
        col {
            h1 { content = "WS time!" }
            text { ::content { mostRecent.await().await() ?: "Nothing yet" } }
            button {
                text("Send junk")
                onClick {
                    println("Preparing to send...")
                    socket.await().send("From KiteUI (Kotlin): ${clockMillis()}")
                    println("Sent!")
                }
            }
            button {
                text("Kill")
                onClick {
                    socket.await().close(1000, "OK")
                }
            }
            reactiveScope {
                println("mostRecent.await(): ${mostRecent.await()}")
            }
            reactiveScope {
                println("mostRecent.await().await(): ${mostRecent.await().await()}")
            }
        }
    }
}

