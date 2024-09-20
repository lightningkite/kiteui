package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.QueryParameter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.reactive.bind
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.forEachUpdating
import com.lightningkite.kiteui.views.minus

@Routable("arguments-example/{id}")
class ArgumentsExampleScreen(val id: String): Screen {

    @QueryParameter
    val toAdd = Property("")

    @QueryParameter
    val list = Property(listOf("sample"))

    override fun ViewWriter.render() = col {
        transitionId = id
        h1 { content = "Hello world!" }
        text {
            content = "My item ID is ${id}"
            transitionId = "itemid"
        }
        text { content = "This is a demonstration of how you can use classes and properties to navigate to different views." }
        link {
            text { content = "Append '-plus'" }
            ::to label@{
                val a = toAdd()
                val b = list()
                return@label {
                    ArgumentsExampleScreen("$id-plus").also {
                        it.toAdd.value = a
                        it.list.value = b
                    }
                }
            }
        }
        h2 { content = "The list so far" }
        col {
            forEachUpdating(list) {
                text { ::content { it() } }
            }
        }
        h2 { content = "Add more" }
        textField { content bind toAdd }
        button {
            text { content = "Add" }
            onClick {
                list.value += toAdd.value
                toAdd.value = ""
            }
        }
        sizeConstraints(height = 10.rem) - image {
            source = when(id.hashCode() % 2) {
                0 -> Resources.imagesSolera
                else -> Resources.imagesMammoth
            }
            scaleType = ImageScaleType.Crop
            transitionId = "Sample"
        }
    }
}

//globalState = "test"
//text { content = globalState }
//
//var ViewContext.globalState: String by viewContextAddon("test")