package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.card
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.expanding
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Routable("sample/data")
object DataLoadingExamplePage : Page {
    @Serializable data class Post(val userId: Int, val id: Int, val title: String, val body: String)

    override fun ViewWriter.render(): Unit = run {
        val data: Reactive<List<Post>> = rememberSuspending {
            delay(5000)
            val response: RequestResponse = fetch("https://jsonplaceholder.typicode.com/posts", onDownloadProgress = { complete, max -> println("$complete/$max") })
            Json.decodeFromString<List<Post>>(response.text())
        }
        col {
            h1 { content = "This example loads some data." }
            text { content = "It's also faking a lot of loading so you can see what it looks like." }
            expanding.recyclerView {
                children(data, id = { it.id }) {
                    card.col {
                        val takesTime = rememberSuspending { delay(Random.nextLong(0, 5000)); "" }
                        val f = remember { takesTime() }
                        h3 { ::content { it().title + f() } }
                        text { ::content.invoke { it().body.substringBefore('\n') + f() } }
                    }
                }
            }
        }
    }
}

