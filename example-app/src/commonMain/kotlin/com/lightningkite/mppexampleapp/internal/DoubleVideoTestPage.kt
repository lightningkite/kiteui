package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.load
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.direct.scrolling
import com.lightningkite.kiteui.views.l2.icon
import com.lightningkite.mppexampleapp.Resources
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

@Routable("double-video-test")
object DoubleVideoTestPage : Page {
    override fun ViewWriter.render(): ViewModifiable = run {
        scrolling - col {
            h1 { content = "Double Video Test" }

            centered - sizeConstraints(width = 30.rem, height = 18.rem) - video {
                source = VideoRemote("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
                scaleType = ImageScaleType.Crop
                showControls = true
            }
            centered - sizeConstraints(width = 30.rem, height = 18.rem) - video {
                source = VideoRemote("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
                scaleType = ImageScaleType.Crop
                showControls = true
            }
            centered - button {
                text("Play test audio")
                onClick {
                    Resources.audioTaunt.load().play()
                }
            }
        }
    }
}
