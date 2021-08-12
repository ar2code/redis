package ru.ar2code.redis.core.android.prepares

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.ar2code.redis.core.IntentMessage
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.coroutines.DefaultIntentSelector
import ru.ar2code.redis.core.coroutines.DefaultReducerSelector
import ru.ar2code.redis.core.coroutines.RedisCoroutineStateService
import ru.ar2code.redis.core.coroutines.StateReducer
import ru.ar2code.redis.core.test.TestLogger

class TestServiceWithException(scope: CoroutineScope, dispatcher: CoroutineDispatcher) :
    RedisCoroutineStateService(
        scope,
        dispatcher,
        State.Initiated(),
        listOf(InitiatedStateOnSomeIntentReducer()),
        DefaultReducerSelector(),
        DefaultIntentSelector(),
        null,
        null,
        null,
        null,
        null,
        TestLogger(),
        emitErrorAsState = true
    ) {

    class SomeIntent : IntentMessage()

    class SomeError : Throwable()

    class InitiatedStateOnSomeIntentReducer :
        StateReducer<State.Initiated, SomeIntent>(TestLogger()) {
        override val isAnyState: Boolean
            get() = false
        override val isAnyIntent: Boolean
            get() = false

        override fun reduce(currentState: State.Initiated, intent: SomeIntent): Flow<State>? {
            return flow {
                throw SomeError()
            }
        }

        override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
            return true
        }
    }
}