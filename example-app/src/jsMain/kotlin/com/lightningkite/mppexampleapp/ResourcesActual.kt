package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.models.*

actual object Resources {
    actual val audioTaunt: AudioResource = AudioResource("common/audio/taunt.mp3")
    actual val fontsGoldman: Font = Font(cssFontFamilyName = "fontsGoldman", direct = FontDirect(normal = mapOf(400 to "common/fonts/goldman/normal.ttf", 700 to "common/fonts/goldman/bold.ttf"), italics = mapOf()))
    actual val fontsMontserrat: Font = Font(cssFontFamilyName = "fontsMontserrat", direct = FontDirect(normal = mapOf(400 to "common/fonts/montserrat/normal.ttf", 700 to "common/fonts/montserrat/bold.ttf"), italics = mapOf(700 to "common/fonts/montserrat/bold-italic.ttf", 400 to "common/fonts/montserrat/italic.ttf")))
    actual val fontsOpensans: Font = Font(cssFontFamilyName = "fontsOpensans", direct = FontDirect(normal = mapOf(700 to "common/fonts/opensans/OpenSans-Bold.ttf", 600 to "common/fonts/opensans/OpenSans-SemiBold.ttf", 800 to "common/fonts/opensans/OpenSans-ExtraBold.ttf", 400 to "common/fonts/opensans/OpenSans-Regular.ttf", 300 to "common/fonts/opensans/OpenSans-Light.ttf", 500 to "common/fonts/opensans/OpenSans-Medium.ttf"), italics = mapOf(700 to "common/fonts/opensans/OpenSans-BoldItalic.ttf", 300 to "common/fonts/opensans/OpenSans-LightItalic.ttf", 500 to "common/fonts/opensans/OpenSans-MediumItalic.ttf", 600 to "common/fonts/opensans/OpenSans-SemiBoldItalic.ttf", 800 to "common/fonts/opensans/OpenSans-ExtraBoldItalic.ttf", 400 to "common/fonts/opensans/OpenSans-Italic.ttf")))
    actual val fontsRoboto: Font = Font(cssFontFamilyName = "fontsRoboto", direct = FontDirect(normal = mapOf(300 to "common/fonts/roboto/light.ttf", 400 to "common/fonts/roboto/normal.ttf", 700 to "common/fonts/roboto/bold.ttf"), italics = mapOf(700 to "common/fonts/roboto/bold-italic.ttf", 300 to "common/fonts/roboto/light-Italic.ttf", 400 to "common/fonts/roboto/italic.ttf")))
    actual val imagesGraph126: ImageResource = ImageResource("common/images/graph_126.png")
    actual val imagesMammoth: ImageResource = ImageResource("common/images/Mammoth.png")
    actual val imagesSolera: ImageResource = ImageResource("common/images/solera.jpg")
    actual val videoBack: VideoResource = VideoResource("common/video/back.mp4")
}