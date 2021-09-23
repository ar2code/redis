package ru.ar2code.redis.core.coroutines.test

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import ru.ar2code.redis.core.IntentMessage
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.coroutines.AwaitStateTimeoutException
import ru.ar2code.redis.core.coroutines.RedisCoroutineStateService
import ru.ar2code.redis.core.coroutines.ServiceIntentDispatcherListener
import ru.ar2code.redis.core.coroutines.awaitStateWithTimeout
import ru.ar2code.redis.core.test.TestLogger
import ru.ar2code.utils.Logger
import kotlin.reflect.KClass

/**
 * Helper class for testing variant from state machine table.
 * Variant means: given initial state then dispatch an intent then receive a new state or keep previous state (means reducer returned null).
 *
 * @param service - service to be tested.
 * @param initialStateIntents - list of intents that should be dispatched to the service for moving state to needed initial state.
 * @param initialState - start state for testing variant. Checker will awaits initial state before start checking variant. Can throw timeout error.
 * @param initialIntentDispatchDelayMs - delay before first init intent dispatched.
 * @param checkStateIntent - checked intent that will be dispatched after getting [initialState].
 * @param timeoutMs - time for awaiting initial state and expected state after dispatching [checkStateIntent].
 * @param expectState - state that service should receive after dispatching [checkStateIntent]. If null - service should keep previous state (means reducer returned null).
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
        private const val AWAIT_DELAY_MS = 1L
    }

    suspend fun checkVariant(): Boolean {
        awaitInitialStateAfterDispatchingInitialIntents()

        val result = checkState()

        disposeService()

        return result
    }

    protected open fun disposeService() {
        service.dispose()
    }

    private suspend fun checkState(): Boolean {

        var reducerReturnsNullForCheckingIntent = false

        service.serviceIntentDispatcherListener = object : ServiceIntentDispatcherListener {
            override suspend fun reducerNotFoundForIntent(intent: IntentMessage) {
                if (intent == checkStateIntent) {
                    reducerReturnsNullForCheckingIntent = true
                }
            }
        }

        service.dispatch(checkStateIntent)

        expectState?.let { expectedState ->

            return try {

                logger.info("$LOG_KEY check machine: awaiting state class $expectedState")

                val state = service.awaitStateWithTimeout(
                    timeoutMs,
                    expectedState
                )

                logger.info("$LOG_KEY received state ${state.objectLogName}")

                expectedState.isInstance(state)

            } catch (e: AwaitStateTimeoutException) {
                logger.info("$LOG_KEY current state=${service.serviceStateInternal.objectLogName}. Check variant timeout error: ${e.message}.")
                false
            }
        } ?: kotlin.run {
            return try {
                logger.info("$LOG_KEY check machine: awaiting null from reducer.")

                val awaitReducerReturnsNull = withTimeoutOrNull(timeoutMs) {
                    while (!reducerReturnsNullForCheckingIntent) {
                        delay(AWAIT_DELAY_MS)
                    }
                    true
                } ?: throw AwaitStateTimeoutException()

                awaitReducerReturnsNull
            } catch (e: AwaitStateTimeoutException) {
                logger.info("$LOG_KEY reducer returning null timeout error. Needed reducer was not executed or returned not nullable flow.")
                false
            }

        }
    }

    private suspend fun awaitInitialStateAfterDispatchingInitialIntents() {

        withTimeoutOrNull(timeoutMs) {
            dispatchInitialIntents()
        }
            ?: throw AwaitStateTimeoutException("Timeout while dispatching initial intents. Seems that something is wrong inside the check helper class.")

        try {
            logger.info("$LOG_KEY awaiting initial state is started")

            service.awaitStateWithTimeout(timeoutMs, initialState)

            logger.info("$LOG_KEY awaiting initial state is OK: currentState=${service.serviceState.objectLogName}")
        } catch (e: AwaitStateTimeoutException) {
            throw AwaitStateTimeoutException("$LOG_KEY awaiting initial state is ERROR: current state=${service.serviceState.objectLogName}. Msg: ${e.message}.")
        }
    }

    private suspend fun dispatchInitialIntents() {
        logger.info("$LOG_KEY dispatchInitialIntents started")

        delay(initialIntentDispatchDelayMs)

        var currentIntentHandled = false
        var currentIntent: IntentMessage? = null

        service.serviceIntentDispatcherListener = object : ServiceIntentDispatcherListener {
            override suspend fun onIntentDispatched(intent: IntentMessage) {
                if (intent == currentIntent) {
                    currentIntentHandled = true
                }
                logger.info("$LOG_KEY:dispatchInitialIntents: onIntentDispatched $intent. currentIntent=$currentIntent, currentIntentHandled=$currentIntentHandled")
            }
        }

        currentIntent = getNextInitialIntent(currentIntent)

        dispatchIntent(currentIntent)

        while (currentIntent != null) {
            //await intent handled
            while (!currentIntentHandled) {
                logger.info("$LOG_KEY awaiting current intent handling: currentIntent=$currentIntent")
                delay(AWAIT_DELAY_MS)
            }
            //dispatch next intent
            currentIntentHandled = false
            currentIntent = getNextInitialIntent(currentIntent)
            dispatchIntent(currentIntent)
        }

        logger.info("$LOG_KEY dispatchInitialIntents finished")
    }

    private fun getNextInitialIntent(lastDispatchedIntent: IntentMessage?): IntentMessage? {

        val intentToDispatch = if (lastDispatchedIntent == null) {
            initialStateIntents?.firstOrNull()
        } else {
            val lastIndex = initialStateIntents?.indexOf(lastDispatchedIntent) ?: -1

            val initialStateIntentsSize = initialStateIntents?.size ?: 0

            if (lastIndex >= 0 && lastIndex < initialStateIntentsSize - 1) {
                initialStateIntents?.get(lastIndex + 1)
            } else {
                null
            }
        }

        return intentToDispatch
    }

    private fun dispatchIntent(intentToDispatch: IntentMessage?) {
        intentToDispatch?.let {
            logger.info("$LOG_KEY dispatched initial intent: $it")
            service.dispatch(it)
        }
    }
}