package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.signal.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.mppexampleapp.Resources
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Routable("load-animation-test")
public object LoadAnimationTestPage : Page {
    @Serializable data class Post(val userId: Int, val id: Int, val title: String, val body: String)

    public override fun ViewWriter.render(): ViewModifiable = run {
        col {
            val loading = LateInitProperty<String>()
            val writable = Property<String>("")
            h1 { content = "Loading animation testing" }
            expanding - scrolling - col {
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
                h1 { ::content { loading() } }
                text { ::content { loading() } }
                card - text { ::content { loading() } }
                select { bind(writable, shared { loading().let(::listOf) }, { it }) }
                textField { content bind loading.withWrite {  } }
                textArea { content bind loading.withWrite {  } }
                sizedBox(SizeConstraints(height = 5.rem)) - image {
                    ::source { loading(); Resources.imagesSolera }
                    scaleType = ImageScaleType.Fit
                }
            }
        }
    }
}

