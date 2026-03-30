package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.direct.space
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive

interface Page {
    val title: Reactive<String>
        get() = Constant((this::class.simpleName ?: "").camelToHuman().removeSuffix(" Screen").removeSuffix(" Page"))

    fun ElementWriter.CanAddTheme.render()

    object Empty : Page {
        override fun ElementWriter.CanAddTheme.render() {
            space()
        }
    }

    open class Direct(title: String = "", val render: ElementWriter.CanAddScrolling.() -> Unit) : Page {
        override fun ElementWriter.CanAddTheme.render(): Unit = this@Direct.render(this)
        override val title: Reactive<String> = Constant(title)
    }
}

private val camelRegex = Regex("([a-z])([A-Z]+)")
private fun String.camelToHuman(): String = this.replace(camelRegex) { it.groupValues[1] + " " + it.groupValues[2] }