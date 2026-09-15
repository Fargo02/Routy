package com.example.routy.core.mvi

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

@Composable
fun <E> CollectEffects(
    effects: Flow<E>,
    onEffect: suspend (E) -> Unit,
) {
    val owner = LocalLifecycleOwner.current
    val handler by rememberUpdatedState(onEffect)
    LaunchedEffect(owner, effects) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { effects.collect { handler(it) } }
    }
}
