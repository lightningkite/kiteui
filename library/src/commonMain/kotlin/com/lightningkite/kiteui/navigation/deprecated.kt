package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.direct.space
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive


@Deprecated("Renamed to PageNavigator", ReplaceWith("PageNavigator")) typealias ScreenNavigator = PageNavigator

@Deprecated("Move to using 'Page'", ReplaceWith("Page", "com.lightningkite.kiteui.navigation.Page"))
interface Screen: Page {
    override fun ViewWriter.render(): Unit {
        @Suppress("DEPRECATION")
        frame { render() }
//        return this.lastWrittenView ?: throw IllegalStateException("Screens must create a single view, but you have not created one.")
    }
    fun ViewWriter.renderOld(): Any?

    @Suppress("Deprecation")
    object Empty: Screen {

        @Deprecated("Use render2", ReplaceWith("render2()"))
        override fun ViewWriter.renderOld() {
            space {}
        }
    }

    @Suppress("Deprecation")
    open class Direct(title: String = "", val render: ViewWriter.()->Unit): Screen {

        @Deprecated("Use render2", ReplaceWith("render2()"))
        override fun ViewWriter.renderOld(): Unit = this@Direct.render(this)
        override val title: Reactive<String> = Constant(title)
    }
}
@Deprecated("Renamed to pageNavigator", ReplaceWith("pageNavigator")) var ViewWriter.screenNavigator by ViewWriter::pageNavigator
@Deprecated("Renamed to mainPageNavigator", ReplaceWith("mainPageNavigator")) var ViewWriter.mainScreenNavigator by ViewWriter::mainPageNavigator
@Deprecated("Renamed to dialogPageNavigator", ReplaceWith("dialogPageNavigator")) var ViewWriter.dialogScreenNavigator by ViewWriter::dialogPageNavigator