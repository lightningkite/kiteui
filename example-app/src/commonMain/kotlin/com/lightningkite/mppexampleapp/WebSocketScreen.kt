package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.reactiveScope

@Routable("sample/websockets")
object WebSocketScreen : Screen {
    override fun ViewWriter.render() {
        val socket = shared {
            retryWebsocket("wss://socketsbay.com/wss/v2/1/demo/", 30_000L).also { use(it) }
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

