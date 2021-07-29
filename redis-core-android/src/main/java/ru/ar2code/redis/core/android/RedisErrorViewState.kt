package ru.ar2code.redis.core.android

import ru.ar2code.redis.core.State

interface RedisErrorViewState : RedisViewState {
    val error: Changeable<State.ErrorOccurred>

    //todo test
    fun isErrorShouldBeRendered(currentUiState: RedisErrorViewState?): Boolean {
        return error.shouldBeRendered(currentUiState?.error)
    }

    fun updateErrorVersion(serviceError: State.ErrorOccurred): RedisErrorViewState

}