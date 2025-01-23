package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.reactive.Constant
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.space

@Deprecated("Use Screen directly instead", ReplaceWith("Screen", "com.lightningkite.kiteui.navigation.Screen"))
@Suppress("Deprecation")
typealias KiteUiScreen = Screen

interface Page {
    val title: Readable<String>
        get() = Constant(
            this::class.simpleName.toString().camelToHuman().removeSuffix(" Screen").removeSuffix(" Page")
        )
    fun ViewWriter.render2(): ViewModifiable
    object Empty: Page {
        override fun ViewWriter.render2(): ViewModifiable = space {}
    }
    open class Direct(title: String = "", val render: ViewWriter.()->ViewModifiable): Page {
        override fun ViewWriter.render2(): ViewModifiable = this@Direct.render(this)
        override val title: Readable<String> = Constant(title)
    }
}
@Deprecated("Move to using 'Page'", ReplaceWith("Page", "com.lightningkite.kiteui.navigation.Page"))
interface Screen: Page {
    override fun ViewWriter.render2(): ViewModifiable {
        @Suppress("DEPRECATION")
        render()
        return this.lastWrittenView ?: throw IllegalStateException("Screens must create a single view, but you have not created one.")
    }
    @Deprecated("Use render2", ReplaceWith("render2()"))
    fun ViewWriter.render(): Any?

    @Suppress("Deprecation")
    object Empty: Screen {

        @Deprecated("Use render2", ReplaceWith("render2()"))
        override fun ViewWriter.render(): ViewModifiable = space {}
    }

    @Suppress("Deprecation")
    open class Direct(title: String = "", val render: ViewWriter.()->ViewModifiable): Screen {

        @Deprecated("Use render2", ReplaceWith("render2()"))
        override fun ViewWriter.render(): ViewModifiable = this@Direct.render(this)
        override val title: Readable<String> = Constant(title)
    }
}
private val camelRegex = Regex("([a-z])([A-Z]+)")
private fun String.camelToHuman(): String = this.replace(camelRegex) { it.groupValues[1] + " " + it.groupValues[2] }