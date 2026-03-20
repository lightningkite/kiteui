package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.space
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive

interface Page {
    val title: Reactive<String>
        get() = Constant(this::class.simpleName.toString().camelToHuman().removeSuffix(" Screen").removeSuffix(" Page"))

    fun ElementWriter.render()

    object Empty : Page {
        override fun ElementWriter.render() {
            space()
        }
    }

    open class Direct(title: String = "", val render: ElementWriter.() -> Unit) : Page {
        override fun ElementWriter.render(): Unit = this@Direct.render(this)
        override val title: Reactive<String> = Constant(title)
    }
}

private val camelRegex = Regex("([a-z])([A-Z]+)")
private fun String.camelToHuman(): String = this.replace(camelRegex) { it.groupValues[1] + " " + it.groupValues[2] }