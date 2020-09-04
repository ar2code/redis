package ru.ar2code.android.architecture.core.prepares

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import ru.ar2code.android.architecture.core.models.IntentMessage
import ru.ar2code.android.architecture.core.models.ServiceResult
import ru.ar2code.android.architecture.core.services.ActorService
import ru.ar2code.android.architecture.core.services.ActorServiceState
import ru.ar2code.android.architecture.core.services.ServiceStateWithResult

@ExperimentalCoroutinesApi
class ServiceNotAllowAnyResult(
    scope: CoroutineScope, dispatcher: CoroutineDispatcher
) :
    ActorService<String>(scope, dispatcher, null, SimpleTestLogger()) {

    override suspend fun onIntentMsg(msg: IntentMessage): ServiceStateWithResult<String>? {
        return ServiceStateWithResult(
            ActorServiceState.Same(),
            ServiceResult.BasicResult(msg.msgType.payload?.toString())
        )
    }

    override fun canChangeState(
        newServiceState: ActorServiceState,
        result: ServiceResult<String>
    ): Boolean {
        return false
    }
}