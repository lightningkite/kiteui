package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.Easing
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.models.Transformation
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal

@Routable("experiment")
object ExperimentPage : Page {
    override val title: Reactive<String>
        get() = super.title

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        col {

            val visible = Signal(true)

            row {
                expanding.h2("In a Column (layout collapse + visual effect)")
                important.button {
                    text { ::content { if (visible()) "Hide All" else "Show All" } }
                    onClick { visible.value = !visible.value }
                }
            }

            centered.sizeConstraints(height = 5.rem, width = 10.rem).card.frame {
                val p =  ScreenTransition(
                    "PushFade",
                    fade = true,
                    easing = Easing.Spring,
                    entryTransform = Transformation(translationX = 1.0),
                    exitTransform = Transformation(translationX = -1.0),
                )
                centered.shownWhen(transition = p) { visible() }.card.text("Hello")
            }
        }
    }
}
