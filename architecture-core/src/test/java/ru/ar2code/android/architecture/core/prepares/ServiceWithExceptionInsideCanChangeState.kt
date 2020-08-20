package ru.ar2code.android.architecture.core.prepares

import kotlinx.coroutines.*
import ru.ar2code.android.architecture.core.models.IntentMessage
import ru.ar2code.android.architecture.core.models.ServiceResult
import ru.ar2code.android.architecture.core.services.ActorService
import ru.ar2code.android.architecture.core.services.ActorServiceState
import ru.ar2code.android.architecture.core.services.ServiceStateWithResult
import ru.ar2code.utils.Logger

@ExperimentalCoroutinesApi
class ServiceWithExceptionInsideCanChangeState(
    scope: CoroutineScope, dispatcher: CoroutineDispatcher
) :
    ActorService<String>(scope, dispatcher, SimpleTestLogger()) {

    override suspend fun onIntentMsg(msg: IntentMessage): ServiceStateWithResult<String>? {
        broadcastNewStateWithResult(ServiceStateWithResult(ActorServiceState.Same(), SimpleService.SimpleEmptyResult()))
        return null
    }

    override fun canChangeState(
        newServiceState: ActorServiceState,
        result: ServiceResult<String>
    ): Boolean {
        throw ServiceWithExceptionInsideIntentHandling.TestException()
    }
}