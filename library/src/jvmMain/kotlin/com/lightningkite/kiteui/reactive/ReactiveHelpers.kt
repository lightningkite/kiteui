package com.lightningkite.kiteui.reactive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.lightningkite.reactive.core.Signal


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