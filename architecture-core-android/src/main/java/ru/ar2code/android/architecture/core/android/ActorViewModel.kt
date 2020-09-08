package ru.ar2code.android.architecture.core.android

import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PROTECTED
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import ru.ar2code.android.architecture.core.android.impl.ActorViewModelService
import ru.ar2code.android.architecture.core.android.impl.ViewModelStateWithEvent
import ru.ar2code.android.architecture.core.models.IntentMessage
import ru.ar2code.android.architecture.core.models.ServiceResult
import ru.ar2code.android.architecture.core.services.ActorServiceState
import ru.ar2code.android.architecture.core.services.ServiceStateWithResult
import ru.ar2code.android.architecture.core.services.ServiceSubscriber
import ru.ar2code.mutableliveevent.EventArgs
import ru.ar2code.mutableliveevent.MutableLiveEvent
import ru.ar2code.utils.Logger

abstract class ActorViewModel<ViewState, ViewEvent>(
    protected val logger: Logger
) :
    ViewModel() where ViewState : BaseViewState, ViewEvent : BaseViewEvent {

    private val viewModelService = ActorViewModelService(
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
     * If [result.payload.viewState] is not null set to [viewStateLive]
     * If [result.payload.viewEvent] is not null set to [viewEventLive]
     */
    protected open fun postResult(result: ServiceStateWithResult<ViewModelStateWithEvent<ViewState, ViewEvent>>?) {
        logger.info("[ActorViewModel] view model got result from own service $result")

            result?.result?.payload?.viewState?.let {
            viewStateLiveMutable.postValue(it)
        } ?: kotlin.run {
            logger.info("[ActorViewModel] viewState is null. No post value to live data {viewEventLive}.")
        }

        result?.result?.payload?.viewEvent?.let {
            viewEventLiveMutable.postValue(EventArgs(it))
        } ?: kotlin.run {
            logger.info("[ActorViewModel] viewEvent is null. No post value to live event {viewEventLive}.")
        }
    }

    private fun subscribeToServiceResults() {

        logger.info("[ActorViewModel] subscribe to internal service")

        viewModelService.subscribe(object :
            ServiceSubscriber<ViewModelStateWithEvent<ViewState, ViewEvent>> {

            override fun onReceive(result: ServiceStateWithResult<ViewModelStateWithEvent<ViewState, ViewEvent>>?) {
                postResult(result)
            }
        })
    }
}