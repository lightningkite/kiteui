package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.viewDebugTarget
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.mppexampleapp.widgets.code

interface DocScreen: Screen {
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
    action: ViewWriter.()->Unit
) {
    card - row {
        expanding - scrollsHorizontally - code { content = codeText }
        separator()
        expanding - action()
    }
}

fun ViewWriter.article(
    setup: ContainingView.()->Unit
) {
    scrolls - stack {
        gravity(Align.Center, Align.Stretch) - sizedBox(SizeConstraints(width = 80.rem)) - col {
            setup()
        }
    }
}