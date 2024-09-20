package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.launch
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.VideoRemote
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.mppexampleapp.Resources

@Routable("docs/video")
object VideoElementScreen: DocScreen {
    override val covers: List<String> = listOf("video", "Video")

    override fun ViewWriter.render() {
        article {
            h1("Video")
            text("You can use the video element to render video, streamed from a remote source or locally.")
            val time = Property(0.0)
            val playing = Property(false)
            example("""
                val time = Property(0.0)
                val playing = Property(false)
                video {
                    source = VideoRemote("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
                    this.time bind time
                    this.playing bind playing
                }
                """.trimIndent()) {
                stack {
                    centered - sizeConstraints(width = 8.rem, height = 8.rem) - video {
                        source =
                            VideoRemote("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
                        this.time bind time
                        this.playing bind playing
                        scaleType = ImageScaleType.Crop
                    }
                }
            }
            text("You can observe or control the current time via 'time'.")
            example("""
                col {
                    text { ::content { "Time: ${'$'}{time.await()}" } }
                    button { 
                        text("Restart")
                        onClick { time set 0.0 }
                    }
                }
                """.trimIndent()) {
                col {
                    text { ::content { "Time: ${time()}" } }
                    button {
                        text("Restart")
                        onClick { time set 0.0 }
                    }
                }
            }
            text("You can observe or control the playing state via 'playing'.")
            example("""
                col {
                    text { ::content { if(playing.await()) "Playing" else "Paused" } }
                    button {
                        text("Play")
                        onClick { playing set true }
                    }
                    button {
                        text("Pause")
                        onClick { playing set false }
                    }
                }
                """.trimIndent()) {
                col {
                    text { ::content { if (playing()) "Playing" else "Paused" } }
                    button {
                        text("Play")
                        onClick { playing set true }
                    }
                    button {
                        text("Pause")
                        onClick { playing set false }
                    }
                }
            }
            text("Here's a looping video")
            example("""
                video {
                    source = VideoRemote("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
                    this.time bind time
                    this.playing bind playing
                }
                """.trimIndent()) {
                stack {
                    centered - sizeConstraints(width = 8.rem, height = 8.rem) - video {
                        source = Resources.videoBack
                        this.loop = true
                        scaleType = ImageScaleType.Crop
                        launch {
                            this.playing set true
                        }
                    }
                }
            }
        }
    }

}