package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.ExternalServices
import com.lightningkite.kiteui.FileReference
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.ImageLocal
import com.lightningkite.kiteui.models.ImageRaw
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.reactive.awaitNotNull
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

@Routable("image-crop")
object ImageCropScreen : Screen {
    override fun ViewWriter.render() {
        val image = Property<FileReference?>(null)
        val croppedImage = Property<ImageRaw?>(null)
        val imageCrop: ImageCrop

        scrolls - col {
            h1("Image Crop")
            button {
                text("Upload Image")
                onClick {
                    image.value = ExternalServices.requestFile(listOf("image/*"))
                }
            }
            centered - sizeConstraints(width = 20.rem, height = 20.rem) - imageCrop {
                aspectRatio = 1 to 1
                imageCrop = this
                reactiveScope {
                    val imageSource = ImageLocal(image.awaitNotNull())
                    source = imageSource
                }
            }
            button {
                text("Crop")
                onClick {
                    //croppedImage.value = imageCrop.crop()
                }
            }
            centered - sizeConstraints(width = 20.rem, height = 20.rem) - image {
                reactiveScope {
                    source = croppedImage.await()
                }
            }
        }
    }
}