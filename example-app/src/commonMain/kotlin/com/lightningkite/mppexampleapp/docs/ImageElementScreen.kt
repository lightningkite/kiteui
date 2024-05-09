package com.lightningkite.mppexampleapp.docs

import ViewWriter
import com.lightningkite.kiteui.ExternalServices
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import kotlin.random.Random

@Routable("docs/image")
object ImageElementScreen: DocScreen {
    override val covers: List<String> = listOf("image", "Image")

    override fun ViewWriter.render() {
        article {
            h1("Image")
            text("You can use the image element to render many types of images with fairly smooth animations.")
            val currentImage = Property<ImageSource>(ImageRemote("https://picsum.photos/seed/starter/640/480"))
            example("""
                val currentImage = Property<ImageSource>(ImageRemote("https://picsum.photos/seed/starter/640/480"))
                col {
                    sizeConstraints(height = 10.rem) - image {
                        scaleType = ImageScaleType.Crop
                        ::source { currentImage.await() }
                    }
                    row {
                        expanding - button {
                            text("Random")
                            onClick {
                                currentImage.value =
                                    ImageRemote("https://picsum.photos/seed/${Random.nextInt()}/640/480")
                            }
                        }
                        expanding - button {
                            text("Pick")
                            onClick {
                                ExternalServices.requestFile(listOf("image/*")) {
                                    if(it != null) currentImage.value = ImageLocal(it)
                                }
                            }
                        }
                    }
                }
                """.trimIndent()) {
                col {
                    expanding - image {
                        scaleType = ImageScaleType.Crop
                        ::source { currentImage.await() }
                    }
                    row {
                        expanding - button {
                            text("Random")
                            onClick {
                                currentImage.value =
                                    ImageRemote("https://picsum.photos/seed/${Random.nextInt()}/640/480")
                            }
                        }
                        expanding - button {
                            text("Pick")
                            onClick {
                                ExternalServices.requestFile(listOf("image/*"))
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
                        centered - image { source = ImageRemote("https://picsum.photos/seed/1/${'$'}size/${'$'}size") }
                    }
                    showSize(40)
                    showSize(60)
                    showSize(80)
                }
            """.trimIndent()) {
                row {
                    fun showSize(size: Int) {
                        centered - image { source = ImageRemote("https://picsum.photos/seed/1/$size/$size") }
                    }
                    showSize(40)
                    showSize(60)
                    showSize(80)
                }
            }
            text("You can also control the crop modes.  Note that images do NOT get padding when they are given a new theme.")
            row {
                fun sample(scaleType: ImageScaleType) {
                    important - sizeConstraints(height = 5.rem) - image {
                        this.scaleType = scaleType
                        source = ImageRemote("https://picsum.photos/seed/1/200/200")
                    }
                }
                ImageScaleType.values().forEach {
                    expanding - col {
                        sample(it)
                        centered - text(it.name)
                    }
                }
            }
        }
    }

}