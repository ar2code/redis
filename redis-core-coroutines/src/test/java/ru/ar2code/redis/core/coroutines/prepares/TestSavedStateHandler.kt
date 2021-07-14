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

import ru.ar2code.redis.core.*
import ru.ar2code.redis.core.test.TestLogger

class TestSavedStateHandler : SavedStateHandler {

    companion object {
        const val STATE_KEY = "TEST_STATE_KEY"
        const val STATE_DATA_KEY = "KEY"
    }

    class TestStateStore : StateStore<StateB>("StateB", TestLogger()) {
        override suspend fun storeStateData(state: StateB, store: SavedStateStore?) {
            store?.set(STATE_DATA_KEY, state.data)
        }

        override fun isStateStoreApplicable(state: State): Boolean {
            return state is StateB
        }

        override val isAnyState: Boolean
            get() = false
    }

    class TestStateRestore : StateRestore("StateB", TestLogger()) {
        override suspend fun restoreState(store: SavedStateStore?): RestoredStateIntent? {
            val data = store?.get<Int>(STATE_DATA_KEY) ?: return null
            return RestoredStateIntent(StateB(data), IntentTypeFlow())
        }
    }

    override val stateStores: List<StateStore<*>>
        get() = listOf(TestStateStore())

    override val stateRestores: List<StateRestore>
        get() = listOf(TestStateRestore())

    override val stateStoreKeyName: String
        get() = STATE_KEY

    override fun getStoredKeys(): List<String> {
        return listOf(STATE_KEY, STATE_DATA_KEY)
    }
}