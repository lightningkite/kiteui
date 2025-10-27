package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.mppexampleapp.Resources
import com.lightningkite.reactive.core.*

@Routable("image-test")
object ImageTestPage : Page {
    override val title: Reactive<String>
        get() = super.title

    override fun ViewWriter.render(): ViewModifiable = run {
            scrolling - sizeConstraints(width = 40.rem) - col {

                text("scaleType = ${ImageScaleType.Crop}")

                 centered -  sizeConstraints(
                    width = 6.rem,
                    height = 6.rem
                ) - image {
                    source = Resources.imagesSnowyBackground
                    scaleType = ImageScaleType.Crop
                }

                text("scaleType = ${ImageScaleType.Stretch}")
                centered -  sizeConstraints(
                    width = 6.rem,
                    height = 6.rem
                ) - image {
                    source = Resources.imagesSnowyBackground
                    scaleType = ImageScaleType.Stretch
                }

            }
        }
}