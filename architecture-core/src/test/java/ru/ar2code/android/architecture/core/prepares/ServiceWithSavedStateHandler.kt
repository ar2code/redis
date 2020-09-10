/*
 * Copyright (c) 2020. Created by Alexey Rozhkov.
 */

package ru.ar2code.android.architecture.core.prepares

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import ru.ar2code.android.architecture.core.models.IntentMessage
import ru.ar2code.android.architecture.core.models.ServiceResult
import ru.ar2code.android.architecture.core.services.ActorService
import ru.ar2code.android.architecture.core.services.ServiceStateWithResult

@ExperimentalCoroutinesApi
class ServiceWithSavedStateHandler(
    scope: CoroutineScope, dispatcher: CoroutineDispatcher
) :
    ActorService<String>(scope, dispatcher, null, SimpleTestLogger()) {

    companion object {
        const val SAVE_KEY = "key"
    }

    override suspend fun onIntentMsg(msg: IntentMessage): ServiceStateWithResult<String>? {
        val payload = msg.msgType.payload?.toString()
        savedStateHandler?.set(SAVE_KEY, payload)

        return ServiceStateWithResult(
            SimpleService.SimpleState(),
            ServiceResult.BasicResult(payload)
        )
    }

    override fun restoreState() {
        super.restoreState()

        val data = savedStateHandler?.get<String>(SAVE_KEY)
        data?.let {
            sendIntent(IntentMessage(SimpleService.SimpleIntentType(it)))
        }
    }

}