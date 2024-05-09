package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.ScreenStack
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.reactive.reactiveScope
import com.lightningkite.kiteui.viewDebugTarget
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@Suppress("UNCHECKED_CAST")
fun <T> rContextAddon(init: T): ReadWriteProperty<ViewWriter, T> = object : ReadWriteProperty<ViewWriter, T> {
    override fun getValue(thisRef: ViewWriter, property: KProperty<*>): T =
        thisRef.context.addons.getOrPut(property.name) { init } as T

    override fun setValue(thisRef: ViewWriter, property: KProperty<*>, value: T) {
        thisRef.context.addons[property.name] = value
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> rContextAddonInit(): ReadWriteProperty<ViewWriter, T> = object : ReadWriteProperty<ViewWriter, T> {
    override fun getValue(thisRef: ViewWriter, property: KProperty<*>): T =
        thisRef.context.addons.getOrPut(property.name) { throw IllegalStateException("${property.name} has not been initialized.") } as T

    override fun setValue(thisRef: ViewWriter, property: KProperty<*>, value: T) {
        thisRef.context.addons[property.name] = value
    }
}

var ViewWriter.navigator by rContextAddonInit<ScreenStack>()
var ViewWriter.popoverClosers by rContextAddonInit<ArrayList<()->Unit>>()
//var ViewContext.screenTransitions by viewContextAddon(ScreenTransitions.HorizontalSlide)

@ViewModifierDsl3 val ViewWriter.debugNext: ViewWrapper get() {
    beforeNextElementSetup {
        viewDebugTarget = this
    }
    afterNextElementSetup {
        if(viewDebugTarget == this) viewDebugTarget = null
    }
    return ViewWrapper
}

@ViewModifierDsl3 fun ViewWriter.setTheme(calculate: suspend () -> Theme?): ViewWrapper {
    beforeNextElementSetup {
        useBackground = UseBackground.Yes
        reactiveScope {
            ::themeChoice { calculate()?.let { ThemeChoice.Set(it) } }
        }
    }
    return ViewWrapper
}
@ViewModifierDsl3 fun ViewWriter.themeFromLast(calculate: (Theme)->Theme?): ViewWrapper {
    beforeNextElementSetup {
        useBackground = UseBackground.Yes
        themeChoice = ThemeChoice.Derive(calculate)
        }
    return ViewWrapper
}
@ViewModifierDsl3 fun ViewWriter.maybeThemeFromLast(calculate: (Theme)-> Theme?): ViewWrapper {
    beforeNextElementSetup {
        useBackground = UseBackground.IfChanged
        themeChoice = ThemeChoice.Derive(calculate)
    }
    return ViewWrapper
}
@ViewModifierDsl3 fun ViewWriter.tweakTheme(calculate: (Theme)->Theme?): ViewWrapper {
    beforeNextElementSetup {
        themeChoice = ThemeChoice.Derive(calculate)
    }
    return ViewWrapper
}
@ViewModifierDsl3 val ViewWriter.card: ViewWrapper get() = themeFromLast { it.card() }
@ViewModifierDsl3 val ViewWriter.dialog: ViewWrapper get() = themeFromLast { it.dialog() }
@ViewModifierDsl3 val ViewWriter.mainContent: ViewWrapper get() = maybeThemeFromLast { it.mainContent() }
@ViewModifierDsl3 val ViewWriter.fieldTheme: ViewWrapper get() = themeFromLast { it.field() }
@ViewModifierDsl3 val ViewWriter.buttonTheme: ViewWrapper get() = themeFromLast { it.button() }
@ViewModifierDsl3 val ViewWriter.hover: ViewWrapper get() = themeFromLast { it.hover() }
@ViewModifierDsl3 val ViewWriter.down: ViewWrapper get() = themeFromLast { it.down() }
@ViewModifierDsl3 val ViewWriter.selected: ViewWrapper get() = themeFromLast { it.selected() }
@ViewModifierDsl3 val ViewWriter.unselected: ViewWrapper get() = themeFromLast { it.unselected() }
@ViewModifierDsl3 val ViewWriter.disabled: ViewWrapper get() = themeFromLast { it.disabled() }
@ViewModifierDsl3 val ViewWriter.bar: ViewWrapper get() = maybeThemeFromLast { it.bar() }
@ViewModifierDsl3 val ViewWriter.nav: ViewWrapper get() = maybeThemeFromLast { it.nav() }
@ViewModifierDsl3 val ViewWriter.important: ViewWrapper get() = themeFromLast { it.important() }
@ViewModifierDsl3 val ViewWriter.critical: ViewWrapper get() = themeFromLast { it.critical() }
@ViewModifierDsl3 val ViewWriter.warning: ViewWrapper get() = themeFromLast { it.warning() }
@ViewModifierDsl3 val ViewWriter.danger: ViewWrapper get() = themeFromLast { it.danger() }
@ViewModifierDsl3 val ViewWriter.affirmative: ViewWrapper get() = themeFromLast { it.affirmative() }
@ViewModifierDsl3 val ViewWriter.compact: ViewWrapper get() = tweakTheme { it.copy(spacing = it.spacing / 2) }
@ViewModifierDsl3 val ViewWriter.bold: ViewWrapper get() = tweakTheme { it.copy(title = it.title.copy(bold = true), body = it.body.copy(bold = true)) }
@ViewModifierDsl3 val ViewWriter.italic: ViewWrapper get() = tweakTheme { it.copy(title = it.title.copy(italic = true), body = it.body.copy(italic = true)) }
@ViewModifierDsl3 val ViewWriter.allCaps: ViewWrapper get() = tweakTheme { it.copy(title = it.title.copy(allCaps = true), body = it.body.copy(allCaps = true)) }
@ViewModifierDsl3 fun ViewWriter.withSpacing(multiplier: Double): ViewWrapper = tweakTheme { it.copy(spacing = it.spacing * multiplier) }

@ViewModifierDsl3
val ViewWriter.navSpacing: ViewWrapper get() {
    beforeNextElementSetup {
        useNavSpacing = true
    }
    return ViewWrapper
}