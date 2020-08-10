package ru.ar2code.demo.impl

import ru.ar2code.android.architecture.core.android.ActorViewModel
import ru.ar2code.android.architecture.core.android.BaseViewEvent
import ru.ar2code.android.architecture.core.android.EmptyViewState
import ru.ar2code.android.architecture.core.android.ViewEventType
import ru.ar2code.android.architecture.core.android.impl.ActorViewModelServiceResult
import ru.ar2code.android.architecture.core.android.impl.ActorViewModelServiceResultValue
import ru.ar2code.android.architecture.core.models.IntentMessage
import ru.ar2code.android.architecture.core.models.ServiceResult
import ru.ar2code.android.architecture.core.services.ActorServiceState
import ru.ar2code.android.architecture.core.services.ServiceStateWithResult
import ru.ar2code.defaults.DefaultLogger

class DemoViewModel :
    ActorViewModel<DemoViewModel.DemoViewState, DemoViewModel.DemoViewEvent>(DefaultLogger()) {

    class DemoViewState(val name: String?) : EmptyViewState()

    class DemoViewEvent(viewEventType: DemoViewEventType) : BaseViewEvent(viewEventType)

    class DemoViewEventType : ViewEventType<String>()

    override suspend fun onIntentMsg(msg: IntentMessage): ServiceStateWithResult<ActorViewModelServiceResult<DemoViewState, DemoViewEvent>> {
        logger.info("rozhkov DemoViewModel onIntentMsg = $msg")
        val r = ActorViewModelServiceResult(
            DemoViewState("test"),
            DemoViewEvent(DemoViewEventType())
        )
        val sr = ActorViewModelServiceResultValue(r)
        return ServiceStateWithResult(ActorServiceState.Same(), sr)
    }

    override fun canChangeState(
        newServiceState: ActorServiceState,
        result: ServiceResult<ActorViewModelServiceResult<DemoViewState, DemoViewEvent>>
    ): Boolean {
        return super.canChangeState(newServiceState, result)
    }

}