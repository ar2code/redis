package ru.ar2code.redis.core.coroutines.test

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import ru.ar2code.redis.core.IntentMessage
import ru.ar2code.redis.core.coroutines.RedisCoroutineStateService
import ru.ar2code.redis.core.coroutines.ServiceIntentDispatcherListener
import kotlin.reflect.KClass

/**
 * Helper for service testing.
 * Awaits specified [number] of dispatched intents and then dispose service.
 */
fun RedisCoroutineStateService.disposeServiceAfterNumbersOfDispatchedIntents(
    number: Int,
    disposeDelayMs: Long = 0L
) {
    var currentCounter = 0

    this.serviceIntentDispatcherListener = object : ServiceIntentDispatcherListener {
        override suspend fun onIntentDispatched(intent: IntentMessage) {
            currentCounter++

            println("currentCounter=$currentCounter")
            if (currentCounter >= number) {
                delay(disposeDelayMs)
                this@disposeServiceAfterNumbersOfDispatchedIntents.dispose()
            }
        }
    }
}

fun RedisCoroutineStateService.disposeServiceWhenIntentDispatched(
    expectIntent: KClass<out IntentMessage>,
    disposeDelayMs: Long = 0L
) {
    this.serviceIntentDispatcherListener = object : ServiceIntentDispatcherListener {
        override suspend fun onIntentDispatched(intent: IntentMessage) {
            if (expectIntent.isInstance(intent)) {
                delay(disposeDelayMs)
                this@disposeServiceWhenIntentDispatched.dispose()
            }
        }
    }
}

suspend fun RedisCoroutineStateService.awaitWhileNotDisposedWithTimeout(timeoutMs: Long) {
    withTimeout(timeoutMs) {
        while (!this@awaitWhileNotDisposedWithTimeout.isDisposed()) {
            delay(1)
        }
    }
}