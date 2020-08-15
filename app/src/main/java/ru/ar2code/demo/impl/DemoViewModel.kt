package ru.ar2code.demo.impl

import ru.ar2code.android.architecture.core.android.ActorViewModel
import ru.ar2code.android.architecture.core.android.BaseViewEvent
import ru.ar2code.android.architecture.core.android.BaseViewState
import ru.ar2code.android.architecture.core.android.ViewEventType
import ru.ar2code.android.architecture.core.android.impl.ViewModelServiceResult
import ru.ar2code.android.architecture.core.android.impl.ViewModelStateWithEvent
import ru.ar2code.android.architecture.core.models.IntentMessage
import ru.ar2code.android.architecture.core.services.ActorServiceState
import ru.ar2code.android.architecture.core.services.ServiceStateWithResult
import ru.ar2code.defaults.DefaultLogger

class DemoViewModel :
    ActorViewModel<DemoViewModel.DemoViewState, DemoViewModel.DemoViewEvent>(DefaultLogger()) {

    class DemoViewState(val name: String?) : BaseViewState()

    class DemoViewEvent(viewEventType: DemoViewEventType) : BaseViewEvent(viewEventType)

    class DemoViewEventType : ViewEventType<String>()

    init {
        sendIntent(IntentMessage(ActionOneIntentMsg("my test")))
    }

    override suspend fun onIntentMsg(msg: IntentMessage): ServiceStateWithResult<ViewModelStateWithEvent<DemoViewState, DemoViewEvent>> {
        logger.info("rozhkov DemoViewModel onIntentMsg = $msg")

        val sr = ViewModelServiceResult(
            ViewModelStateWithEvent<DemoViewState, DemoViewEvent>(
                DemoViewState("test"),
                null
            )
        )
        return ServiceStateWithResult(ActorServiceState.Same(), sr)
    }
}