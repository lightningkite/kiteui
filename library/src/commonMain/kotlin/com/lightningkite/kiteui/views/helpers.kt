package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.views.direct.*

@ViewModifierDsl3 val ViewWriter.centered get() = gravity(Align.Center, Align.Center)
@ViewModifierDsl3 val ViewWriter.atStart get() = gravity(Align.Start, Align.Stretch)
@ViewModifierDsl3 val ViewWriter.atEnd get() = gravity(Align.End, Align.Stretch)
@ViewModifierDsl3 val ViewWriter.atTop get() = gravity(Align.Stretch, Align.Start)
@ViewModifierDsl3 val ViewWriter.atBottom get() = gravity(Align.Stretch, Align.End)
@ViewModifierDsl3 val ViewWriter.atTopStart get() = gravity(Align.Start, Align.Start)
@ViewModifierDsl3 val ViewWriter.atBottomStart get() = gravity(Align.Start, Align.End)
@ViewModifierDsl3 val ViewWriter.atTopCenter get() = gravity(Align.Center, Align.Start)
@ViewModifierDsl3 val ViewWriter.atBottomCenter get() = gravity(Align.Center, Align.End)
@ViewModifierDsl3 val ViewWriter.atTopEnd get() = gravity(Align.End, Align.Start)
@ViewModifierDsl3 val ViewWriter.atBottomEnd get() = gravity(Align.End, Align.End)

@ViewModifierDsl3 val ViewWriter.expanding get() = weight(1f)
@ViewModifierDsl3 operator fun ViewWrapper.minus(unit: Unit) = Unit
@ViewModifierDsl3 operator fun ViewWrapper.minus(wrapper: ViewWrapper) = ViewWrapper

@ViewModifierDsl3 fun ViewWriter.maxWidthCentered(width: Dimension) = gravity(Align.Center, Align.Stretch) - sizedBox(SizeConstraints(maxWidth = width))
@ViewModifierDsl3 fun ViewWriter.maxHeight(height: Dimension) = sizedBox(SizeConstraints(maxHeight = height))

@ViewDsl
fun ViewWriter.icon(icon: suspend ()->Icon, description: String, setup: ImageView.()->Unit = {}) {
    image {
        val currentTheme = currentTheme
        ::source { icon().toImageSource(currentTheme().foreground) }
        this.description = description
        setup(this)
    }
}

@ViewModifierDsl3 inline fun ViewWriter.maybeThemeFromLast(crossinline calculate: suspend (Theme)-> Theme?): ViewWrapper {
    var x: Theme? = null
    transitionNextView = ViewWriter.TransitionNextView.Maybe { x != null }
    return themeModifier {
        val previous = it()
        val result = calculate(previous)
        x = result
        result ?: previous
    }
}

val Icon.Companion.empty get() = Icon(2.rem, 2.rem, 0, -960, 960, 960, listOf())