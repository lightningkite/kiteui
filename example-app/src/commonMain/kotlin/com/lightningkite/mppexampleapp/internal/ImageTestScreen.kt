package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.exceptions.PlainTextException
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.ImageRemote
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.field
import com.lightningkite.kiteui.views.l2.icon
import com.lightningkite.mppexampleapp.Resources
import kotlin.random.Random

@Routable("image-test")
object ImageTestScreen : Screen {
    override val title: Readable<String>
        get() = super.title

    override fun ViewWriter.render() {
        col {
            val prop = Property(Random.nextInt())
            button {
                text("Reload")
                onClick { prop.value = Random.nextInt() }
            }
            sizeConstraints(width = 20.rem, height = 20.rem) - zoomableImage() {
                ::source { ImageRemote("https://picsum.photos/seed/${prop()}/300/300") }
            }
        }
    }
}