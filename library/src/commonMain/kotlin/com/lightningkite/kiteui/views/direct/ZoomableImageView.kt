package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.ImageSource
import com.lightningkite.kiteui.models.UrlCacheStrategy
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RView
import kotlin.jvm.JvmInline
import kotlin.contracts.*


expect class ZoomableImageView(context: RContext) : RView {

    var source: ImageSource?
    var scaleType: ImageScaleType
    var description: String?
    var refreshOnParamChange: Boolean
    var useLoadingSpinners: Boolean
}