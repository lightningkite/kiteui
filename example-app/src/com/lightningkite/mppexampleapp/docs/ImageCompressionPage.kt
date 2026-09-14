package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*

@Routable("docs/image-compression")
object ImageCompressionPage : DocPage {
    override val covers: List<String> = listOf("image", "compression", "compress", "resize", "quality", "ImageLocal.compressed")

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        article {
            h1("Image Compression")
            text("Pick an image, configure compression parameters, and compare original vs compressed results.")

            val selectedFile = Signal<FileReference?>(null)
            val originalSize = Signal<Long>(0L)
            val maxWidth = Signal("2048")
            val maxHeight = Signal("2048")
            val quality = Signal("0.8")
            val compressedResult = Signal<ImageRaw?>(null)
            val compressedSize = Signal<Long>(0L)
            val status = Signal("")

            button {
                text("Pick Image")
                onClick {
                    context.requestFile(listOf("image/*"))?.let { file ->
                        selectedFile.value = file
                        originalSize.value = file.bytes()
                        compressedResult.value = null
                        compressedSize.value = 0L
                        status.value = ""
                    }
                }
            }

            col {
                ::shown { selectedFile() != null }

                h2("Original")
                sizeConstraints(height = 5.rem).image {
                    scaleType = ImageScaleType.Fit
                    ::source { selectedFile()?.let(::ImageLocal) }
                }
                text { ::content { "Size: ${formatBytes(originalSize())}" } }

                separator()
                h2("Compression Settings")

                row {
                    centered.text("Max Width")
                    expanding.textInput { content bind maxWidth }
                }
                row {
                    centered.text("Max Height")
                    expanding.textInput { content bind maxHeight }
                }
                row {
                    centered.text("Quality (0.0 – 1.0)")
                    expanding.textInput { content bind quality }
                }

                button {
                    text("Compress")
                    onClick {
                        val file = selectedFile.value ?: return@onClick
                        status.value = "Compressing..."
                        compressedResult.value = null
                        compressedSize.value = 0L
                        try {
                            val w = maxWidth.value.trim().toInt()
                            val h = maxHeight.value.trim().toInt()
                            val q = quality.value.trim().toFloat()
                            val result = ImageLocal(file).compressed(w, h, q)
                            compressedResult.value = result
                            compressedSize.value = result.data.bytes()
                            status.value = ""
                        } catch (e: Exception) {
                            status.value = "Error: ${e.message}"
                        }
                    }
                }

                text {
                    ::shown { status().isNotEmpty() }
                    ::content { status() }
                }

                col {
                    ::shown { compressedResult() != null }

                    separator()
                    h2("Compressed Result")
                    sizeConstraints(height = 5.rem).image {
                        scaleType = ImageScaleType.Fit
                        ::source { compressedResult() }
                    }
                    text { ::content { "Size: ${formatBytes(compressedSize())}" } }
                    text {
                        ::content {
                            val orig = originalSize()
                            val comp = compressedSize()
                            if (orig > 0 && comp > 0) {
                                val ratio = orig.toDouble() / comp
                                "Compression ratio: ${ratio}% smaller (${(1.0 - comp.toDouble() / orig) * 100}% reduction)"
                            } else ""
                        }
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes <= 0 -> "unknown"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${bytes / 1024 / 1024} MB"
}
