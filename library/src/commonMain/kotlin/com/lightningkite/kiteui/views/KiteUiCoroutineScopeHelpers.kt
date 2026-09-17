package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.reactive.context.CoroutineScopeHelpers
import kotlinx.coroutines.CoroutineScope

/**
 * A [CoroutineScope] with helper functions which require an additional [CoroutineScope] context.
 * This will eventually be removed in favor of context parameters.
 * */
public interface KiteUiCoroutineScopeHelpers : CoroutineScopeHelpers {
    public operator fun Action.invoke(): Unit = startAction(this@KiteUiCoroutineScopeHelpers)
}