package com.lightningkite.kiteui.navigation

import com.lightningkite.signal.Constant
import com.lightningkite.signal.Readable
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.space

interface Page {
    val title: Readable<String>
        get() = Constant(
            this::class.simpleName.toString().camelToHuman().removeSuffix(" Screen").removeSuffix(" Page")
        )
    fun ViewWriter.render(): ViewModifiable
    object Empty: Page {
        override fun ViewWriter.render(): ViewModifiable = space {}
    }
    open class Direct(title: String = "", val render: ViewWriter.()->ViewModifiable): Page {
        override fun ViewWriter.render(): ViewModifiable = this@Direct.render(this)
        override val title: Readable<String> = Constant(title)
    }
}

private val camelRegex = Regex("([a-z])([A-Z]+)")
private fun String.camelToHuman(): String = this.replace(camelRegex) { it.groupValues[1] + " " + it.groupValues[2] }