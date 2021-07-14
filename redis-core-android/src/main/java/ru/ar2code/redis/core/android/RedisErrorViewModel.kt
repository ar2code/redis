package ru.ar2code.redis.core.android

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.ar2code.redis.core.*
import ru.ar2code.redis.core.android.ext.toRedisSavedStateStore
import ru.ar2code.redis.core.coroutines.*
import ru.ar2code.utils.Logger

//todo test
abstract class RedisErrorViewModel(
    savedState: SavedStateHandle?,
    initialState: ViewModelStateWithEvent,
    reducers: List<StateReducer<*, *>>,
    triggers: List<StateTrigger<*, *>>? = null,
    reducerSelector: ReducerSelector = DefaultReducerSelector(),
    triggerSelector: StateTriggerSelector = DefaultStateTriggerSelector(),
    listenedServiceIntentSelector: IntentSelector = DefaultIntentSelector(),
    stateStoreSelector: StateStoreSelector = DefaultStateStoreSelector(),
    savedStateHandler: SavedStateHandler? = null,
    logger: Logger = RedisCoreAndroidLogger()
) : RedisViewModel(
    savedState,
    initialState,
    mutableListOf<StateReducer<*, *>>(
        ErrorStateOnReloadAfterErrorIntentReducer(logger),
        AnyStateOnReloadAfterErrorIntentReducer(logger),
    ).apply { addAll(reducers) }.toList(),
    mutableListOf<StateTrigger<*, *>>(
        AnyStateToErrorOccurredStateTrigger(logger)
    ).apply {
        triggers?.let {
            addAll(
                triggers
            )
        }
    }.toList(),
    reducerSelector,
    triggerSelector,
    listenedServiceIntentSelector,
    stateStoreSelector,
    savedStateHandler,
    logger
) {

    override val emitExceptionAsErrorState: Boolean
        get() = true

    //region States

    class ErrorState(
        viewState: RedisErrorViewState?
    ) : ViewModelStateWithEvent(
        viewState,
        null
    ) {
        override fun clone(): State {
            return ErrorState(viewState?.clone().castOrNull())
        }
    }

    class ReloadingAfterErrorState(
        viewState: RedisErrorViewState?
    ) : ViewModelStateWithEvent(
        viewState,
        null
    ) {
        override fun clone(): State {
            return ReloadingAfterErrorState(viewState?.clone().castOrNull())
        }
    }

    //endregion

    //region Reducers

    class ErrorStateOnReloadAfterErrorIntentReducer(logger: Logger) :
        ViewStateReducer<ErrorState, ReloadAfterErrorIntent>(
            logger
        ) {
        override fun reduce(
            currentState: ErrorState,
            intent: ReloadAfterErrorIntent
        ): Flow<State> {
            return flow {
                emit(ReloadingAfterErrorState(currentState.cast<ViewModelStateWithEvent>().viewState.castOrNull()))
            }
        }

        override val isAnyState: Boolean
            get() = false

        override val isAnyIntent: Boolean
            get() = false

        override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
            return currentState is ErrorState && intent is ReloadAfterErrorIntent
        }
    }

    class AnyStateOnReloadAfterErrorIntentReducer(logger: Logger) :
        ViewStateReducer<State, ReloadAfterErrorIntent>(
            logger
        ) {

        override fun reduce(currentState: State, intent: ReloadAfterErrorIntent): Flow<State>? {
            return null
        }

        override val isAnyState: Boolean
            get() = true

        override val isAnyIntent: Boolean
            get() = false

        override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
            return intent is ReloadAfterErrorIntent
        }
    }

    //endregion

    //region Triggers

    class AnyStateToErrorOccurredStateTrigger(logger: Logger) :
        StateTrigger<State, State.ErrorOccurred>(
            logger
        ) {

        override fun specifyTriggerIntent(
            oldState: State,
            newState: State.ErrorOccurred
        ): IntentMessage {
            return OnServiceErrorIntent(newState.cast())
        }

        override val isAnyOldState: Boolean
            get() = true
        override val isAnyNewState: Boolean
            get() = false

        override fun isTriggerApplicable(oldState: State, newState: State): Boolean {
            return newState is State.ErrorOccurred
        }
    }

    //endregion

    //region Intents

    /**
     * Intent indicates that view model should try to restart a whole work process.
     */
    class ReloadAfterErrorIntent : IntentMessage()

    //endregion

    /**
     * Adds variant: [State.ErrorOccurred] to [OnServiceErrorIntent] to [serviceStateListener].
     *
     * When listening service emits [State.ErrorOccurred] view model dispatches [OnServiceErrorIntent] intent.
     */
    fun listenWithErrorHandling(serviceStateListener: ServiceStateListener) {
        viewModelService.listen(extendWithErrorStateListener(serviceStateListener))
    }

    /**
     * Dispatch [ReloadAfterErrorIntent]
     */
    fun tryAgainAfterError() {
        dispatch(ReloadAfterErrorIntent())
    }

    override fun listen(serviceStateListener: ServiceStateListener) {
        throw Exception("Use :listenWithErrorHandling for RedisErrorViewModel instead of :listen.")
    }

    private fun extendWithErrorStateListener(serviceStateListener: ServiceStateListener): ServiceStateListener {
        val mapWithErrorState = serviceStateListener.stateIntentMap.toMutableMap().apply {
            put(State.ErrorOccurred::class, OnServiceErrorIntent.createBuilder())
        }

        return ServiceStateListener(serviceStateListener.listeningService, mapWithErrorState)
    }
}