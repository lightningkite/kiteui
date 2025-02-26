package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.readable.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.reactiveScope

@Routable("sample/websockets")
object WebSocketPage : Page {
    override fun ViewWriter.render(): ViewModifiable = run {
        val socket = shared {
            retryWebsocket("wss://socketsbay.com/wss/v2/1/demo/", 30_000L).also { use(it) }
        }
        val mostRecent = shared { socket().mostRecentMessage }
        col {
            h1 { content = "WS time!" }
            text { ::content { mostRecent()() ?: "Nothing yet" } }
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
                println("mostRecent.await(): ${mostRecent()}")
            }
            reactiveScope {
                println("mostRecent.await().await(): ${mostRecent()()}")
            }
        }
    }
}

