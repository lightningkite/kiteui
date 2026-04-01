package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.CornerRadii.Fixed
import com.lightningkite.kiteui.models.ImageRaw
import com.lightningkite.kiteui.models.ImageRemote
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.Semantic
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.themed
import com.lightningkite.mppexampleapp.Resources
import com.lightningkite.reactive.core.*

@Routable("image-test")
object ImageTestPage : Page {
    override val title: Reactive<String>
        get() = super.title


    data object ImageSemantic : Semantic("imageSemantic") {
        override fun default(theme: Theme): ThemeAndBack = theme.copy(
            id = "imageSemantic",
            cornerRadii = Fixed(5.rem),
        ).withBackNoPadding
    }



    override fun ElementWriter.CanAddTheme.render(): Unit = run {
            scrolling.sizeConstraints(width = 40.rem).col {

                text("scaleType = ${ImageScaleType.Crop}")

                themed(ImageSemantic).centered.sizeConstraints(
                    width = 6.rem,
                    height = 6.rem
                ).image {
                    source = Resources.imagesSnowyBackground
                    scaleType = ImageScaleType.Crop
                }

                text(" Tests scaleType = ${ImageScaleType.Stretch}")
                themed(ImageSemantic).image {
                    source = Resources.imagesSnowyBackground
                    scaleType = ImageScaleType.Stretch
                }

                text("Resource .GIF from Resource")
                val gif = rememberSuspending {
                    ImageRaw(Resources.imagesGifTest())
                }
                centered.sizeConstraints(
                    width = 20.rem,
                    height = 20.rem
                ).image {
                    ::source{ gif() }
                }

                text("Remote gif ")
                centered.sizeConstraints(width = 20.rem,height=20.rem).image{
                    source = ImageRemote("https://upload.wikimedia.org/wikipedia/commons/2/2c/Rotating_earth_%28large%29.gif")
                }
            }
        }
}