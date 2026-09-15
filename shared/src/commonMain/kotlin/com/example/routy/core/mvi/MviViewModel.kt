package com.example.routy.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

abstract class MviViewModel<I, E> : ViewModel() {
    private val events = Channel<E>(Channel.BUFFERED)
    val effects = events.receiveAsFlow()

    protected fun effect(value: E) {
        viewModelScope.launch { events.send(value) }
    }

    abstract fun accept(intent: I)
}
