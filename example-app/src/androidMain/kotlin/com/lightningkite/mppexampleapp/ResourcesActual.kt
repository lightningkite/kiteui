package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.AndroidAppContext

actual object Resources {
    actual val fontsGoldman: Font = AndroidAppContext.applicationCtx.resources.getFont(R.font.fonts_goldman)
    actual val fontsMontserrat: Font = AndroidAppContext.applicationCtx.resources.getFont(R.font.fonts_montserrat)
    actual val fontsRoboto: Font = AndroidAppContext.applicationCtx.resources.getFont(R.font.fonts_roboto)
    actual val fontsRobotoLight: Font = AndroidAppContext.applicationCtx.resources.getFont(R.font.fonts_roboto_light)
    actual val fontsRobotoLightItalic: Font = AndroidAppContext.applicationCtx.resources.getFont(R.font.fonts_roboto_light_italic)
    actual val imagesMammoth: ImageResource = ImageResource(R.drawable.images_mammoth)
    actual val imagesSolera: ImageResource = ImageResource(R.drawable.images_solera)
    actual val videoBack: VideoResource = VideoResource(R.raw.video_back)
}