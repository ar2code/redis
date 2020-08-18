package ru.ar2code.android.architecture.core.prepares

import kotlinx.coroutines.*
import ru.ar2code.android.architecture.core.models.IntentMessage
import ru.ar2code.android.architecture.core.models.ServiceResult
import ru.ar2code.android.architecture.core.services.ActorService
import ru.ar2code.android.architecture.core.services.ActorServiceState
import ru.ar2code.android.architecture.core.services.ServiceStateWithResult
import ru.ar2code.utils.Logger

@ExperimentalCoroutinesApi
class ServiceNotAllowAnyResult(
    scope: CoroutineScope, dispatcher: CoroutineDispatcher
) :
    ActorService<String>(scope, dispatcher, object : Logger("Test") {
        override fun info(msg: String) {
        }

        override fun error(msg: String, t: Throwable) {
        }

        override fun warning(msg: String) {
        }
    }) {

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