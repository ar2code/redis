package ru.ar2code.redis.core.android.test

import ru.ar2code.redis.core.IntentMessage
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.android.RedisViewEvent
import ru.ar2code.redis.core.android.RedisViewState
import ru.ar2code.redis.core.android.RedisViewModel
import ru.ar2code.redis.core.coroutines.test.RedisServiceStateTableVariantCheckHelper
import ru.ar2code.redis.core.test.TestLogger
import ru.ar2code.utils.Logger
import kotlin.reflect.KClass

/**
 * Helper class for testing viewModel variant from state machine table.
 * Variant means: given initial state then dispatch an intent then receive a new state or keep previous state.
 *
 * @param viewModel - viewModel to be tested
 * @param initialStateIntents - list of intents that should be dispatched to the viewModel's service for moving state to needed initial state.
 * @param initialState - start state for testing variant. Checker will awaits initial state before start checking variant. Can throw timeout error.
 * @param initialIntentDispatchDelayMs - delay before first init intent dispatched.
 * @param checkStateIntent - checked intent that will be dispatched after getting [initialState].
 * @param timeoutMs - time for awaiting initial state and expected state after dispatching [checkStateIntent].
 * @param expectState - state that service should receive after dispatching [checkStateIntent]. If null - service should keep previous state (means reducer returned null).
 * @param logger - log object.
 */
class RedisViewModelStateTableVariantCheckHelper(
    private val viewModel: RedisViewModel<*,*>,
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