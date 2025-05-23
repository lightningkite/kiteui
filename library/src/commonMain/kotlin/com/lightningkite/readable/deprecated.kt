package com.lightningkite.readable

import com.lightningkite.kiteui.exceptions.ExceptionHandlers
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.reactive.DependentAction
import com.lightningkite.kiteui.reactive.FrequencyCapAction
import com.lightningkite.kiteui.reactive.RetryableAction
import com.lightningkite.signal.AppScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.serializer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

typealias Action = com.lightningkite.kiteui.reactive.Action
typealias AppState = com.lightningkite.kiteui.reactive.AppState
typealias PersistentProperty<T> = com.lightningkite.kiteui.reactive.PersistentProperty<T>
inline fun <reified T> PersistentProperty(
    key: String,
    defaultValue: T
): com.lightningkite.kiteui.reactive.PersistentProperty<T> = com.lightningkite.kiteui.reactive.PersistentProperty(key, defaultValue, serializer())
fun Action(
    title: String,
    icon: Icon = Icon.send,
    clearErrorOnDependencyChange: Boolean = ExceptionHandlers.clearErrorOnDependencyChange,
    keepRunningWhile: CoroutineScope? = AppScope,
    frequencyCap: Duration? = 500.milliseconds,
    ignoreRetryWhileRunning: Boolean = true,
    action: suspend () -> Unit
) = if (clearErrorOnDependencyChange) {
    DependentAction(title, icon, keepRunningWhile, ignoreRetryWhileRunning, action = action)
} else {
    RetryableAction(title, icon, keepRunningWhile, ignoreRetryWhileRunning, action = action)
}.let {
    frequencyCap?.let { f ->
        FrequencyCapAction(it, f)
    } ?: it
}