/*
 * Copyright (c) 2020.  The Redim Open Source Project
 * Author: Alexey Rozhkov https://github.com/ar2code
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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