package ru.ar2code.android.architecture.core.prepares

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import ru.ar2code.android.architecture.core.models.IntentMessage
import ru.ar2code.android.architecture.core.models.ServiceResult
import ru.ar2code.android.architecture.core.services.ActorService
import ru.ar2code.android.architecture.core.services.ActorServiceState
import ru.ar2code.utils.Logger
import ru.ar2code.utils.impl.ConsoleLogger

class SimpleService(scope: CoroutineScope, dispatcher: CoroutineDispatcher) :
    ActorService<String>(scope, dispatcher, object : Logger("Test") {
        override fun info(msg: String) {
        }

        override fun error(msg: String, t: Throwable) {
        }

        override fun warning(msg: String) {
        }
    }) {

    override suspend fun onIntentMsg(msg: IntentMessage) {
        provideResult(ActorServiceState.Ready(), ServiceResult.BasicResult<String>(msg.msgType.payload?.toString()))
    }

    class SimpleIntentType(payload : String? = null) : IntentMessage.IntentMessageType<String>(payload)
}