/*
 * Copyright (c) 2020.  The Redis Open Source Project
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

package ru.ar2code.android.service_save_state_demo

import kotlinx.coroutines.CoroutineScope
import ru.ar2code.redis.core.IntentMessage
import ru.ar2code.redis.core.models.ServiceResult
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.SavedStateStore
import ru.ar2code.redis.core.services.ServiceStateWithResult
import ru.ar2code.redis.core.defaults.DefaultServiceRedisCoroutine

class MainServiceRedisCoroutine(scope: CoroutineScope, savedStateStore: SavedStateStore) : DefaultServiceRedisCoroutine<String>(scope, savedStateStore) {

    companion object {
        private const val SAVE_KEY = "state"
    }

    class MainServiceIntentType : IntentMessage.IntentMessageType<String>()

    override suspend fun onIntentMsg(msg: IntentMessage): ServiceStateWithResult<String>? {

        savedStateStore?.set(SAVE_KEY, "intent")

        return ServiceStateWithResult(
            State.Same(),
            ServiceResult.BasicResult("Some result for user button click.")
        )
    }

    override fun getResultFotInitializedState(): ServiceResult<String> {
        return ServiceResult.InitResult("Initial result. No state was saved.")
    }

    override fun restoreState() {
        super.restoreState()

        val state = savedStateStore?.get<String>(SAVE_KEY)
        state?.let {
            logger.info("Restore MainService state. Send intent.")
            sendIntent(IntentMessage(MainServiceIntentType()))
        }
    }

}