package com.lightningkite.rock.views

import com.lightningkite.rock.ViewWrapper
import com.lightningkite.rock.models.*
import com.lightningkite.rock.navigation.DummyRockNavigator
import com.lightningkite.rock.navigation.RockNavigator
import com.lightningkite.rock.reactive.PersistentProperty
import com.lightningkite.rock.reactive.ReactiveScope
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@Suppress("UNCHECKED_CAST")
fun <T> viewContextAddon(init: T): ReadWriteProperty<ViewContext, T> = object : ReadWriteProperty<ViewContext, T> {
    override fun getValue(thisRef: ViewContext, property: KProperty<*>): T =
        thisRef.addons.getOrPut(property.name) { init } as T

    override fun setValue(thisRef: ViewContext, property: KProperty<*>, value: T) {
        thisRef.addons[property.name] = value
    }
}

var ViewContext.navigator by viewContextAddon<RockNavigator>(DummyRockNavigator())
var ViewContext.screenTransitions by viewContextAddon(ScreenTransitions.HorizontalSlide)

var ViewContext.themeStack by viewContextAddon(listOf<ReactiveScope.() -> Theme>())
@ViewModifierDsl3 inline fun ViewContext.withTheme(theme: Theme, action: () -> Unit) {
    val old = themeStack
    themeStack += { theme }
    try {
        action()
    } finally {
        themeStack = old
    }
}
@ViewModifierDsl3 expect fun ViewContext.setTheme(calculate: ReactiveScope.()->Theme): ViewWrapper
@ViewModifierDsl3 inline fun ViewContext.themeFromLast(crossinline calculate: (Theme)->Theme): ViewWrapper {
    val previous = themeStack.last()
    return setTheme { calculate(previous()) }
}
@ViewModifierDsl3 val ViewContext.card: ViewWrapper get() = themeFromLast { it }
@ViewModifierDsl3 val ViewContext.hover: ViewWrapper get() = themeFromLast { it.hover() }
@ViewModifierDsl3 val ViewContext.down: ViewWrapper get() = themeFromLast { it.down() }
@ViewModifierDsl3 val ViewContext.selected: ViewWrapper get() = themeFromLast { it.selected() }
@ViewModifierDsl3 val ViewContext.disabled: ViewWrapper get() = themeFromLast { it.disabled() }
@ViewModifierDsl3 val ViewContext.important: ViewWrapper get() = themeFromLast { it.important() }
@ViewModifierDsl3 val ViewContext.critical: ViewWrapper get() = themeFromLast { it.critical() }
@ViewModifierDsl3 val ViewContext.warning: ViewWrapper get() = themeFromLast { it.warning() }
@ViewModifierDsl3 val ViewContext.danger: ViewWrapper get() = themeFromLast { it.danger() }
