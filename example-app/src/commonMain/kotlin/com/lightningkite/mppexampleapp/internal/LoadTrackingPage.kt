package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.h1
import com.lightningkite.kiteui.views.direct.progressBar
import com.lightningkite.kiteui.views.direct.scrolling
import com.lightningkite.kiteui.views.direct.shownWhen
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.loadTracker
import com.lightningkite.kiteui.views.withLoadingProgress
import com.lightningkite.reactive.context.reactiveSuspending
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

@Routable("/load-tracking")
object LoadTrackingPage : Page {
    override fun ElementWriter.CanAddTheme.render() {
        scrolling.withLoadingProgress { progress ->
            col {
                h1("Load Tracking Page")

                val tracking = context.loadTracker

                text {
                    ::content { "Progress: ${progress()}  Load Count: ${tracking.loadCount()}" }
                }

                shownWhen { progress() != null }.progressBar { ::ratio { progress() ?: 1f } }

                for (i in 0..100) {
                    text {
                        val delay = (i + Random.nextInt(-2, 2)).coerceAtLeast(0).seconds / 10
                        reactiveSuspending {
                            delay(delay)
                            content = "Delayed $delay"
                        }
                    }
                }
            }
        }
    }
}