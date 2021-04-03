package ru.ar2code.redis.core.coroutines

import kotlinx.coroutines.delay
import ru.ar2code.redis.core.IntentMessage
import ru.ar2code.redis.core.RedisStateService
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.test.TestLogger
import ru.ar2code.utils.Logger
import kotlin.reflect.KClass

/**
 * todo comments
 */
class RedisServiceStateTableVariantCheckHelper(
    private val service: RedisStateService,
    private val initialStateIntents: List<IntentMessage>?,
    private val initialState: KClass<out State>,
    private val initialIntentDispatchDelayMs: Long,
    private val checkStateIntent: IntentMessage,
    private val timeoutMs: Long,
    private val expectState: KClass<out State>?,
    private val logger: Logger = TestLogger()
) {

    companion object {
        private const val LOG_KEY = "[TestRedisServiceStateTableVariant]"
    }

    private val isExpectNotStateChanged = expectState == null

    suspend fun checkVariant(): Boolean {
        goToInitialState()

        val result = checkMachine()

        service.dispose()

        return result
    }

    suspend fun awaitChecking() {
        while (!service.isDisposed()) {
            delay(1)
        }
    }

    private suspend fun checkMachine(): Boolean {
        service.dispatch(checkStateIntent)

        val expectedState = expectState ?: State::class

        return try {
            val state = service.awaitStateWithTimeout(
                timeoutMs,
                expectedState
            )

            logger.info("$LOG_KEY received state = ${state.objectLogName}")

            expectedState.isInstance(state)

        } catch (e: AwaitStateTimeoutException) {
            logger.info("$LOG_KEY current state = ${service.serviceState.objectLogName}. Check variant timeout error: ${e.message}.")

            isExpectNotStateChanged
        }
    }

    private suspend fun goToInitialState() {
        initialStateIntents?.forEach {
            service.dispatch(it)
            delay(initialIntentDispatchDelayMs)
        }

        try {
            service.awaitStateWithTimeout(timeoutMs, initialState)
        } catch (e: AwaitStateTimeoutException) {
            throw AwaitStateTimeoutException("$LOG_KEY current state = ${service.serviceState.objectLogName}. Initial state error: ${e.message}.")
        }
    }

}