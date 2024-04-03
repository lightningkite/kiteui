package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.models.*

actual object Resources {
    actual val audioTaunt: AudioResource = AudioResource("common/audio/taunt.mp3")
    actual val fontsGoldman: Font = Font(cssFontFamilyName = "fontsGoldman", direct = FontDirect(normal = "common/fonts/goldman/normal.ttf", bold = "common/fonts/goldman/bold.ttf", italic = null, boldItalic = null))
    actual val fontsMontserrat: Font = Font(cssFontFamilyName = "fontsMontserrat", direct = FontDirect(normal = "common/fonts/montserrat/normal.ttf", bold = "common/fonts/montserrat/bold.ttf", italic = "common/fonts/montserrat/italic.ttf", boldItalic = "common/fonts/montserrat/bold-italic.ttf"))
    actual val fontsRoboto: Font = Font(cssFontFamilyName = "fontsRoboto", direct = FontDirect(normal = "common/fonts/roboto/normal.ttf", bold = "common/fonts/roboto/bold.ttf", italic = "common/fonts/roboto/italic.ttf", boldItalic = "common/fonts/roboto/bold-italic.ttf"))
    actual val fontsRobotoLight: Font = Font(cssFontFamilyName = "fontsRobotoLight", direct = FontDirect(normal = "common/fonts/roboto/light.ttf", bold = null, italic = null, boldItalic = null))
    actual val fontsRobotoLightItalic: Font = Font(cssFontFamilyName = "fontsRobotoLightItalic", direct = FontDirect(normal = "common/fonts/roboto/light-Italic.ttf", bold = null, italic = null, boldItalic = null))
    actual val imagesMammoth: ImageResource = ImageResource("common/images/Mammoth.png")
    actual val imagesSolera: ImageResource = ImageResource("common/images/solera.jpg")
    actual val videoBack: VideoResource = VideoResource("common/video/back.mp4")
}