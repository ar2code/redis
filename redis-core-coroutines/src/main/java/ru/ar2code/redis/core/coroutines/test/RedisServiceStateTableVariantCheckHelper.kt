package ru.ar2code.redis.core.coroutines.test

import kotlinx.coroutines.delay
import ru.ar2code.redis.core.IntentMessage
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.coroutines.AwaitStateTimeoutException
import ru.ar2code.redis.core.coroutines.RedisCoroutineStateService
import ru.ar2code.redis.core.coroutines.awaitStateWithTimeout
import ru.ar2code.redis.core.test.TestLogger
import ru.ar2code.utils.Logger
import kotlin.reflect.KClass

/**
 * Helper class for testing variant from state machine table.
 * Variant means: given initial state then dispatch an intent then receive a new state or keep previous state.
 *
 * @param service - service to be tested
 * @param initialStateIntents - list of intents that should be dispatched to the service for move it state to needed initial state.
 * @param initialState - start state for testing variant
 * @param initialIntentDispatchDelayMs - delay before each init intent dispatch
 * @param checkStateIntent - intent that should be checked as state table variant
 * @param timeoutMs - time for awaiting expected state after dispatching [checkStateIntent]
 * @param expectState - state that service should receive after dispatching [checkStateIntent]. If null - service should keep previous state.
 * @param logger - log object.
 */
open class RedisServiceStateTableVariantCheckHelper(
    private val service: RedisCoroutineStateService,
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

        disposeService()

        return result
    }

    protected open fun disposeService() {
        service.dispose()
    }

    private suspend fun checkMachine(): Boolean {

        val currentState = service.serviceState

        service.dispatch(checkStateIntent)

        /*
        Expect state as an instance of [expectState] or currentState if parameter expectState is null.
        ExpectState is null means keep previous state.
         */
        val expectedState = expectState ?: currentState::class

        return try {
            val state = service.awaitStateWithTimeout(
                timeoutMs,
                expectedState
            )

            logger.info("$LOG_KEY received state=${state.objectLogName}")

            expectedState.isInstance(state)

        } catch (e: AwaitStateTimeoutException) {
            logger.info("$LOG_KEY current state=${service.serviceState.objectLogName}. Check variant timeout error: ${e.message}.")

            isExpectNotStateChanged
        }
    }

    private suspend fun goToInitialState() {
        initialStateIntents?.forEach {
            logger.info("$LOG_KEY dispatch intent=${it.objectLogName}")
            service.dispatch(it)
            delay(initialIntentDispatchDelayMs)
        }

        try {
            val state = service.awaitStateWithTimeout(timeoutMs, initialState)
            logger.info("$LOG_KEY initial state=${state.objectLogName}")
        } catch (e: AwaitStateTimeoutException) {
            throw AwaitStateTimeoutException("$LOG_KEY current state=${service.serviceState.objectLogName}. Initial state error: ${e.message}.")
        }
    }

}