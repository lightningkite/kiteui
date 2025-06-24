package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.QueryParameter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.mainPageNavigator
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.readable.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


@Routable("/slideshow")
class SlideshowTestPage() : Page {

    val current = LateInitProperty<ImageSource>()

    override fun ViewWriter.render(): ViewModifiable {
        return unpadded - FullScreenImageSemantic.onNext - button {
            // cannotBeCovered = false
            debugName = "FullScreenImage"
            withoutLoading.launch {
                var i = 0
                while (true) {
                    i = i + 1
                    current.value = ImageRemote("https://picsum.photos/seed/${i}/200/200")
                    delay(4.seconds)
                }
            }
            val inRemoval = HashSet<RView>()
            fun cleanOthers(ok: RView) {
                for (child in children.toList()) {
                    if (child in inRemoval) continue
                    if (child !== ok) {
                        inRemoval.add(child)
                        child.opacity = 0.0
                        child.withoutLoading.launch {
                            delay(theme.transitionDuration)
                            removeChild(child)
                        }
                    }
                }
            }
            centered - activityIndicator {
                // cannotBeCovered = false
            }
            reactive {
                val it = current()
                when (it) {
                    is VideoRemote -> video {
                        // cannotBeCovered = false
                        scaleType = ImageScaleType.Fit
                        source = it
                        loop = true
                        withoutAnimation {
                            opacity = 0.0
                        }
                        withoutLoading.launch {
                            volume set 0f
                            delay(1.seconds)
                            opacity = 1.0
                            playing set true
                            delay(theme.transitionDuration + 0.1.seconds)
                            cleanOthers(this@video)
                        }
                    }

                    is ImageRemote -> rawImage(it, "Image", ImageScaleType.Fit) {
                        // cannotBeCovered = false
                        withoutAnimation {
                            opacity = 0.0
                        }
                        withoutLoading.reactive {
                            this@rawImage.state.state().handle(
                                success = {
                                    this@rawImage.launch {
                                        delay(50.milliseconds)
                                        opacity = 1.0
                                        delay(theme.transitionDuration + 0.1.seconds)
                                        cleanOthers(this@rawImage)
                                    }
                                },
                                exception = {},
                                notReady = {}
                            )
                        }
                    }

                    else -> text("Huh?")
                }
            }
            onClick {
                pageNavigator.dismiss()
            }
        }
    }
}

val CoroutineScope.withoutLoading get() = CoroutineScope(this.coroutineContext.minusKey(StatusListener))

data object FullScreenImageSemantic : Semantic("fullscreenimage") {
    override fun default(theme: Theme): ThemeAndBack {
        return theme.withBack(
            background = Color.black,
            foreground = Color.white,
            cornerRadii = CornerRadii.ForceConstant(0.px),
            derivations = mapOf(
                FocusSemantic to { it.withoutBack },
                HoverSemantic to { it.withoutBack },
                DownSemantic to { it.withoutBack },
            )
        )
    }
}

