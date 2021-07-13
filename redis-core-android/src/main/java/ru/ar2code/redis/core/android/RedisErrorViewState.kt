package ru.ar2code.redis.core.android

import ru.ar2code.redis.core.State

interface RedisErrorViewState : RedisViewState {
    val error: Changeable<State.ErrorOccurred>

    //todo test
    fun isErrorShouldBeRender(currentUiState: RedisErrorViewState?): Boolean {
        return error.data != null && error.version != currentUiState?.error?.version
    }

    fun updateErrorVersion(serviceError: State.ErrorOccurred): RedisErrorViewState
}