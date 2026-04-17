package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlin.time.Duration.Companion.seconds

data object SlowAnimationSemantic: Semantic("slow") {
    override fun default(theme: Theme): ThemeAndBack {
        return theme.withoutBack(transitionDuration = 1.seconds)
    }
}

@Routable("shownwhen-transitions")
object ShownWhenTransitionPage : Page {
    override val title: Reactive<String> = Constant("shownWhen Transitions")

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        val visible = Signal(true)

        val transitions = listOf(
            ScreenTransition.None to "Default behavior - space animates, no visual effect",
            ScreenTransition.Fade to "Fades in/out while space collapses",
            ScreenTransition.Push to "Slides in from right, exits to left",
            ScreenTransition.Pop to "Slides in from left, exits to right",
            ScreenTransition.PullUp to "Slides in from bottom, exits to top",
            ScreenTransition.GrowFade to "Scales up with fade",
            ScreenTransition.ShrinkFade to "Scales down with fade",
            ScreenTransition(
                name = "CustomSpin",
                entryTransform = Transformation(rotation = 90.0, scaleX = 0.5, scaleY = 0.5),
                exitTransform = Transformation(rotation = -90.0, scaleX = 0.5, scaleY = 0.5),
                fade = true,
                easing = Easing.Spring,
            ) to "rotation=90, scale=0.5, Spring easing"
        )

        themed(SlowAnimationSemantic).scrolling.col {
            h1("shownWhen Transition Demo")
            text("Toggle visibility to see different entry/exit animations layered on top of the automatic layout collapse.")

            separator()

            row {
                expanding.h2("In a Column (layout collapse + visual effect)")
                important.button {
                    text { ::content { if (visible()) "Hide All" else "Show All" } }
                    onClick { visible.value = !visible.value }
                }
            }

            sizeConstraints(height = 30.rem).row {
                expanding.col {
                    card.text("Above")
                    for ((transition, description) in transitions.take(transitions.size / 2)) {
                        shownWhen(transition = transition) { visible() }.card.col {
                            gap = 0.1.rem
                            text(transition.name)
                            subtext(description)
                        }
                    }
                    card.text("Below")
                }
                expanding.col {
                    card.text("Above")
                    for ((transition, description) in transitions.drop(transitions.size / 2)) {
                        shownWhen(transition = transition) { visible() }.card.col {
                            gap = 0.1.rem
                            text(transition.name)
                            subtext(description)
                        }
                    }
                    card.text("Below")
                }
            }

            separator()
            row {
                expanding.h2("Row")
                important.button {
                    text { ::content { if (visible()) "Hide All" else "Show All" } }
                    onClick { visible.value = !visible.value }
                }
            }

            sizeConstraints(height = 4.rem).row {
                text("Before")
                for ((transition, description) in transitions.take(transitions.size / 2)) {
                    expanding.shownWhen(transition = transition) { visible() }.card.col {
                        gap = 0.1.rem
                        text(transition.name)
                        subtext(description)
                    }
                }
                text("After")
            }
            sizeConstraints(height = 4.rem).row {
                text("Before")
                for ((transition, description) in transitions.drop(transitions.size / 2)) {
                    expanding.shownWhen(transition = transition) { visible() }.card.col {
                        gap = 0.1.rem
                        text(transition.name)
                        subtext(description)
                    }
                }
                text("After")
            }

            separator()
            row {
                expanding.h2("Frame")
                important.button {
                    text { ::content { if (visible()) "Hide All" else "Show All" } }
                    onClick { visible.value = !visible.value }
                }
            }

            sizeConstraints(height = 4.rem).row {
                for ((transition, description) in transitions.take(transitions.size / 2)) {
                    expanding.frame {
                        shownWhen(transition = transition) { visible() }.card.col {
                            gap = 0.1.rem
                            text(transition.name)
                            subtext(description)
                        }
                    }
                }
            }
            sizeConstraints(height = 4.rem).row {
                for ((transition, description) in transitions.drop(transitions.size / 2)) {
                    expanding.frame {
                        shownWhen(transition = transition) { visible() }.card.col {
                            gap = 0.1.rem
                            text(transition.name)
                            subtext(description)
                        }
                    }
                }
            }
        }
    }
}
