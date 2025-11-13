package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

@Routable("sample/websockets")
object WebSocketPage : Page {
    override fun ViewWriter.render(): Unit = run {
        val socket = remember {
            retryWebsocket("wss://socketsbay.com/wss/v2/1/demo/", 30_000L).also { use(it) }
        }
        val mostRecent = remember { socket().mostRecentMessage }
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

