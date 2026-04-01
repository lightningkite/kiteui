package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.lottie.views.direct.LottieView
import com.lightningkite.kiteui.lottie.views.direct.lottie
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.*

@Routable("lottie")
object LottieExamplePage : Page {

    // Sample Lottie animation URLs from LottieFiles
    private const val LOADING_ANIMATION = "https://assets2.lottiefiles.com/packages/lf20_p8bfn5to.json"
    private const val CHECK_ANIMATION = "https://assets5.lottiefiles.com/packages/lf20_jbrw3hcz.json"
    private const val ROCKET_ANIMATION = "https://assets3.lottiefiles.com/packages/lf20_0yfsb3a1.json"

    override val title: Reactive<String> = Constant("Lottie Examples")

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        scrolling.col {
            h1 { content = "Lottie Animation Examples" }

            text { content = "Lottie is a library for rendering After Effects animations exported as JSON." }

            space()

            h2 { content = "Loading Animation" }
            sizeConstraints(width = 200.px, height = 200.px).lottie(
                url = LOADING_ANIMATION,
                description = "Loading spinner animation"
            ) {
                loop = true
                autoPlay = true
            }

            space()

            h2 { content = "Success Check Animation" }
            sizeConstraints(width = 200.px, height = 200.px).lottie(
                url = CHECK_ANIMATION,
                description = "Success checkmark animation"
            ) {
                loop = false
                autoPlay = true
            }

            space()

            h2 { content = "Rocket Animation with Controls" }
            var rocketView: LottieView? = null
            sizeConstraints(width = 300.px, height = 300.px).lottie(
                url = ROCKET_ANIMATION,
                description = "Rocket launch animation"
            ) {
                rocketView = this
                loop = true
                autoPlay = false
            }

            row {
                expanding.button {
                    text { content = "Play" }
                    onClick { rocketView?.play() }
                }
                expanding.button {
                    text { content = "Pause" }
                    onClick { rocketView?.pause() }
                }
                expanding.button {
                    text { content = "Stop" }
                    onClick { rocketView?.stop() }
                }
            }

            row {
                text { content = "Speed: " }
                expanding.button {
                    text { content = "0.5x" }
                    onClick { rocketView?.speed = 0.5f }
                }
                expanding.button {
                    text { content = "1x" }
                    onClick { rocketView?.speed = 1f }
                }
                expanding.button {
                    text { content = "2x" }
                    onClick { rocketView?.speed = 2f }
                }
            }
        }
    }
}
