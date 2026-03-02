package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.requestFile
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.random.Random

@Routable("docs/image")
object ImageElementPage: DocPage {
    override val covers: List<String> = listOf("image", "Image")

    override fun ViewWriter.render(): Unit = run {
        article {
            h1("Image")
            text("You can use the image element to render many types of images with fairly smooth animations.")
            val currentImage = Signal<ImageSource>(ImageRemote("https://picsum.photos/seed/starter/640/480"))
            example("""
                val currentImage = Signal<ImageSource>(ImageRemote("https://picsum.photos/seed/starter/640/480"))
                col {
                    sizeConstraints(height = 10.rem).image {
                        scaleType = ImageScaleType.Crop
                        ::source { currentImage.await() }
                    }
                    row {
                        expanding.button {
                            text("Random")
                            onClick {
                                currentImage.value =
                                    ImageRemote("https://picsum.photos/seed/${Random.nextInt()}/640/480")
                            }
                        }
                        expanding.button {
                            text("Pick")
                            onClick {
                                externalServices.requestFile(listOf("image/*"))
                                    ?.let(::ImageLocal)
                                    ?.let { currentImage.value = it }
                            }
                        }
                    }
                }
                """.trimIndent()) {
                col {
                    sizeConstraints(height = 10.rem).image {
                        scaleType = ImageScaleType.Crop
                        ::source { currentImage() }
                    }
                    row {
                        expanding.button {
                            text("Random")
                            onClick {
                                currentImage.value =
                                    ImageRemote("https://picsum.photos/seed/${Random.nextInt()}/640/480")
                            }
                        }
                        expanding.button {
                            text("Pick")
                            onClick {
                                context.requestFile(listOf("image/*"))
                                    ?.let(::ImageLocal)
                                    ?.let { currentImage.value = it }
                            }
                        }
                    }
                }
            }
            text("Images that load near-instantly don't animate their load upon creation.")
            example("""
                image {
                    source = Icon.person.toImageSource(Color.red)
                }
            """.trimIndent()
            ) {
                image {
                    source = Icon.person.toImageSource(Color.red)
                }
            }
            text("Images will naturally size if you don't force their size somehow.")
            example("""
                row {
                    fun showSize(size: Int) {
                        centered.image { source = ImageRemote("https://picsum.photos/seed/1/${'$'}size/${'$'}size") }
                    }
                    showSize(40)
                    showSize(60)
                    showSize(80)
                }
            """.trimIndent()) {
                row {
                    fun showSize(size: Int) {
                        centered.image { source = ImageRemote("https://picsum.photos/seed/1/$size/$size") }
                    }
                    showSize(40)
                    showSize(60)
                    showSize(80)
                }
            }
            text("You can also control the crop modes.  Note that images do NOT get padding when they are given a new theme.")
            row {
                fun sample(scaleType: ImageScaleType) {
                    important.sizeConstraints(height = 5.rem).image {
                        this.scaleType = scaleType
                        source = ImageRemote("https://picsum.photos/seed/1/200/200")
                    }
                }
                ImageScaleType.values().forEach {
                    expanding.col {
                        sample(it)
                        centered.text(it.name)
                    }
                }
            }
        }
    }

}