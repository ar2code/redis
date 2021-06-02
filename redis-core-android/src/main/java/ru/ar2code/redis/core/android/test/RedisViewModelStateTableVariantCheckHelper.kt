package ru.ar2code.redis.core.android.test

import kotlinx.coroutines.delay
import ru.ar2code.redis.core.IntentMessage
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.android.BaseViewEvent
import ru.ar2code.redis.core.android.BaseViewState
import ru.ar2code.redis.core.android.RedisViewModel
import ru.ar2code.redis.core.coroutines.AwaitStateTimeoutException
import ru.ar2code.redis.core.coroutines.RedisCoroutineStateService
import ru.ar2code.redis.core.coroutines.awaitStateWithTimeout
import ru.ar2code.redis.core.coroutines.test.RedisServiceStateTableVariantCheckHelper
import ru.ar2code.redis.core.test.TestLogger
import ru.ar2code.utils.Logger
import kotlin.reflect.KClass

/**
 * Helper class for testing variant from state machine table.
 * Variant means: given initial state then dispatch an intent then receive a new state or keep previous state.
 *
 * @param viewModel - viewModel to be tested
 * @param initialStateIntents - list of intents that should be dispatched to the service for move it state to needed initial state.
 * @param initialState - start state for testing variant
 * @param initialIntentDispatchDelayMs - delay before each init intent dispatch
 * @param checkStateIntent - intent that should be checked as state table variant
 * @param timeoutMs - time for awaiting expected state after dispatching [checkStateIntent]
 * @param expectState - state that service should receive after dispatching [checkStateIntent]. If null - service should keep previous state.
 * @param logger - log object.
 */
class RedisViewModelStateTableVariantCheckHelper(
    private val viewModel: RedisViewModel<out BaseViewState, out BaseViewEvent>,
    private val initialStateIntents: List<IntentMessage>?,
    private val initialState: KClass<out State>,
    private val initialIntentDispatchDelayMs: Long,
    private val checkStateIntent: IntentMessage,
    private val timeoutMs: Long,
    private val expectState: KClass<out State>?,
    private val logger: Logger = TestLogger()
) : RedisServiceStateTableVariantCheckHelper(
    viewModel.viewModelService,
    initialStateIntents,
    initialState,
    initialIntentDispatchDelayMs,
    checkStateIntent,
    timeoutMs,
    expectState,
    logger
)