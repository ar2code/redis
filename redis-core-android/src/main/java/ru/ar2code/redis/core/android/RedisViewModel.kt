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

import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PROTECTED
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import ru.ar2code.mutableliveevent.EventArgs
import ru.ar2code.mutableliveevent.MutableLiveEvent
import ru.ar2code.redis.core.*
import ru.ar2code.redis.core.android.ext.toRedisSavedStateStore
import ru.ar2code.redis.core.coroutines.*
import ru.ar2code.redis.core.coroutines.DefaultStateStoreSelector
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
abstract class RedisViewModel<ViewState, ViewEvent>(
    protected val savedState: SavedStateHandle?
) :
    ViewModel(), RedisDispatcher, LoggableObject, RedisListener
        where ViewState : BaseViewState, ViewEvent : BaseViewEvent {

    protected abstract val initialState: ViewModelStateWithEvent<ViewState, ViewEvent>

    protected abstract val reducers: List<ViewStateReducer<ViewState, ViewEvent>>

    protected open val triggers: List<ViewStateTrigger<ViewState, ViewEvent>>? = null

    protected open val reducerSelector: ReducerSelector = DefaultReducerSelector()

    protected open val triggerSelector: StateTriggerSelector = DefaultStateTriggerSelector()

    protected open val listenedServiceIntentSelector: IntentSelector = DefaultIntentSelector()

    protected open val stateStoreSelector: StateStoreSelector = DefaultStateStoreSelector()

    protected open val savedStateHandler: SavedStateHandler? = null

    protected open val logger: Logger = RedisCoreAndroidLogger()

    private val viewModelService by lazy {
        RedisCoroutineSavedStateService(
            viewModelScope,
            Dispatchers.Default,
            initialState,
            reducers,
            reducerSelector,
            listenedServiceIntentSelector,
            triggers,
            triggerSelector,
            logger,
            "${objectLogName()}.service",
            savedState?.toRedisSavedStateStore(),
            savedStateHandler,
            stateStoreSelector

        )
    }

    private val viewStateLiveMutable = MutableLiveData<ViewState>()

    /**
     * View state. Ui should bind and render this data.
     */
    val viewStateLive: LiveData<ViewState> = viewStateLiveMutable

    private val viewEventLiveMutable = MutableLiveEvent<EventArgs<ViewEvent>>()

    /**
     * Event that can be sent to all currently active observers.
     * Use it if you need to do some action only one time and don`t keep event as a state like show toast etc.
     */
    val viewEventLive: LiveData<EventArgs<ViewEvent>> = viewEventLiveMutable

    /**
     * Current state of the view model
     */
    @VisibleForTesting(otherwise = PROTECTED)
    val state: State
        get() = viewModelService.serviceState

    init {
        subscribeToServiceResults()
    }

    override fun getListenServiceCount(): Int {
        return viewModelService.getListenServiceCount()
    }

    /**
     * Send some intent for changing view model state.
     * UI uses this method for communicating with internal services and use cases.
     */
    override fun dispatch(msg: IntentMessage) {
        logger.info("[${objectLogName()}] dispatch intent ${msg.objectLogName()}")

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
     * If viewState is not null set to [viewStateLive]
     * If viewEvent is not null set to [viewEventLive]
     */
    protected open fun postResult(newState: ViewModelStateWithEvent<ViewState, ViewEvent>) {
        logger.info("[${objectLogName()}] is changing state to ${newState.objectLogName()}")

        newState.viewState?.let {
            viewStateLiveMutable.postValue(it)
        } ?: kotlin.run {
            logger.info("[${objectLogName()}] viewState is null. No post value to live data {viewStateLive}.")
        }

        newState.viewEvent?.let {
            viewEventLiveMutable.postValue(EventArgs(it))
        } ?: kotlin.run {
            logger.info("[${objectLogName()}] viewEvent is null. No post value to live event {viewEventLive}.")
        }
    }

    private fun subscribeToServiceResults() {

        logger.info("[${objectLogName()}] subscribe to internal service")

        viewModelService.subscribe(object : ServiceSubscriber {
            override suspend fun onReceive(newState: State) {
                val viewModelState = newState as? ViewModelStateWithEvent<ViewState, ViewEvent>
                viewModelState?.let {
                    postResult(viewModelState)
                }
            }
        })
    }
}