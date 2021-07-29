package ru.ar2code.redis.core.android

import ru.ar2code.redis.core.State
import kotlin.reflect.KClass

interface RedisErrorViewState : RedisViewState {
    val error: Changeable<State.ErrorOccurred>

    //todo test
    fun isErrorShouldBeRendered(currentUiState: RedisErrorViewState?): Boolean {
        return error.shouldBeRendered(currentUiState?.error)
    }

    fun isErrorThrowableInstanceOf(errorType: KClass<out Throwable>): Boolean {
        return error.data?.throwable != null && errorType.isInstance(error.data?.throwable)
    }

    fun updateErrorVersion(serviceError: State.ErrorOccurred): RedisErrorViewState

    /**
     * @return true if error is not specific for the current viewState, and will not be rendered in a special way.
     */
    fun isUnhandledError(): Boolean
}