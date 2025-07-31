//package com.lightningkite.kiteui.views.direct
//
//import com.lightningkite.kiteui.models.ImageLocal
//import com.lightningkite.kiteui.models.ImageRaw
//import com.lightningkite.kiteui.views.RView
//import com.lightningkite.kiteui.views.ViewDsl
//
//import platform.UIKit.UIView
//
//@Suppress("ACTUAL_WITHOUT_EXPECT")
//public actual typealias NImageCrop = UIView
//public actual class ImageCrop public actual constructor(native: NImageCrop) : RView<NImageCrop> {
//    public actual override val native: NImageCrop
//        get() = TODO("Not yet implemented")
//    public actual var source: ImageLocal?
//        get() = TODO("Not yet implemented")
//        set(value) {}
//    public actual var aspectRatio: Pair<Int, Int>?
//        get() = TODO("Not yet implemented")
//        set(value) {}
//
//    public actual suspend fun crop(): ImageRaw? {
//        TODO("Not yet implemented")
//    }
//}
//
//@ViewDsl
//public actual fun ViewWriter.imageCropActual(setup: ImageCrop.() -> Unit) {
//}