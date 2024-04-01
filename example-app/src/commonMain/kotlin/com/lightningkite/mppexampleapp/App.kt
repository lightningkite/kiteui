package com.lightningkite.mppexampleapp

import com.lightningkite.mppexampleapp.docs.DocSearchScreen
import com.lightningkite.kiteui.contains
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.*

val appTheme = Property<Theme>(
    Theme(
        title = FontAndStyle(Resources.fontsGoldman),
        body = FontAndStyle(Resources.fontsRoboto),
        elevation = 0.dp,
        cornerRadii = CornerRadii.RatioOfSpacing(0.8f),
        spacing = 0.75.rem,
        outline = Color.gray(0.8f),
        outlineWidth = 0.px,
        foreground = Color.gray(0.8f),
        background = RadialGradient(
            stops = listOf(
                GradientStop(0f, Color.gray(0.2f)),
                GradientStop(0.4f, Color.gray(0.1f)),
                GradientStop(1f, Color.gray(0.1f)),
            ),
        ),
        navSpacing = 1.rem,
        mainContent = { card() },
        bar = { card() },
        dialog = { card() },
        card = {
            copy(background = this.background.closestColor().highlight(0.05f))
        },
        nav = {
            copy(
                background = LinearGradient(
                    stops = listOf(
                        GradientStop(0f, Color.gray(0.12f)),
                        GradientStop(1f, Color.gray(0.1f)),
                    ),
                    angle = Angle.halfTurn
                ),
            )
        }
    )
)

fun ViewWriter.app() {
    rootTheme = { appTheme() }
    appNav(AutoRoutes) {
        appName = "KiteUI Sample App"
        ::navItems {
            listOf(
                NavLink(title = { "Home" }, icon = { Icon.home }) { RootScreen },
                NavLink({ "Themes" }, { Icon.sync }) { ThemesScreen },
                NavLink({ "Navigation" }, { Icon.chevronRight }) { NavigationTestScreen },
                NavLink(title = { "Docs" }, icon = { Icon.list }) { DocSearchScreen },
            )
        }

        ::exists {
            navigator.currentScreen.await() !is UseFullScreen
        }

        actions = listOf(
            NavLink(
                title = { "Search" },
                icon = { Icon.search },
                destination = { DocSearchScreen }
            )
        )

    }
}
