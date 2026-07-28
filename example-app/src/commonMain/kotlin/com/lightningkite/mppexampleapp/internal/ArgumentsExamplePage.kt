package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.QueryParameter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.direct.textInput
import com.lightningkite.kiteui.views.forEachUpdating
import com.lightningkite.mppexampleapp.Resources
import com.lightningkite.reactive.core.*
import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class IdWrapper(val id: String)

@Routable("arguments-example/{id}/{id2}")
class ArgumentsExamplePage(val id: String, val id2: IdWrapper = IdWrapper(id)): Page {

    @QueryParameter
    val toAdd = Signal("")

    @QueryParameter
    val list = Signal(listOf("sample"))

    override fun ElementWriter.CanAddTheme.render() {
        col {
            transitionId = id
            h1 { content = "Hello world!" }
            text {
                content = "My item ID is ${id}"
                transitionId = "itemid"
            }
            text {
                content =
                    "This is a demonstration of how you can use classes and properties to navigate to different views."
            }
            link {
                text { content = "Append '-plus'" }
                ::to label@{
                    val a = toAdd()
                    val b = list()
                    return@label {
                        ArgumentsExamplePage("$id-plus").also {
                            it.toAdd.value = a
                            it.list.value = b
                        }
                    }
                }
            }
            h2 { content = "The list so far" }
            colOf(list) {
                text { ::content { it() } }
            }
            h2 { content = "Add more" }
            textInput { content bind toAdd }
            button {
                text { content = "Add" }
                onClick {
                    list.value += toAdd.value
                    toAdd.value = ""
                }
            }
            sizeConstraints(height = 10.rem).image {
                source = when (this.hashCode() % 2) {
                    0 -> Resources.imagesSnowyBackground
                    else -> Resources.imagesLightningBackground
                }
                scaleType = ImageScaleType.Crop
                transitionId = "Sample"
            }
        }
    }
}

//globalState = "test"
//text { content = globalState }
//
//var ViewContext.globalState: String by viewContextAddon("test")