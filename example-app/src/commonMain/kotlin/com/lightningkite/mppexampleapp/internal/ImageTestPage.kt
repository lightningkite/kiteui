package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.mppexampleapp.Resources
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

@Routable("image-test")
public object ImageTestPage : Page {
    public override val title: Reactive<String>
        get() = super.title

    public override fun ViewWriter.render(): ViewModifiable = run {
        frame {
            centered - sizeConstraints(width = 40.rem) - col {
                val value = Signal(false)
                card - toggleButton {
                    checked bind value
                    text("Show")
                }
                image {
                    ::source { if(value()) Resources.imagesSnowyBackground else null }
                }
            }
        }
    }
}