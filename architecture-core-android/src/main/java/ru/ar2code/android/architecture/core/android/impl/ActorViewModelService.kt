package ru.ar2code.android.architecture.core.android.impl

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import ru.ar2code.android.architecture.core.android.BaseViewEvent
import ru.ar2code.android.architecture.core.android.BaseViewState
import ru.ar2code.android.architecture.core.models.IntentMessage
import ru.ar2code.android.architecture.core.models.ServiceResult
import ru.ar2code.android.architecture.core.services.ActorService
import ru.ar2code.android.architecture.core.services.ActorServiceState
import ru.ar2code.android.architecture.core.services.ServiceStateWithResult
import ru.ar2code.defaults.DefaultActorService
import ru.ar2code.utils.Logger

@ExperimentalCoroutinesApi
internal class ActorViewModelService<ViewState, ViewEvent>(
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher,
    logger: Logger,
    private val onIntentMsgCallback: (suspend (IntentMessage) -> ServiceStateWithResult<ViewModelStateWithEvent<ViewState, ViewEvent>>)?,
    private val canChangeStateCallback: ((ActorServiceState, ServiceResult<ViewModelStateWithEvent<ViewState, ViewEvent>>) -> Boolean)?
) :
    ActorService<ViewModelStateWithEvent<ViewState, ViewEvent>>(
        scope, dispatcher, null, logger
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