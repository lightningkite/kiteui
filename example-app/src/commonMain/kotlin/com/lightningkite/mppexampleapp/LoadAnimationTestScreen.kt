package com.lightningkite.mppexampleapp

import com.lightningkite.mppexampleapp.Resources
import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

@Routable("load-animation-test")
object LoadAnimationTestScreen : Screen {
    @Serializable data class Post(val userId: Int, val id: Int, val title: String, val body: String)

    override fun ViewWriter.render() {
        col {
            val loading = LateInitProperty<String>()
            val writable = Property<String>("")
            h1 { content = "Loading animation testing" }
            expanding - scrolls - col {
                row {
                    button {
                        text("Load")
                        onClick { loading.value = "Test" }
                    }
                    button {
                        text("Unload")
                        onClick { loading.unset() }
                    }
                }
                important - button {
                    text("Do action")
                    onClick { delay(5000) }
                }
                important - button {
                    col {
                        text("Big do action")
                        text("with multiple text lines")
                        text("wow")
                    }
                    onClick { delay(5000) }
                }
                text { ::content { loading.await() } }
                subtext { ::content { loading.await() } }
                h1 { ::content { loading.await() } }
                h2 { ::content { loading.await() } }
                h3 { ::content { loading.await() } }
                h4 { ::content { loading.await() } }
                h5 { ::content { loading.await() } }
                h6 { ::content { loading.await() } }
                select { bind(writable, shared { loading.await().let(::listOf) }, { it }) }
                textField { content bind loading.withWrite {  } }
                textArea { content bind loading.withWrite {  } }
                sizedBox(SizeConstraints(height = 5.rem)) - image {
                    ::source { loading.await(); Resources.imagesSolera }
                    scaleType = ImageScaleType.Fit
                }
            }
        }
    }
}

