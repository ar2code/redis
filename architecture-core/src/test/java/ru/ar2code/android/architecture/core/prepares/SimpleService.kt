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
class SimpleService(
    scope: CoroutineScope, dispatcher: CoroutineDispatcher
) :
    ActorService<String>(scope, dispatcher, null, SimpleTestLogger()) {

    override suspend fun onIntentMsg(msg: IntentMessage): ServiceStateWithResult<String>? {
        return ServiceStateWithResult(
            SimpleState(),
            ServiceResult.BasicResult(msg.msgType.payload?.toString())
        )
    }

    class SimpleIntentType(payload: String? = null) :
        IntentMessage.IntentMessageType<String>(payload)

    class SimpleEmptyResult : ServiceResult.EmptyResult<String>(SIMPLE_EMPTY) {

        companion object {
            const val SIMPLE_EMPTY = "SIMPLE_EMPTY"
        }
    }

    class SimpleState : ActorServiceState()
}