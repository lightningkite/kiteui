package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.QueryParameter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.signal.Property
import com.lightningkite.signal.await
import com.lightningkite.signal.bind
import com.lightningkite.signal.invoke
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.forEachUpdating
import com.lightningkite.kiteui.views.minus
import com.lightningkite.mppexampleapp.Resources

@Routable("arguments-example/{id}")
public class ArgumentsExamplePage(val id: String): Page {

    @QueryParameter
    public val toAdd = Property("")

    @QueryParameter
    public val list = Property(listOf("sample"))

    public override fun ViewWriter.render() = col {
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
                    ArgumentsExamplePage("$id-plus").also {
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