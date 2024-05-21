package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.NContext

@Deprecated("Use ScreenStack directly instead", ReplaceWith("ScreenStack", "com.lightningkite.kiteui.navigation.ScreenStack"))
typealias KiteUiNavigator = ScreenStack
class ScreenStack(private val routesGetter: ()->Routes, dialog: ScreenStack? = null) {
    val dialog: ScreenStack = dialog ?: this
    val routes: Routes by lazy { routesGetter() }
    
    val stack: Property<List<Screen>> = Property(listOf())
    fun wrap(screen: Screen): Screen = screen
    
    val currentScreen: Readable<Screen?> get() = shared { stack().lastOrNull() }
    val canGoBack: Readable<Boolean> get() = shared { stack().size > 1 }
    
    fun navigate(screen: Screen) = navigateRaw(wrap(screen))
    fun replace(screen: Screen) = replaceRaw(wrap(screen))
    fun reset(screen: Screen) = resetRaw(wrap(screen))

    fun navigateRaw(screen: Screen) {
        stack.value += screen
    }
    fun replaceRaw(screen: Screen) {
        stack.value = stack.value.dropLast(1) + screen
    }
    fun resetRaw(screen: Screen) {
        stack.value = listOf(screen)
    }

    fun goBack(): Boolean {
        if(stack.value.size <= 1)
            return false
        stack.value = stack.value.dropLast(1)
        return true
    }

    fun goBackOrClear() {
        if (stack.value.size <= 1) {
            stack.value = listOf()
        } else {
            stack.value = stack.value.dropLast(1)
        }
    }

    fun dismiss(): Boolean {
        if(stack.value.isEmpty())
            return false
        stack.value = stack.value.dropLast(1)
        return true
    }
    fun clear() {
        stack.value = listOf()
    }
    fun isStackEmpty(): Boolean = stack.value.isEmpty()

    companion object {
        lateinit var mainRoutes: Routes
        val main = ScreenStack({ mainRoutes }, ScreenStack({ mainRoutes }))
        val dialog get() = main.dialog
    }
}

@Deprecated("Use ScreenStack.main", ReplaceWith("ScreenStack.main", "com.lightningkite.kiteui.navigation.ScreenStack"))
val PlatformNavigator get() = ScreenStack.main

expect fun ScreenStack.bindToPlatform(context: NContext)
