package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.exceptions.PlainTextException
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.signal.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.field
import com.lightningkite.kiteui.views.l2.icon
import com.lightningkite.mppexampleapp.Resources

@Routable("image-test")
public object ImageTestPage : Page {
    public override val title: Readable<String>
        get() = super.title

    public override fun ViewWriter.render(): ViewModifiable = run {
        frame {
            centered - sizeConstraints(width = 40.rem) - col {
                val value = Property(false)
                card - toggleButton {
                    checked bind value
                    text("Show")
                }
                image {
                    ::source { if(value()) Resources.imagesSolera else null }
                }
            }
        }
    }
}