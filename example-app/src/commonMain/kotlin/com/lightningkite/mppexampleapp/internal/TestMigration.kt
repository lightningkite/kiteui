package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.models.CardSemantic
import com.lightningkite.kiteui.models.ImportantSemantic
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.h2
import com.lightningkite.kiteui.views.direct.row
import com.lightningkite.kiteui.views.direct.scrollingHorizontally
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.dynamicTheme
import com.lightningkite.kiteui.views.themed
import com.lightningkite.reactive.core.Signal

object TestMigration : Page {
    override fun ElementWriter.CanAddTheme.render() {
        val expanded = Signal(0)

        dynamicThemed { ThemeDerivation.None }.col name@{
            h2("Test Migration")

            themed(CardSemantic).dynamicThemed {
                if (expanded() == 0) ImportantSemantic
                else ThemeDerivation { it.withoutBack }
            }.scrollingHorizontally.row {
                text("First")

                dynamicThemed {
                    ThemeDerivation.None
                }.row {
                    text("First Scope")

                    col {
                        text("Second scope")
                    }
                }

                card.dynamicThemed {
                    var t: ThemeDerivation = ThemeDerivation.None
                    for (i in 0..10) {
                        t += ThemeDerivation { it.withoutBack }
                    }
                    t
                }.col {
                    text("Second")
                    text("Third")


                }
            }
        }
    }
}