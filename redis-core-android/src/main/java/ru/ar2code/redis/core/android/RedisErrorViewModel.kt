package ru.ar2code.redis.core.android

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.ar2code.redis.core.*
import ru.ar2code.redis.core.coroutines.*
import ru.ar2code.utils.Logger

//todo test
/**
 * When unhandled exception occurred inside view model (reducer, trigger, etc), view model gets [State.ErrorOccurred] as state
 * and dispatches [OnViewModelErrorIntent] intent with error information. You should add reducer to handle [OnViewModelErrorIntent].
 */
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
            return OnViewModelErrorIntent(newState.cast())
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
     * Intent with error state from some Redis service (listening service or internal view model service).
     */
    class OnListeningServiceErrorIntent(val errorState: State.ErrorOccurred) :
        IntentMessage() {
        companion object {
            fun createBuilder(): StateIntentMessageBuilder {
                return object : StateIntentMessageBuilder {
                    override fun build(state: State): IntentMessage {
                        return OnListeningServiceErrorIntent(state.cast())
                    }
                }
            }
        }
    }

    /**
     * Intent with error that occurred inside view model.
     */
    class OnViewModelErrorIntent(val errorState: State.ErrorOccurred) : IntentMessage()

    /**
     * Intent indicates that view model should try to restart a whole work process.
     */
    class ReloadAfterErrorIntent : IntentMessage()

    //endregion

    /**
     * Adds variant: [State.ErrorOccurred] to [OnListeningServiceErrorIntent] to [serviceStateListener].
     *
     * When listening service emits [State.ErrorOccurred] view model dispatches [OnListeningServiceErrorIntent] intent.
     *
     * If you need to add custom variant to handle listening service error, use [listen].
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

    private fun extendWithErrorStateListener(serviceStateListener: ServiceStateListener): ServiceStateListener {
        val mapWithErrorState = serviceStateListener.stateIntentMap.toMutableMap().apply {
            put(State.ErrorOccurred::class, OnListeningServiceErrorIntent.createBuilder())
        }

        return ServiceStateListener(serviceStateListener.listeningService, mapWithErrorState)
    }
}