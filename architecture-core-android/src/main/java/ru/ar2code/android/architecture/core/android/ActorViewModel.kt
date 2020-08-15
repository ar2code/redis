package ru.ar2code.android.architecture.core.android

import androidx.lifecycle.*
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
        ::onIntentMsg,
        ::canChangeState
    )

    private val viewStateLiveMutable = MutableLiveData<ViewState>()
    val viewStateLive: LiveData<ViewState> = viewStateLiveMutable

    private val viewEventLiveMutable = MutableLiveEvent<EventArgs<ViewEvent>>()
    val viewEventLive: LiveData<EventArgs<ViewEvent>> = viewEventLiveMutable

    init {
        subscribeToServiceResults()
    }

    protected abstract suspend fun onIntentMsg(msg: IntentMessage) : ServiceStateWithResult<ViewModelStateWithEvent<ViewState, ViewEvent>>

    protected open fun canChangeState(
        newServiceState: ActorServiceState,
        withEvent: ServiceResult<ViewModelStateWithEvent<ViewState, ViewEvent>>
    ): Boolean = true

    fun sendIntent(msg: IntentMessage) {
        logger.info("[ActorViewModel] send intent $msg")

        viewModelService.sendIntent(msg)
    }

    private fun subscribeToServiceResults() {

        logger.info("[ActorViewModel] subscribe to internal service")

        viewModelService.subscribe(object :
            ServiceSubscriber<ViewModelStateWithEvent<ViewState, ViewEvent>> {

            override fun onReceive(result: ServiceResult<ViewModelStateWithEvent<ViewState, ViewEvent>>?) {
                logger.info("[ActorViewModel] view model got result from own service $result")

                result?.payload?.viewState?.let {
                    viewStateLiveMutable.postValue(it)
                } ?: kotlin.run {
                    logger.info("[ActorViewModel] viewState is null. No post value to live data {viewEventLive}.")
                }

                result?.payload?.viewEvent?.let {
                    viewEventLiveMutable.postValue(EventArgs(it))
                }?: kotlin.run {
                    logger.info("[ActorViewModel] viewEvent is null. No post value to live event {viewEventLive}.")
                }
            }
        })
    }
}