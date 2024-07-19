package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.mppexampleapp.docs.DocSearchScreen
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.ScreenNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.*
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

fun brandBasedExperimental(id: String, normalBack: Color): Theme {
    val startingBack = normalBack.highlight(0.05f)
    val dominant = Color.fromHex(0x002b3f).toHSP()
    val critical = Color.fromHex(0xffcc33).toHSP()
    val secondary = Color.fromHex(0xa2a2a2)
    val tertiary1 = Color.fromHex(0x5f5f5f).also {
        if (normalBack.perceivedBrightness < 0.5) it.toHSP().let { it.copy(brightness = 1f - it.brightness) }
            .toRGB() else it
    }
    val tertiary2 = Color.fromHex(0x04A7CD).also {
        if (normalBack.perceivedBrightness < 0.5) it.toHSP().let { it.copy(brightness = 1f - it.brightness) }
            .toRGB() else it
    }
    val tertiary3 = Color.fromHex(0x97d0e2).also {
        if (normalBack.perceivedBrightness < 0.5) it.toHSP().let { it.copy(brightness = 1f - it.brightness) }
            .toRGB() else it
    }
    val tertiary4 = Color.fromHex(0x008EBC).also {
        if (normalBack.perceivedBrightness < 0.5) it.toHSP().let { it.copy(brightness = 1f - it.brightness) }
            .toRGB() else it
    }
//    0x04A7CD
//    0x008EBC
    val assurance = if (normalBack.perceivedBrightness > 0.6f) Color.fromHex(0x04A7CD) else Color.fromHex(0x008EBC)
    val importanceRotation = listOf(tertiary3, normalBack, tertiary2, tertiary1, tertiary4).map { it.toHSP() }
    infix fun HSPColor.distanceTo(other: HSPColor): Float {
        return (this.hue - other.hue).absoluteValue.turns + (this.saturation - other.saturation).absoluteValue + (this.brightness - other.brightness).absoluteValue
    }

    fun Paint.outermostColor(): Color = when (this) {
        is Color -> this
        is LinearGradient -> this.stops.first().color
        is RadialGradient -> this.stops.last().color
    }

    fun Paint.rotateImportance(): Paint {
        val hsp = outermostColor().toHSP()
        if (hsp.saturation > 0.5f) return hsp.toRGB().highlight(0.1f)
        return importanceRotation[
            importanceRotation.indices.minBy { importanceRotation[it] distanceTo hsp }.plus(1) % importanceRotation.size
        ].toRGB()
    }

    fun Paint.rotateBack(): Paint {
        val hsp = outermostColor().toHSP()
        if (hsp.saturation > 0.5f) return outermostColor().highlight(0.1f)
        return when {
            hsp.brightness < 0.99f -> normalBack
            else -> Color.gray(0.95f)
        }
    }

    fun Paint.deriveForeground(): Paint {
        val hsp = outermostColor().toHSP()
        return if (hsp.brightness > 0.6f) {
            if (dominant.brightness < 0.2f)
                dominant.toRGB()
            else
                Color.black
        } else {
            if (dominant.brightness > 0.8f)
                dominant.toRGB()
            else
                Color.white
        }
    }

    fun Color.withStripe() = LinearGradient(
        stops = listOf(
//            0.46f..0.48f,
//            0.5f..0.6f,
//            0.62f..0.64f,
//            0.66f..0.67f,
            0.7f..0.8f
        ).flatMap {
            listOf(
                GradientStop(it.start, this),
                GradientStop(it.start + 0.0001f, Color.interpolate(this, assurance, 0.25f)),
                GradientStop(it.endInclusive - 0.0001f, Color.interpolate(this, assurance, 0.25f)),
                GradientStop(it.endInclusive, this),
            )
        }.let {
            listOf(GradientStop(0f, this)) + it + listOf(GradientStop(1f, this))
        },
//        stops = listOf(
//            GradientStop(0f, this),
//            GradientStop(0.5f, this),
//            GradientStop(0.501f, Color.interpolate(this, assurance, 0.5f)),
//            GradientStop(0.599f, Color.interpolate(this, assurance, 0.5f)),
//            GradientStop(0.6f, this),
//            GradientStop(1f, this),
//        ),
        angle = 0.degrees,
        screenStatic = true
    )
    return Theme(
        id = id,
//        title = FontAndStyle(Resources.blinker),
//        body = FontAndStyle(Resources.opensans),
        background = startingBack.withStripe(),
        elevation = 0.dp,
        transitionDuration = 0.25.seconds,
        cornerRadii = CornerRadii.RatioOfSpacing(0.8f),
        spacing = 0.75.rem,
        outlineWidth = 0.px,
        foreground = startingBack.deriveForeground(),
        unselected = {
            val b = background.outermostColor().toHSP()
            if (b.saturation > 0.5 && (b.brightness - 0.5f).absoluteValue < 0.2f)
                copy(
                    id = "uns",
                    background = startingBack.highlight(0.1f),
                    foreground = startingBack.highlight(0.1f).deriveForeground(),
                )
            else this
        },
        selected = { important() },
        field = {
            copy(
                id = "fld",
                outline = background.outermostColor().highlight(0.3f),
                outlineWidth = 0.1.rem,
                background = background.outermostColor()
            )
        },
        focus = {
            copy(
                id = "fcs",
                outline = important().background,
                outlineWidth = 0.3.rem,
            )
        },
        bar = {
            val newBack = dominant.toRGB()
            copy(
                id = "bar",
                background = newBack,
                foreground = newBack.deriveForeground()
            )
        },
        important = {
            val newBack = background.rotateImportance()
            copy(
                id = "imp",
                background = newBack,
                foreground = newBack.deriveForeground()
            )
        },
        critical = {
            val newBack = critical.toRGB()
            copy(
                id = "crt",
                background = newBack,
                foreground = newBack.deriveForeground()
            )
        },
        card = {
            val newBack = background.rotateBack()
            copy(
                id = "crd",
                background = if (background !is Color) newBack.applyAlpha(0.87f) else newBack,
//                backdropFilters = if(background !is Color) listOf(BackdropFilter.Blur(1.rem)) else listOf(),
                foreground = newBack.deriveForeground()
            )
        },
        dialog = {
            copy(
                id = "dlg",
                background = background.outermostColor()
            )
        },
//        danger = {
//            copy(
//                background = Color.fromHex(0xFFB00020.toInt()),
//                outline = Color.fromHex(0xFFB00020.toInt()).highlight(0.1f),
//                foreground = Color.white
//            )
//        },
//        affirmative = {
//            copy(
//                background = Color.fromHex(0xFF20a020.toInt()),
//                outline = Color.fromHex(0xFF20a020.toInt()).highlight(0.1f),
//                foreground = Color.white
//            )
//        },
    )/*.let {
        it.customize(
            newId = it.id + "-start",
            background = startingBack.withStripe(),
            revert = true
        )
    }*/
}
val defaultTheme = brandBasedExperimental("bsa", normalBack = Color.white)
//val defaultTheme = Theme.flat("default", Angle(0.55f))// brandBasedExperimental("bsa", normalBack = Color.white)
val appTheme = Property<Theme>(defaultTheme)

fun ViewWriter.app(navigator: ScreenNavigator, dialog: ScreenNavigator) {
//    rootTheme = { appTheme() }
    appNav(navigator, dialog) {
        appName = "KiteUI Sample App"
        ::navItems {
            listOf(
                NavLink(title = { "Home" }, icon = { Icon.home }) { { RootScreen } },
                NavLink({ "Themes" }, { Icon.sync }) { { ThemesScreen } },
                NavLink({ "Navigation" }, { Icon.chevronRight }) { { NavigationTestScreen } },
                NavLink(title = { "Docs" }, icon = { Icon.list }) { { DocSearchScreen } },
            )
        }

        ::exists {
            navigator.currentScreen.await() !is UseFullScreen
        }

        actions = listOf(
            NavLink(
                title = { "Search" },
                icon = { Icon.search },
                destination = { { DocSearchScreen } }
            )
        )
    }
}
