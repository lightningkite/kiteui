package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.readable.Property
import com.lightningkite.readable.reactiveScope
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

@Routable("docs/glass-frame")
object GlassFramePage : DocPage {
    override val covers: List<String> =
        listOf("glass frame", "GlassFrame", "glassFrame", "blur", "transparency")

    override fun ViewWriter.render(): ViewModifiable = run {
        article {
            h1("Glass Frame")
            text("GlassFrame is a frame that has transparency and blurs the views behind it. If a background is applied to it, the background should go on top so that you can use a background with alpha to tint the glass effect.")

            h2("Basic Usage")
            text("Here's a basic example of a GlassFrame with default blur strength:")
            example(
                """
                frame {
                    // Background content
                    col {
                        card - text("Background Content 1")
                        card - text("Background Content 2")
                        card - text("Background Content 3")
                    }

                    // Glass frame overlay
                    centered - sizeConstraints(width = 200.px, height = 100.px) - glassFrame {
                        centered - text("Glass Frame with Default Blur")
                    } 
                }
                """.trimIndent()
            ) {
                sizeConstraints(minHeight = 200.px) - frame {
                    // Background content
                    col {
                        card - text("Background Content 1")
                        card - text("Background Content 2")
                        card - text("Background Content 3")
                    }

                    // Glass frame overlay
                    centered - sizeConstraints(width = 200.px, height = 100.px) - glassFrame {
                        centered - text("Glass Frame with Default Blur")
                    }
                }
            }

            h2("Adjustable Blur Strength")
            text("You can adjust the blur strength to control the intensity of the blur effect:")

            val blurStrength = Property(10.0f)

            example(
                """
                val blurStrength = Property(10.0f)

                col {
                    text { 
                        reactiveScope { 
                            content = "Blur Strength: " + blurStrength().toString() 
                        } 
                    }
                    slider {
                        min = 0.0f
                        max = 20.0f
                        value bind blurStrength
                    }

                    sizeConstraints(minHeight = 200.px) - frame {
                        // Background content
                        col {
                            card - text("Background Content 1")
                            card - text("Background Content 2")
                            card - text("Background Content 3")
                        }

                        // Glass frame overlay with adjustable blur
                        centered - sizeConstraints(width = 200.px, height = 100.px) - glassFrame {
                            var myBlurStrength = 10.0
                            reactiveScope { 
                                myBlurStrength = blurStrength().toDouble()
                                this@glassFrame.blurStrength = myBlurStrength
                            }
                            centered - text("Adjustable Blur")
                        }
                    }
                }
                """.trimIndent()
            ) {
                col {
                    text { 
                        reactiveScope { 
                            content = "Blur Strength: " + blurStrength().toString() 
                        } 
                    }
                    slider {
                        min = 0.0f
                        max = 20.0f
                        value bind blurStrength
                    }

                    sizeConstraints(minHeight = 200.px) - frame {
                        // Background content
                        col {
                            card - text("Background Content 1")
                            card - text("Background Content 2")
                            card - text("Background Content 3")
                        }

                        // Glass frame overlay with adjustable blur
                        centered - sizeConstraints(width = 200.px, height = 100.px) - glassFrame {
                            var myBlurStrength = 10.0
                            reactiveScope { 
                                myBlurStrength = blurStrength().toDouble()
                                this@glassFrame.blurStrength = myBlurStrength
                            }
                            centered - text("Adjustable Blur")
                        }
                    }
                }
            }

            h2("With Background Tint")
            text("You can apply a background with alpha to tint the glass effect:")
            example(
                """
                frame {
                    // Background content
                    col {
                        card - text("Background Content 1")
                        card - text("Background Content 2")
                        card - text("Background Content 3")
                    }

                    // Glass frame with tint
                    centered - sizeConstraints(width = 200.px, height = 100.px) - themeFromLast {
                        it.copy(background = Color.blue.withAlpha(0.5f))
                    } - glassFrame {
                        centered - text("Tinted Glass")
                    }
                }
                """.trimIndent()
            ) {
                sizeConstraints(minHeight = 200.px) - frame {
                    // Background content
                    col {
                        card - text("Background Content 1")
                        card - text("Background Content 2")
                        card - text("Background Content 3")
                    }

                    // Glass frame with tint
                    centered - sizeConstraints(width = 200.px, height = 100.px) - themeFromLast {
                        it.copy(background = Color.blue.withAlpha(0.25f))
                    } - glassFrame {
                        centered - text("Tinted Glass")
                    }
                }
            }
        }
    }
}
