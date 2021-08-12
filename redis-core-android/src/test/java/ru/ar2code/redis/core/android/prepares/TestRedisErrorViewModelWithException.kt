package ru.ar2code.redis.core.android.prepares

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.ar2code.redis.core.IntentMessage
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.android.Changeable
import ru.ar2code.redis.core.android.RedisErrorViewModel
import ru.ar2code.redis.core.android.ViewStateReducer
import ru.ar2code.redis.core.coroutines.StateReducer
import ru.ar2code.redis.core.test.TestLogger

class TestRedisErrorViewModelWithException(reducers: List<StateReducer<*, *>>) :
    RedisErrorViewModel(
        null,
        initialState = ViewModelInitiatedState(),
        reducers = reducers,
        logger = TestLogger()
    ) {

    class ViewModelErrorState(val error: State.ErrorOccurred) : State() {
        override fun clone(): State {
            return ViewModelErrorState(error)
        }
    }

    class ServiceErrorState(val error: State.ErrorOccurred) : State() {
        override fun clone(): State {
            return ServiceErrorState(error)
        }
    }

    class TestRedisViewModelThrowable : Throwable()

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

    class ErrorOccurredOnViewModelErrorNullReducer :
        ViewStateReducer<State.ErrorOccurred, OnViewModelErrorIntent>(
            TestLogger()
        ) {
        override fun reduce(
            currentState: State.ErrorOccurred,
            intent: OnViewModelErrorIntent
        ): Flow<State>? {
            return null
        }

        override val isAnyState: Boolean
            get() = false
        override val isAnyIntent: Boolean
            get() = false

        override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
            return currentState is State.ErrorOccurred && intent is OnViewModelErrorIntent
        }
    }

    class ErrorOccurredOnViewModelErrorEmitErrorStateReducer :
        ViewStateReducer<State.ErrorOccurred, OnViewModelErrorIntent>(
            TestLogger()
        ) {
        override fun reduce(
            currentState: State.ErrorOccurred,
            intent: OnViewModelErrorIntent
        ): Flow<State> {
            return flow {
                emit(ErrorState(TestViewModelState(Changeable(currentState))))
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

    class ErrorOccurredOnViewModelErrorEmitViewModelErrorStateReducer :
        ViewStateReducer<State.ErrorOccurred, OnViewModelErrorIntent>(
            TestLogger()
        ) {
        override fun reduce(
            currentState: State.ErrorOccurred,
            intent: OnViewModelErrorIntent
        ): Flow<State> {
            return flow {
                emit(ViewModelErrorState(intent.errorState))
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

    class ViewModelTypeAStateOnListeningServiceErrorIntentEmitErrorStateReducer :
        StateReducer<ViewModelTypeAState, OnListeningServiceErrorIntent>(TestLogger()) {
        override val isAnyState: Boolean
            get() = false
        override val isAnyIntent: Boolean
            get() = false

        override fun reduce(
            currentState: ViewModelTypeAState,
            intent: OnListeningServiceErrorIntent
        ): Flow<State> {
            return flow {
                emit((ErrorState(null)))
            }
        }

        override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
            return currentState is ViewModelTypeAState && intent is OnListeningServiceErrorIntent
        }
    }

    class ViewModelTypeAStateOnListeningServiceErrorIntentEmitServiceStateReducer :
        StateReducer<ViewModelTypeAState, OnListeningServiceErrorIntent>(TestLogger()) {
        override val isAnyState: Boolean
            get() = false
        override val isAnyIntent: Boolean
            get() = false

        override fun reduce(
            currentState: ViewModelTypeAState,
            intent: OnListeningServiceErrorIntent
        ): Flow<State> {
            return flow {
                emit((ServiceErrorState(intent.errorState)))
            }
        }

        override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
            return currentState is ViewModelTypeAState && intent is OnListeningServiceErrorIntent
        }
    }

}