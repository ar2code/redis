package ru.ar2code.android.architecture.core.prepares

import kotlinx.coroutines.*
import ru.ar2code.android.architecture.core.models.IntentMessage
import ru.ar2code.android.architecture.core.models.ServiceResult
import ru.ar2code.android.architecture.core.services.ActorService
import ru.ar2code.android.architecture.core.services.ActorServiceState
import ru.ar2code.android.architecture.core.services.ServiceStateWithResult
import ru.ar2code.utils.Logger

@ExperimentalCoroutinesApi
class SimpleService(
    scope: CoroutineScope, dispatcher: CoroutineDispatcher,
    private val canChangeStateCallback: ((newServiceState: ActorServiceState, result: ServiceResult<String>) -> Boolean)? = { _,_ -> true },
    private val newStateCallback: (() -> ActorServiceState)? = { ActorServiceState.Same() }
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
            newStateCallback?.invoke() ?: ActorServiceState.Same(),
            ServiceResult.BasicResult(msg.msgType.payload?.toString())
        )
    }

    override fun canChangeState(
        newServiceState: ActorServiceState,
        result: ServiceResult<String>
    ): Boolean {
        return canChangeStateCallback?.invoke(newServiceState, result) ?: true
    }

    override fun getResultFotInitializedState(): ServiceResult<String> {
        return SimpleEmptyResult()
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