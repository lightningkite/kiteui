package com.lightningkite.readable

import com.lightningkite.kiteui.exceptions.ExceptionHandlers
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.reactive.DependentAction
import com.lightningkite.kiteui.reactive.FrequencyCapAction
import com.lightningkite.kiteui.reactive.PersistentProperty
import com.lightningkite.kiteui.reactive.RetryableAction
import com.lightningkite.signal.AppScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.serializer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

public typealias Action = Action
public typealias AppState = AppState
public typealias PersistentProperty<T> = PersistentProperty<T>
public inline fun <reified T> PersistentProperty(
    key: String,
    defaultValue: T
): PersistentProperty<T> = PersistentProperty(key, defaultValue, serializer())
public fun Action(
    title: String,
    icon: Icon = Icon.send,
    clearErrorOnDependencyChange: Boolean = ExceptionHandlers.clearErrorOnDependencyChange,
    keepRunningWhile: CoroutineScope? = AppScope,
    frequencyCap: Duration? = 500.milliseconds,
    ignoreRetryWhileRunning: Boolean = true,
    action: suspend () -> Unit
): Action = if (clearErrorOnDependencyChange) {
    DependentAction(title, icon, keepRunningWhile, ignoreRetryWhileRunning, action = action)
} else {
    RetryableAction(title, icon, keepRunningWhile, ignoreRetryWhileRunning, action = action)
}.let {
    frequencyCap?.let { f ->
        FrequencyCapAction(it, f)
    } ?: it
}