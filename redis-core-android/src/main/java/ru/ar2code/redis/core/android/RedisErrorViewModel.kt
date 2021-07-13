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
    reducers: List<StateReducer>,
    triggers: List<StateTrigger>? = null,
    reducerSelector: ReducerSelector = DefaultReducerSelector(),
    triggerSelector: StateTriggerSelector = DefaultStateTriggerSelector(),
    listenedServiceIntentSelector: IntentSelector = DefaultIntentSelector(),
    stateStoreSelector: StateStoreSelector = DefaultStateStoreSelector(),
    savedStateHandler: SavedStateHandler? = null,
    logger: Logger = RedisCoreAndroidLogger()
) : RedisViewModel(
    savedState,
    initialState,
    mutableListOf<StateReducer>(
        ErrorStateOnReloadAfterErrorIntentReducer(logger),
        AnyStateOnReloadAfterErrorIntentReducer(logger),
    ).apply { addAll(reducers) }.toList(),
    mutableListOf<StateTrigger>(
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
        ViewStateReducer(
            ErrorState::class,
            ReloadAfterErrorIntent::class,
            logger
        ) {

        override fun reduce(currentState: State, intent: IntentMessage): Flow<State> {
            return flow {
                emit(ReloadingAfterErrorState(currentState.cast<ViewModelStateWithEvent>().viewState.castOrNull()))
            }
        }
    }

    class AnyStateOnReloadAfterErrorIntentReducer(logger: Logger) :
        ViewStateReducer(
            null,
            ReloadAfterErrorIntent::class,
            logger
        ) {

        override fun reduce(currentState: State, intent: IntentMessage): Flow<State>? {
            return null
        }
    }

    //endregion

    //region Triggers

    class AnyStateToErrorOccurredStateTrigger(logger: Logger) : StateTrigger(
        null, State.ErrorOccurred::class,
        logger
    ) {
        override fun getTriggerIntent(oldState: State, newState: State): IntentMessage {
            return OnServiceErrorIntent(newState.cast())
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