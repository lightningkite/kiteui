package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.ImagePaint
import com.lightningkite.kiteui.models.ImagePaintMode
import com.lightningkite.kiteui.models.ListSemantic
import com.lightningkite.kiteui.models.SelectedSemantic
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.readable.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.Recycler2
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.kiteui.views.l2.icon
import com.lightningkite.mppexampleapp.Resources
import com.lightningkite.readable.invoke
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

@Routable("experiment")
object ExperimentPage : Page {
    override val title: Readable<String>
        get() = super.title

    override fun ViewWriter.render(): ViewModifiable = run {
        col {
            text {
                setBasicHtmlContent("Log in / Sign Up")
            }
            text("Emoji test 😊")
            text("Emoji test \uD83D\uDE0A")
            text { setBasicHtmlContent("Emoji test 😊") }
            text { setBasicHtmlContent("<strong>Emoji test 😊</strong>") }
            text { setBasicHtmlContent("Emoji test \uD83D\uDE0A") }
            text { setBasicHtmlContent("<strong>Emoji test \uD83D\uDE0A</strong>") }

            sizeConstraints(height = 5.rem) - ThemeDerivation {
                it.copy("weird", background = ImagePaint(
                    source = Resources.imagesNoiseTexture,
                    overlayColor = Color.white.withAlpha(0.8f),
                    mode = ImagePaintMode.Repeating,
                ), foreground = Color.gray(0.2f)).withBack
            }.onNext - frame {
                text("Look at my fancy background text")
            }
            sizeConstraints(height = 5.rem) - ThemeDerivation {
                it.copy("weird2", background = ImagePaint(
                    source = Resources.imagesSolera,
                    overlayColor = Color.black.withAlpha(0.5f),
                    mode = ImagePaintMode.Crop,
                ), foreground = Color.white).withBack
            }.onNext - frame {
                text("Look at my fancy background text 2")
            }
            sizeConstraints(height = 5.rem) - card - frame {
                text("This one's just a card to compare rounding.")
            }
        }
    }
}
