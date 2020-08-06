package ru.ar2code.android.architecture.core.android

import androidx.lifecycle.*
import ru.ar2code.android.architecture.core.android.impl.ActorViewModelService
import ru.ar2code.android.architecture.core.android.impl.ActorViewModelServiceResult
import ru.ar2code.android.architecture.core.models.IntentMessage
import ru.ar2code.android.architecture.core.models.ServiceResult
import ru.ar2code.android.architecture.core.services.ActorServiceState
import ru.ar2code.android.architecture.core.services.ServiceResultWithState
import ru.ar2code.android.architecture.core.services.ServiceSubscriber
import ru.ar2code.mutableliveevent.EventArgs
import ru.ar2code.mutableliveevent.MutableLiveEvent
import ru.ar2code.utils.Logger

abstract class ActorViewModel<ViewState, ViewEvent>(
    protected val logger: Logger
) :
    ViewModel() where ViewState : EmptyViewState, ViewEvent : BaseViewEvent {

    private val viewModelService = ActorViewModelService(
        viewModelScope,
        ::onIntentMsg,
        ::provideIntentHandlingResult,
        ::canChangeState,
        ::onIntentHandlingFinished
    )

    private val viewStateLiveMutable = MutableLiveData<ViewState>()
    val viewStateLive: LiveData<ViewState> =
        Transformations.map(viewStateLiveMutable) { input -> input }

    private val viewEventLiveMutable = MutableLiveEvent<EventArgs<ViewEvent>>()
    val viewEventLive: LiveData<EventArgs<ViewEvent>> =
        Transformations.map(viewEventLiveMutable) { input -> input }

    protected abstract suspend fun onIntentMsg(msg: IntentMessage)

    protected abstract fun provideIntentHandlingResult(): ServiceResultWithState<ActorViewModelServiceResult<ViewState, ViewEvent>>

    protected open fun canChangeState(
        newServiceState: ActorServiceState,
        result: ServiceResult<ActorViewModelServiceResult<ViewState, ViewEvent>>
    ): Boolean = true

    protected open fun onIntentHandlingFinished() {}

    init {
        subscribeToServiceResults()
    }

    fun sendIntent(msg: IntentMessage) {
        viewModelService.sendIntent(msg)
    }

    private fun subscribeToServiceResults() {

        viewModelService.subscribe(object :
            ServiceSubscriber<ActorViewModelServiceResult<ViewState, ViewEvent>> {

            override fun onReceive(result: ServiceResult<ActorViewModelServiceResult<ViewState, ViewEvent>>?) {
                logger.info("ROZHKOV view model got result from own service $result")

                viewStateLiveMutable.postValue(result?.payload?.viewState)
                viewEventLiveMutable.postValue(EventArgs(result?.payload?.viewEvent))
            }

        })
    }
}