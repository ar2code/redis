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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.ar2code.redis.core.IntentMessage
import ru.ar2code.redis.core.RestoredStateIntent
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.coroutines.*
import ru.ar2code.redis.core.defaults.DefaultLogger
import ru.ar2code.redis.core.defaults.DefaultRedisSavedStateService

class MainServiceRedisCoroutine(scope: CoroutineScope, savedStateStore: SavedStateStore) :
    DefaultRedisSavedStateService<String>(
        scope,
        listOf(MainReducer()),
        null,
        savedStateStore,
        SaveHandler()
    ) {

    class MainIntent(val timestamp: Long) : IntentMessage()

    data class KeepState(val timestamp: Long) : State() {
        override fun clone(): State {
            return this.copy(timestamp = timestamp)
        }
    }

    class MainReducer : StateReducer(null, MainIntent::class, DefaultLogger()) {
        override fun reduce(currentState: State, intent: IntentMessage): Flow<State> {
            return flow {
                val timeStamp = (intent as? MainIntent)?.timestamp
                emit(KeepState(timeStamp!!))
            }
        }
    }

    class SaveHandler : SavedStateHandler {
        override suspend fun storeState(state: State, store: SavedStateStore?) {
            val keep = state as? KeepState
            keep?.let {
                store?.set(SAVE_KEY, it.timestamp)
            }
        }

        override suspend fun restoreState(store: SavedStateStore?): RestoredStateIntent? {
            val timestamp = store?.get<Long>(SAVE_KEY) ?: return null
            return RestoredStateIntent(null, MainIntent(timestamp))
        }
    }

    companion object {
        private const val SAVE_KEY = "state"
    }


}