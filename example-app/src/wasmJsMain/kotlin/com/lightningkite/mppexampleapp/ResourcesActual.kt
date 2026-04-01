package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

actual object Resources {
    actual val fontsMontserrat: Font = Font(cssFontFamilyName = "fontsMontserrat", direct = FontDirect(normal = "common/fonts/montserrat/normal.ttf", bold = "common/fonts/montserrat/bold.ttf", italic = "common/fonts/montserrat/italic.ttf", boldItalic = "common/fonts/montserrat/bold-italic.ttf"))
    actual val imagesMammoth: ImageResource = ImageResource("common/images/Mammoth.png")
    actual val imagesSolera: ImageResource = ImageResource("common/images/solera.jpg")
}