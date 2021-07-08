package ru.ar2code.redis.core.android

import androidx.lifecycle.SavedStateHandle
import ru.ar2code.redis.core.SavedStateHandler
import ru.ar2code.redis.core.ServiceStateListener
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.StateStoreSelector
import ru.ar2code.redis.core.coroutines.*
import ru.ar2code.utils.Logger

abstract class RedisErrorViewModel<ViewState, ViewEvent>(
    savedState: SavedStateHandle?,
    initialState: ViewModelStateWithEvent<ViewState, ViewEvent>,
    reducers: List<ViewStateReducer<ViewState, ViewEvent>>,
    triggers: List<ViewStateTrigger<ViewState, ViewEvent>>? = null,
    reducerSelector: ReducerSelector = DefaultReducerSelector(),
    triggerSelector: StateTriggerSelector = DefaultStateTriggerSelector(),
    listenedServiceIntentSelector: IntentSelector = DefaultIntentSelector(),
    stateStoreSelector: StateStoreSelector = DefaultStateStoreSelector(),
    savedStateHandler: SavedStateHandler? = null,
    logger: Logger = RedisCoreAndroidLogger()
) : RedisViewModel<ViewState, ViewEvent>(
    savedState,
    initialState,
    reducers,
    triggers,
    reducerSelector,
    triggerSelector,
    listenedServiceIntentSelector,
    stateStoreSelector,
    savedStateHandler,
    logger
) where ViewState : RedisErrorViewState, ViewEvent : RedisViewEvent {

    /**
     * Adds variant: [State.ErrorOccurred] to [OnServiceErrorIntent] to [serviceStateListener]
     *
     * When listening service emits [State.ErrorOccurred] view model dispatches [OnServiceErrorIntent] intent.
     */
    fun listenWithErrorHandling(serviceStateListener: ServiceStateListener) {
        listen(extendWithErrorStateListener(serviceStateListener))
    }

    //todo test
    internal fun extendWithErrorStateListener(serviceStateListener: ServiceStateListener): ServiceStateListener {
        val mapWithErrorState = serviceStateListener.stateIntentMap.toMutableMap().apply {
            put(State.ErrorOccurred::class, OnServiceErrorIntent.createBuilder())
        }

        return ServiceStateListener(serviceStateListener.listeningService, mapWithErrorState)
    }
}