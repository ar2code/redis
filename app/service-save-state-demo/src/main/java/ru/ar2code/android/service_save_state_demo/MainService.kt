/*
 * Copyright (c) 2020. Created by Alexey Rozhkov.
 */

package ru.ar2code.android.service_save_state_demo

import kotlinx.coroutines.CoroutineScope
import ru.ar2code.android.architecture.core.models.IntentMessage
import ru.ar2code.android.architecture.core.models.ServiceResult
import ru.ar2code.android.architecture.core.services.ActorServiceState
import ru.ar2code.android.architecture.core.services.ServiceSavedStateHandler
import ru.ar2code.android.architecture.core.services.ServiceStateWithResult
import ru.ar2code.defaults.DefaultActorService

class MainService(scope: CoroutineScope, savedStateHandler: ServiceSavedStateHandler) : DefaultActorService<String>(scope, savedStateHandler) {

    companion object {
        private const val SAVE_KEY = "state"
    }

    class MainServiceIntentType : IntentMessage.IntentMessageType<String>()

    override suspend fun onIntentMsg(msg: IntentMessage): ServiceStateWithResult<String>? {

        savedStateHandler?.set(SAVE_KEY, "intent")

        return ServiceStateWithResult(
            ActorServiceState.Same(),
            ServiceResult.BasicResult("Some result for user button click.")
        )
    }

    override fun getResultFotInitializedState(): ServiceResult<String> {
        return ServiceResult.InitResult("Initial result. No state was saved.")
    }

    override fun restoreState() {
        super.restoreState()

        val state = savedStateHandler?.get<String>(SAVE_KEY)
        state?.let {
            logger.info("Restore MainService state. Send intent.")
            sendIntent(IntentMessage(MainServiceIntentType()))
        }
    }

}