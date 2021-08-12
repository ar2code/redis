package ru.ar2code.redis.core.android.prepares

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.ar2code.redis.core.IntentMessage
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.android.RedisErrorViewModel
import ru.ar2code.redis.core.android.ViewStateReducer
import ru.ar2code.redis.core.test.TestLogger

class TestRedisErrorViewModelWithException :
    RedisErrorViewModel(
        null,
        initialState = ViewModelInitiatedState(),
        reducers = listOf(
            InitiatedStateTypeAReducer(),
            InitiatedStateTypeBExceptionReducer(),
            InitiatedStateUiViewStateOnlyReducer(),
            InitiatedStateUiEventOnlyReducer(),
            InitiatedStateUiViewWithEventReducer(),
            ErrorOccurredOnViewModelErrorReducer()
        ),
        logger = TestLogger()
    ) {

    class TestRedisViewModelThrowable : Throwable()

    class OnViewModelErrorIntentReceivedState(val intent: OnViewModelErrorIntent) : State() {
        override fun clone(): State {
            return OnViewModelErrorIntentReceivedState(intent)
        }
    }

    class InitiatedStateTypeBExceptionReducer :
        ViewStateReducer<ViewModelInitiatedState, IntentUiTypeB>(
            TestLogger()
        ) {
        override fun reduce(
            currentState: ViewModelInitiatedState,
            intent: IntentUiTypeB
        ): Flow<State> {
            return flow {
                throw TestRedisViewModelThrowable()
            }
        }

        override val isAnyState: Boolean
            get() = false
        override val isAnyIntent: Boolean
            get() = false

        override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
            return currentState is ViewModelInitiatedState && intent is IntentUiTypeB
        }
    }

    class ErrorOccurredOnViewModelErrorReducer :
        ViewStateReducer<State.ErrorOccurred, OnViewModelErrorIntent>(
            TestLogger()
        ) {
        override fun reduce(
            currentState: State.ErrorOccurred,
            intent: OnViewModelErrorIntent
        ): Flow<State> {
            return flow {
                emit(OnViewModelErrorIntentReceivedState(intent))
            }
        }

        override val isAnyState: Boolean
            get() = false
        override val isAnyIntent: Boolean
            get() = false

        override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
            return currentState is State.ErrorOccurred && intent is OnViewModelErrorIntent
        }
    }

}