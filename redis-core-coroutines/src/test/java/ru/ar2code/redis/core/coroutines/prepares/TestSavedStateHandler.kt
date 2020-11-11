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

package ru.ar2code.redis.core.coroutines.prepares

import ru.ar2code.redis.core.RestoredStateIntent
import ru.ar2code.redis.core.SavedStateHandler
import ru.ar2code.redis.core.SavedStateStore
import ru.ar2code.redis.core.State

class TestSavedStateHandler : SavedStateHandler {

    companion object {
        const val KEY = "KEY"
    }

    override suspend fun storeState(state: State, store: SavedStateStore?) {
        if(state is StateB){
            store?.set(KEY, state.data)
        }
    }

    override suspend fun restoreState(store: SavedStateStore?): RestoredStateIntent? {
        val data = store?.get<Int>(KEY) ?: return null
        return RestoredStateIntent(StateB(data), IntentTypeFlow())
    }

}