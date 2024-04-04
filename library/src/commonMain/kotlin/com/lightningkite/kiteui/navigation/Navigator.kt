package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.reactive.*

interface KiteUiNavigator {
    val dialog: KiteUiNavigator
    val routes: Routes
    val currentScreen: Readable<KiteUiScreen?>
    val canGoBack: Readable<Boolean>
    fun navigate(screen: KiteUiScreen) = navigateRaw(wrap(screen))
    fun replace(screen: KiteUiScreen) = replaceRaw(wrap(screen))
    fun reset(screen: KiteUiScreen) = resetRaw(wrap(screen))

    fun wrap(screen: KiteUiScreen): KiteUiScreen = screen
    fun navigateRaw(screen: KiteUiScreen)
    fun replaceRaw(screen: KiteUiScreen)
    fun resetRaw(screen: KiteUiScreen)

    fun goBack(): Boolean
    fun dismiss(): Boolean
    fun clear()
    fun isStackEmpty(): Boolean
    val direction: Direction?
    enum class Direction { Back, Neutral, Forward }
}

open class LocalNavigator(val routesGetter: ()->Routes, dialog: KiteUiNavigator? = null): KiteUiNavigator {
    constructor(routes: Routes, dialog: KiteUiNavigator? = null):this({routes}, dialog)
    override val routes: Routes by lazy { routesGetter() }
    override val dialog: KiteUiNavigator = dialog ?: this
    override var direction: KiteUiNavigator.Direction? = null
        protected set
    val stack = Property<List<KiteUiScreen>>(listOf())
    override val canGoBack: Readable<Boolean>
        get() = shared { stack.await().size > 1 }
    override val currentScreen: Readable<KiteUiScreen?>
        get() = shared { stack.await().lastOrNull() }
    override fun goBack(): Boolean {
        direction = KiteUiNavigator.Direction.Back
        if(stack.value.size > 1) {
            stack.value = stack.value.dropLast(1)
            return true
        } else return false
    }
    override fun dismiss(): Boolean {
        direction = KiteUiNavigator.Direction.Back
        if(stack.value.isNotEmpty()) {
            stack.value = stack.value.dropLast(1)
            return true
        } else return false
    }
    override fun clear() {
        stack.value = listOf()
    }
    override fun navigateRaw(screen: KiteUiScreen) {
        direction = KiteUiNavigator.Direction.Forward
        stack.value = stack.value.plus(screen)
    }
    override fun replaceRaw(screen: KiteUiScreen) {
        direction = KiteUiNavigator.Direction.Neutral
        stack.value = stack.value.dropLast(1).plus(screen)
    }
    override fun resetRaw(screen: KiteUiScreen) {
        direction = KiteUiNavigator.Direction.Neutral
        stack.value = listOf(screen)
    }
    override fun isStackEmpty() = stack.value.isEmpty()
    fun saveStack(): Array<String> =
        stack.value.mapNotNull { routes.render(it)?.urlLikePath?.render() }.toTypedArray()
    fun restoreStack(navStack: Array<String>) {
        direction = KiteUiNavigator.Direction.Forward
        stack.value = navStack.map(UrlLikePath::fromUrlString).mapNotNull(routes::parse)
    }
}

expect object PlatformNavigator : KiteUiNavigator {
    override var routes: Routes
}
