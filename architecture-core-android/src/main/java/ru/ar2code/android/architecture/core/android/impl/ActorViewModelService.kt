package ru.ar2code.android.architecture.core.android.impl

import kotlinx.coroutines.CoroutineScope
import ru.ar2code.android.architecture.core.android.BaseViewEvent
import ru.ar2code.android.architecture.core.android.BaseViewState
import ru.ar2code.android.architecture.core.models.IntentMessage
import ru.ar2code.android.architecture.core.models.ServiceResult
import ru.ar2code.android.architecture.core.services.ActorServiceState
import ru.ar2code.android.architecture.core.services.ServiceStateWithResult
import ru.ar2code.defaults.DefaultActorService

class ActorViewModelService<ViewState, ViewEvent>(
    scope: CoroutineScope,
    private val onIntentMsgCallback: (suspend (IntentMessage) -> ServiceStateWithResult<ViewModelStateWithEvent<ViewState, ViewEvent>>)?,
    private val canChangeStateCallback: ((ActorServiceState, ServiceResult<ViewModelStateWithEvent<ViewState, ViewEvent>>) -> Boolean)?
) :
    DefaultActorService<ViewModelStateWithEvent<ViewState, ViewEvent>>(
        scope
    ) where ViewState : BaseViewState, ViewEvent : BaseViewEvent {

    override suspend fun onIntentMsg(msg: IntentMessage): ServiceStateWithResult<ViewModelStateWithEvent<ViewState, ViewEvent>>? {
        return onIntentMsgCallback?.invoke(msg)
    }

    override fun canChangeState(
        newServiceState: ActorServiceState,
        result: ServiceResult<ViewModelStateWithEvent<ViewState, ViewEvent>>
    ): Boolean {
        return canChangeStateCallback?.invoke(newServiceState, result) ?: true
    }
}