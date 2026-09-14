package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.VideoRemote
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.mppexampleapp.Resources
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.coroutines.launch

@Routable("docs/video")
object VideoElementPage: DocPage {
    override val covers: List<String> = listOf("video", "Video")

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        article {
            h1("Video")
            text("You can use the video element to render video, streamed from a remote source or locally.")
            val time = Signal(0.0)
            val playing = Signal(false)
            example("""
                val time = Signal(0.0)
                val playing = Signal(false)
                video {
                    source = VideoRemote("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
                    this.time bind time
                    this.playing bind playing
                }
                """.trimIndent()) {
                frame {
                    centered.sizeConstraints(width = 12.rem, height = 12.rem).video {
                        source = VideoRemote("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
                        this.time bind time
                        this.playing bind playing
                        scaleType = ImageScaleType.Crop
                    }
                }
            }
            text("You can observe or control the current time via 'time'.")
            example("""
                col {
                    text { ::content { "Time: ${'$'}{time()}" } }
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
                    text { ::content { if(playing()) "Playing" else "Paused" } }
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
                // docs:abridged — displayed shows the bare API; live wraps it in a sizing frame and uses a local asset
                frame {
                    centered.sizeConstraints(width = 12.rem, height = 12.rem).media {
                        source = Resources.videoBack
                        this.loop = true
                        scaleType = ImageScaleType.Crop
                        launch {
                            playing set true
                        }
                    }
                }
            }


            text("Media View is the new way to play videos. Use this instead of 'Video'")
            example("""
                media {
                    source = Resources.videoBack
                    this.loop = true
                    this.showControls = true
                    scaleType = ImageScaleType.Crop
                }
                """.trimIndent()) {
                // docs:abridged — displayed shows the bare API; live wraps it in a sizing frame
                frame {
                    centered.sizeConstraints(width = 12.rem, height = 12.rem).media {
                        source = Resources.videoBack
                        this.loop = true
                        this.showControls = true
                        scaleType = ImageScaleType.Crop
                    }
                }
            }

            space(5.0)
        }
    }

}