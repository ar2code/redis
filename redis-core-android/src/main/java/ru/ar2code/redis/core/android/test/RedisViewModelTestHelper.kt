package ru.ar2code.redis.core.android.test

import ru.ar2code.redis.core.IntentMessage
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.android.RedisViewEvent
import ru.ar2code.redis.core.android.RedisViewState
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
fun RedisViewModel<*, *>.disposeServiceAfterNumbersOfDispatchedIntents(
    number: Int,
    disposeDelayMs: Long = 0L
) {
    this.viewModelService.disposeServiceAfterNumbersOfDispatchedIntents(number, disposeDelayMs)
}

fun RedisViewModel<*, *>.disposeServiceWhenIntentDispatched(
    expectIntent: KClass<out IntentMessage>,
    disposeDelayMs: Long = 0L
) {
    this.viewModelService.disposeServiceWhenIntentDispatched(expectIntent, disposeDelayMs)
}

suspend fun RedisViewModel<*, *>.awaitWhileNotDisposedWithTimeout(
    timeoutMs: Long
) {
    this.viewModelService.awaitWhileNotDisposedWithTimeout(timeoutMs)
}

suspend fun RedisViewModel<*, *>.awaitStateWithTimeout(
    timeoutMs: Long,
    expectState: KClass<out State>
): State {
    return this.viewModelService.awaitStateWithTimeout(timeoutMs, expectState)
}