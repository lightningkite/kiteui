package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.errorText
import com.lightningkite.kiteui.views.l2.label
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.core.remember
import com.lightningkite.reactive.extensions.nullToZero

@Routable("experiment")
object ExperimentPage : Page {
    override val title: Reactive<String>
        get() = super.title

    val squircleSemantic = object : Semantic("squircle") {
        override fun default(theme: Theme): ThemeAndBack =
            theme.copy(id = key, cornerShape = CornerShape.Continuous).withBack
    }

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

            centered.card.row {
                col {
                    text("Always")
                    shownWhen { visible() }.beforeSetup { debugName = "FIELD" }.label("asdf") {
                        sizeConstraints(width = 3.rem).fieldTheme.numberInput {
                            hint = "0"
                        }
                        errorText()
                    }
                    shownWhen { !visible() }.beforeSetup { debugName = "TEXT" }.text("something")
//                    text("Always")
                }
//                row {
//                    centered.text("Something")
//                    centered.text("-")
//                    centered.text("Free")
//                    centered.text("-")
//                    col {
//                        shownWhen { visible() }.label("asdf") {
//                            sizeConstraints(width = 3.rem).fieldTheme.numberInput {
//                                hint = "0"
//                            }
//                            errorText()
//                        }
//                        shownWhen { !visible() }.text("something")
//                    }
//                }
            }
        }
    }
}
