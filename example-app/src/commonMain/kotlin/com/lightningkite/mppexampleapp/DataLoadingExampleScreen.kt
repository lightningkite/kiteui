package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.card
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.expanding
import com.lightningkite.kiteui.views.minus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

@Routable("sample/data")
object DataLoadingExampleScreen : Screen {
    @Serializable data class Post(val userId: Int, val id: Int, val title: String, val body: String)

    override fun ViewWriter.render() {
        val data: Readable<List<Post>> = asyncReadable {
            delay(5000)
            val response: RequestResponse = fetch("https://jsonplaceholder.typicode.com/posts", onDownloadProgress = { complete, max -> println("$complete/$max") })
            Json.decodeFromString<List<Post>>(response.text())
        }
        col {
            h1 { content = "This example loads some data." }
            text { content = "It's also faking a lot of loading so you can see what it looks like." }
            expanding - recyclerView {
                children(data) {
                    card - col {
                        val takesTime = asyncReadable { delay(Random.nextLong(0, 5000)); "" }
                        val f = shared { takesTime() }
                        h3 { ::content { it().title + f() } }
                        text { ::content { it().body.substringBefore('\n') + f() } }
                    }
                }
            }
        }
    }
}

