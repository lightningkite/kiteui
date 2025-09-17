package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.mppexampleapp.Resources
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

//actual fun ViewWriter.platformSpecific(): ViewModifiable {
//    return col {
//        text("Lookie here at this icon:")
//        val visible = Signal(false)
//        switch { checked bind visible }
//        expanding - zoomableImage {
//            source = Resources.imagesSnowyBackground
//        }
//    }
//}