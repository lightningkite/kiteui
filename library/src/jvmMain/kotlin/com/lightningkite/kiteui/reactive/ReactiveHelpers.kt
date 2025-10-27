package com.lightningkite.kiteui.reactive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.core.MutableReactiveValue
import com.lightningkite.reactive.core.Reactive


// Custom composable hook to handle Signal -> State conversion
@Composable
fun <T> Signal<T>.collectAsMutableState(): MutableState<T> {
    val state = remember { mutableStateOf(this.value) }

    DisposableEffect(this) {
        val listener = { state.value = this@collectAsMutableState.value }
        this@collectAsMutableState.addListener(listener)
        onDispose {}
    }

    return state
}

// Extension for MutableReactiveValue
@Composable
fun <T> MutableReactiveValue<T>.collectAsMutableState(): MutableState<T> {
    val state = remember { mutableStateOf(this.value) }

    DisposableEffect(this) {
        val listener = { state.value = this@collectAsMutableState.value }
        this@collectAsMutableState.addListener(listener)
        onDispose {}
    }

    return state
}

// Extension for Reactive (read-only)
@Composable
fun <T> Reactive<T>.collectAsState(default: T): androidx.compose.runtime.State<T> {
    val state = remember { mutableStateOf(default) }

    DisposableEffect(this) {
        val listener = { state.value = this@collectAsState.state.getOrNull() ?: default }
        this@collectAsState.addListener(listener)
        onDispose {}
    }

    return state
}