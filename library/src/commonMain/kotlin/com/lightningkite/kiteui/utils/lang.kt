package com.lightningkite.kiteui.utils

import kotlinx.coroutines.delay
import kotlinx.datetime.Clock.System.now
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

suspend fun takeAtLeast(duration: Duration, action: suspend () -> Unit) {
    val begin = now()
    action()
    val elapsed = now() - begin
    val remaining = duration - elapsed
    if (remaining > 0.seconds) {
        delay(duration)
    }
}