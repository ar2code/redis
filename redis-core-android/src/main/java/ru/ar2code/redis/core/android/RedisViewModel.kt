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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import ru.ar2code.redis.core.IntentMessage
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.ServiceSubscriber
import ru.ar2code.mutableliveevent.EventArgs
import ru.ar2code.mutableliveevent.MutableLiveEvent
import ru.ar2code.redis.core.SavedStateHandler
import ru.ar2code.redis.core.android.ext.toRedisSavedStateStore
import ru.ar2code.redis.core.coroutines.*
import ru.ar2code.utils.Logger

@ExperimentalCoroutinesApi
abstract class RedisViewModel<ViewState, ViewEvent>(
    protected val savedState: SavedStateHandle?
) :
    ViewModel() where ViewState : BaseViewState, ViewEvent : BaseViewEvent {

    protected abstract val initialState: ViewModelStateWithEvent<ViewState, ViewEvent>

    protected abstract val reducers: List<ViewStateReducer<ViewState, ViewEvent>>

    protected open val triggers: List<ViewStateTrigger<ViewState, ViewEvent>>? = null

    protected open val reducerSelector: ReducerSelector = DefaultReducerSelector()

    protected open val triggerSelector: StateTriggerSelector = DefaultStateTriggerSelector()

    protected open val savedStateHandler: SavedStateHandler? = null

    protected open val logger: Logger = RedisCoreAndroidLogger()

    private val viewModelService by lazy {
        RedisSavedStateService(
            viewModelScope,
            Dispatchers.Default,
            initialState,
            reducers,
            reducerSelector,
            triggers,
            triggerSelector,
            logger,
            savedState?.toRedisSavedStateStore(),
            savedStateHandler
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

    /**
     * Send some intent for changing view model state.
     * UI uses this method for communicating with internal services and use cases.
     */
    fun dispatch(msg: IntentMessage) {
        logger.info("[ActorViewModel] dispatch intent $msg")

        viewModelService.dispatch(msg)
    }

    /**
     * Set result from IntentMessage if reducer return [ViewModelStateWithEvent] state
     * If [newState.viewState] is not null set to [viewStateLive]
     * If [newState.viewEvent] is not null set to [viewEventLive]
     */
    protected open fun postResult(newState: ViewModelStateWithEvent<ViewState, ViewEvent>) {
        logger.info("[ActorViewModel] view model got result from own service $newState")

        newState.viewState?.let {
            viewStateLiveMutable.postValue(it)
        } ?: kotlin.run {
            logger.info("[ActorViewModel] viewState is null. No post value to live data {viewEventLive}.")
        }

        newState.viewEvent?.let {
            viewEventLiveMutable.postValue(EventArgs(it))
        } ?: kotlin.run {
            logger.info("[ActorViewModel] viewEvent is null. No post value to live event {viewEventLive}.")
        }
    }

    private fun subscribeToServiceResults() {

        logger.info("[ActorViewModel] subscribe to internal service")

        viewModelService.subscribe(object : ServiceSubscriber {
            override fun onReceive(newState: State) {
                val viewModelState = newState as? ViewModelStateWithEvent<ViewState, ViewEvent>
                viewModelState?.let {
                    postResult(viewModelState)
                }
            }
        })
    }
}