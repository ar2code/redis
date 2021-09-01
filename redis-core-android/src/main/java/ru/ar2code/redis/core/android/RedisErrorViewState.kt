package ru.ar2code.redis.core.android

import ru.ar2code.redis.core.State

interface RedisErrorViewState : RedisViewState {
    val error: Changeable<State.ErrorOccurred>

    fun isErrorShouldBeRendered(currentUiState: RedisErrorViewState?): Boolean {
        return error.shouldBeRendered(currentUiState?.error)
    }

    /**
     * @return RedisErrorViewState with [RedisErrorViewState.error] that contains [serviceError] as data and upper version from current [RedisErrorViewState.error] changeable.
     */
    fun updateErrorVersion(serviceError: State.ErrorOccurred): RedisErrorViewState

    /**
     * @return RedisErrorViewState without error. [RedisErrorViewState.error] data should be null.
     */
    fun clearError(): RedisErrorViewState
}