package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.direct.scrolling
import com.lightningkite.mppexampleapp.widgets.code

interface DocPage: Page {
    val covers: List<String>
}

//data object CodeSemantic: Semantic {
//    override val key: String = "code"
//    override fun default(theme: Theme): ThemeAndBack = theme.copy(
//        id = key,
//        font = FontAndStyle(font = systemDefaultFixedWidthFont, size = 0.75.rem)
//    ).withoutBack
//}
//val ViewWriter.code: ViewWrapper get() = CodeSemantic.onNext

fun ViewWriter.example(
    codeText: String,
    action: ViewWriter.()->ViewModifiable
): ViewModifiable {
    card.rowCollapsingToColumn(40.rem) {
        expanding.scrollingHorizontally.code { content = codeText }
        separator()
        expanding.action()
    }
}

fun ViewWriter.article(
    setup: ContainingView.()->Unit
): ViewModifiable {
    scrolling.frame {
        align(Align.Center, Align.Stretch).sizedBox(SizeConstraints(width = 80.rem)).col {
            setup()
        }
    }
}