package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

interface DocScreen: Screen {
    val covers: List<String>
}

val ViewWriter.code: ViewWrapper get() = themeFromLast {
    it.copy(body = FontAndStyle(font = systemDefaultFixedWidthFont))
}

fun ViewWriter.example(
    codeText: String,
    action: ViewWriter.()->Unit
) {
    row {
        expanding - scrollsHorizontally - code - text(codeText)
        separator()
        expanding - action()
    }
}

fun ViewWriter.article(
    setup: ContainingView.()->Unit
) {
    stack {
        gravity(Align.Center, Align.Stretch) - sizedBox(SizeConstraints(width = 80.rem)) - scrolls - col {
            setup()
        }
    }
}