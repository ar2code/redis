package ru.ar2code.demo.impl

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.inject
import ru.ar2code.android.architecture.core.android.ActorViewModel
import ru.ar2code.android.architecture.core.android.BaseViewEvent
import ru.ar2code.android.architecture.core.android.ChangeableState
import ru.ar2code.android.architecture.core.android.ViewEventType
import ru.ar2code.android.architecture.core.android.impl.ViewModelServiceResult
import ru.ar2code.android.architecture.core.android.impl.ViewModelStateWithEvent
import ru.ar2code.android.architecture.core.models.IntentMessage
import ru.ar2code.android.architecture.core.services.ActorServiceState
import ru.ar2code.android.architecture.core.services.ServiceStateWithResult
import ru.ar2code.defaults.DefaultLogger

class DemoViewModel :
    ActorViewModel<DemoViewModel.DemoViewState, DemoViewModel.DemoViewEvent>(DefaultLogger()) {

    class DemoViewState(
        state: String, isChangedSincePrevious: Boolean
    ) : ChangeableState<String>(state, isChangedSincePrevious)

    class DemoViewEvent(viewEventType: DemoViewEventType) : BaseViewEvent(viewEventType)

    class DemoViewEventType : ViewEventType<String>()

    val injService : AbstractDemoService by inject(AbstractDemoService::class.java) { parametersOf(viewModelScope) }

    init {
        val state = injService.serviceState
        viewModelScope.launch {
            sendIntent(IntentMessage(ActionOneIntentMsg("1")))
            repeat(10){
                delay(1000)
                sendIntent(IntentMessage(ActionOneIntentMsg("1")))
            }
            delay(1000)
            sendIntent(IntentMessage(ActionOneIntentMsg("2")))
        }
    }

    override suspend fun onIntentMsg(msg: IntentMessage): ServiceStateWithResult<ViewModelStateWithEvent<DemoViewState, DemoViewEvent>> {
        logger.info("rozhkov DemoViewModel onIntentMsg = $msg")

        val newState = (msg.msgType as ActionOneIntentMsg).payload.orEmpty()
        val lastState = this.viewStateLive.value
        val isChanged = lastState == null || lastState.state != newState

        val sr = ViewModelServiceResult(
            ViewModelStateWithEvent<DemoViewState, DemoViewEvent>(
                DemoViewState(newState, isChanged),
                null
            )
        )
        return ServiceStateWithResult(ActorServiceState.Same(), sr)
    }
}