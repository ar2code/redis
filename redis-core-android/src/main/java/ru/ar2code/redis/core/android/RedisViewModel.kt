/*
 * Copyright (c) 2020.  The Redis Open Source Project
 * Author: Alexey Rozhkov https://github.com/ar2code
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.ar2code.redis.core.android

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import ru.ar2code.mutableliveevent.EventArgs
import ru.ar2code.mutableliveevent.MutableLiveEvent
import ru.ar2code.redis.core.*
import ru.ar2code.redis.core.android.ext.toRedisSavedStateStore
import ru.ar2code.redis.core.coroutines.*
import ru.ar2code.utils.LoggableObject
import ru.ar2code.utils.Logger

/**
 * Android ViewModel with Actor behaviour.
 *
 * To change ViewModel state you should dispatch some IntentMessage.
 *
 * ViewModel State consists from viewStateLive and viewEventLive.
 * viewStateLive is a live data with UI model. Any observer will receive this state immediately after starting observing.
 *
 * viewEventLive is a live data with UI event. Only active observers will receive new state.
 * If you start observe event after it occurred you will not receive it (miss an event).
 * viewEventLive should use for events that occurred sometimes, like showing some toasts.
 *
 * You can read more about LiveEvent here https://github.com/ar2code/MutableLiveEvent
 */
abstract class RedisViewModel(
    protected val savedState: SavedStateHandle?,
    protected val initialState: ViewModelStateWithEvent,
    protected val reducers: List<StateReducer>,
    protected val triggers: List<StateTrigger>? = null,
    protected val reducerSelector: ReducerSelector = DefaultReducerSelector(),
    protected val triggerSelector: StateTriggerSelector = DefaultStateTriggerSelector(),
    protected val listenedServiceIntentSelector: IntentSelector = DefaultIntentSelector(),
    protected val stateStoreSelector: StateStoreSelector = DefaultStateStoreSelector(),
    protected val savedStateHandler: SavedStateHandler? = null,
    protected val logger: Logger = RedisCoreAndroidLogger(),
) :
    ViewModel(), RedisDispatcher, LoggableObject, RedisListener {

    internal open val viewModelService by lazy {
        RedisCoroutineStateService(
            viewModelScope,
            Dispatchers.Default,
            initialState,
            reducers,
            reducerSelector,
            listenedServiceIntentSelector,
            triggers,
            triggerSelector,
            savedState?.toRedisSavedStateStore(),
            savedStateHandler,
            stateStoreSelector,
            logger,
            "${objectLogName}.service",
            emitExceptionAsErrorState
        )
    }

    /**
     * If true exceptions inside [StateReducer.reduce], [StateTrigger.invokeAction], [StateRestore.restoreState], will emit as [State.ErrorOccurred] state.
     */
    protected open val emitExceptionAsErrorState = false

    private val viewStateLiveMutable = MutableLiveData<RedisViewState>()

    /**
     * View state. Ui should bind and render this data.
     */
    val viewStateLive: LiveData<RedisViewState> = viewStateLiveMutable

    private val viewEventLiveMutable = MutableLiveEvent<EventArgs<RedisViewEvent>>()

    /**
     * Event that can be sent to all currently active observers.
     * Use it if you need to do some action only one time and don`t keep event as a state like show toast etc.
     */
    val viewEventLive: LiveData<EventArgs<RedisViewEvent>> = viewEventLiveMutable

    /**
     * Current state of the view model
     */
    val state: State
        get() = viewModelService.serviceState

    private val stateLiveMutable =
        MutableLiveData<ViewModelStateWithEvent>()

    /**
     * Current state [state] of the view model as liveData
     */
    val stateLive: LiveData<ViewModelStateWithEvent> =
        stateLiveMutable

    init {
        subscribeToServiceResults()
    }

    /**
     * Get count of active services that current service is listening
     */
    override fun getListenServiceCount(): Int {
        return viewModelService.getListenServiceCount()
    }

    /**
     * Send some intent for changing view model state.
     * UI uses this method for communicating with internal services and use cases.
     */
    override fun dispatch(msg: IntentMessage) {
        logger.info("[$objectLogName] dispatch intent ${msg.objectLogName}")

        viewModelService.dispatch(msg)
    }

    /**
     * Listening of state changing of another service.
     */
    override fun listen(serviceStateListener: ServiceStateListener) {
        viewModelService.listen(serviceStateListener)
    }

    /**
     * Stop listening of service state changing
     */
    override fun stopListening(serviceStateListener: ServiceStateListener) {
        viewModelService.stopListening(serviceStateListener)
    }

    /**
     * Set result from IntentMessage if reducer return [ViewModelStateWithEvent] state
     */
    protected open fun postResult(newState: ViewModelStateWithEvent) {
        logger.info("[$objectLogName] is post result to ${newState.objectLogName}")

        stateLiveMutable.postValue(newState)
        viewStateLiveMutable.postValue(newState.viewState)
        viewEventLiveMutable.postValue(EventArgs(newState.viewEvent))
    }

    override fun onCleared() {
        super.onCleared()

        viewModelService.dispose()
    }

    private fun subscribeToServiceResults() {

        logger.info("[$objectLogName] subscribe to it internal service")

        viewModelService.subscribe(object : ServiceSubscriber {
            override suspend fun onReceive(newState: State) {

                val viewModelState = newState.castOrNull<ViewModelStateWithEvent>()

                viewModelState?.let {
                    postResult(viewModelState)
                } ?: kotlin.run {
                    logger.info("[$objectLogName] cannot post result to live data. ${newState.objectLogName} is not a ViewModelStateWithEvent.")
                }
            }
        })
    }
}