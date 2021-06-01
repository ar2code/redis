package ru.ar2code.redis.core.android.test

import ru.ar2code.redis.core.IntentMessage
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.android.BaseViewEvent
import ru.ar2code.redis.core.android.BaseViewState
import ru.ar2code.redis.core.android.RedisViewModel
import ru.ar2code.redis.core.coroutines.awaitStateWithTimeout
import ru.ar2code.redis.core.coroutines.test.awaitWhileNotDisposedWithTimeout
import ru.ar2code.redis.core.coroutines.test.disposeServiceAfterNumbersOfDispatchedIntents
import ru.ar2code.redis.core.coroutines.test.disposeServiceWhenIntentDispatched
import kotlin.reflect.KClass

/**
 * Helper for service testing.
 * Awaits specified [number] of dispatched intents and then dispose service.
 */
fun RedisViewModel<out BaseViewState, out BaseViewEvent>.disposeServiceAfterNumbersOfDispatchedIntents(
    number: Int,
    disposeDelayMs: Long = 0L
) {
    this.viewModelService.disposeServiceAfterNumbersOfDispatchedIntents(number, disposeDelayMs)
}

fun RedisViewModel<out BaseViewState, out BaseViewEvent>.disposeServiceWhenIntentDispatched(
    expectIntent: KClass<out IntentMessage>,
    disposeDelayMs: Long = 0L
) {
    this.viewModelService.disposeServiceWhenIntentDispatched(expectIntent, disposeDelayMs)
}

suspend fun RedisViewModel<out BaseViewState, out BaseViewEvent>.awaitWhileNotDisposedWithTimeout(
    timeoutMs: Long
) {
    this.viewModelService.awaitWhileNotDisposedWithTimeout(timeoutMs)
}

suspend fun RedisViewModel<out BaseViewState, out BaseViewEvent>.awaitStateWithTimeout(
    timeoutMs: Long,
    expectState: KClass<out State>
): State {
    return this.viewModelService.awaitStateWithTimeout(timeoutMs, expectState)
}