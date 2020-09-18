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
import ru.ar2code.redis.core.android.impl.CoroutineActorViewModelService
import ru.ar2code.redis.core.android.impl.ViewModelStateWithEvent
import ru.ar2code.redis.core.models.IntentMessage
import ru.ar2code.redis.core.models.ServiceResult
import ru.ar2code.redis.core.services.ActorServiceState
import ru.ar2code.redis.core.services.ServiceStateWithResult
import ru.ar2code.redis.core.services.ServiceSubscriber
import ru.ar2code.mutableliveevent.EventArgs
import ru.ar2code.mutableliveevent.MutableLiveEvent
import ru.ar2code.utils.Logger

abstract class ActorViewModel<ViewState, ViewEvent>(
    protected val logger: Logger
) :
    ViewModel() where ViewState : BaseViewState, ViewEvent : BaseViewEvent {

    private val viewModelService = CoroutineActorViewModelService(
        viewModelScope,
        Dispatchers.Default,
        logger,
        ::onIntentMsg,
        ::canChangeState
    )

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
    val state: ActorServiceState
        get() = viewModelService.serviceState

    init {
        subscribeToServiceResults()
    }

    /**
     * Handle each intent within this method
     * @return is a pair that consists of a new internal viewModel [state] and UI result that consists of UI State and Event.
     */
    protected abstract suspend fun onIntentMsg(msg: IntentMessage): ServiceStateWithResult<ViewModelStateWithEvent<ViewState, ViewEvent>>

    /**
     * Check is [newServiceState] can be applied for this ViewModel with current result [withEvent]
     * You should check current ViewModel [state] here and previous result for making decision.
     */
    protected open fun canChangeState(
        newServiceState: ActorServiceState,
        withEvent: ServiceResult<ViewModelStateWithEvent<ViewState, ViewEvent>>
    ): Boolean = true

    /**
     * Send some intent for changing view model state.
     * UI uses this method for communicating with internal services and use cases.
     */
    fun sendIntent(msg: IntentMessage) {
        logger.info("[ActorViewModel] send intent $msg")

        viewModelService.sendIntent(msg)
    }

    /**
     * Set result from [onIntentMsg]
     * If [stateWithResult.payload.viewState] is not null set to [viewStateLive]
     * If [stateWithResult.payload.viewEvent] is not null set to [viewEventLive]
     */
    protected open fun postResult(stateWithResult: ServiceStateWithResult<ViewModelStateWithEvent<ViewState, ViewEvent>>?) {
        logger.info("[ActorViewModel] view model got result from own service $stateWithResult")

            stateWithResult?.result?.payload?.viewState?.let {
            viewStateLiveMutable.postValue(it)
        } ?: kotlin.run {
            logger.info("[ActorViewModel] viewState is null. No post value to live data {viewEventLive}.")
        }

        stateWithResult?.result?.payload?.viewEvent?.let {
            viewEventLiveMutable.postValue(EventArgs(it))
        } ?: kotlin.run {
            logger.info("[ActorViewModel] viewEvent is null. No post value to live event {viewEventLive}.")
        }
    }

    private fun subscribeToServiceResults() {

        logger.info("[ActorViewModel] subscribe to internal service")

        viewModelService.subscribe(object :
            ServiceSubscriber<ViewModelStateWithEvent<ViewState, ViewEvent>> {

            override fun onReceive(stateWithResult: ServiceStateWithResult<ViewModelStateWithEvent<ViewState, ViewEvent>>?) {
                postResult(stateWithResult)
            }
        })
    }
}