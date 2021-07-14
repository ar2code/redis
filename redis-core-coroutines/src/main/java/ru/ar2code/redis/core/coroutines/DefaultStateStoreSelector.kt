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

package ru.ar2code.redis.core.coroutines

import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.StateRestore
import ru.ar2code.redis.core.StateStore
import ru.ar2code.redis.core.StateStoreSelector

/**
 * Default state store selector that searches item for concrete state.
 * If concrete StateStore was not found selector tries to find any applicable StateStore.
 * If nothing found returns null.
 */
class DefaultStateStoreSelector : StateStoreSelector {

    override fun findStateStore(state: State, stateStores: List<StateStore<*>>): StateStore<*>? {

        var anyStateStore: StateStore<*>? = null

        stateStores.forEach {
            val isConcreteStateStoreApplicable =
                !it.isAnyState && it.isStateStoreApplicable(state)

            if (isConcreteStateStoreApplicable) {
                return it
            }

            val isAnyStateStoreApplicable =
                it.isAnyState && it.isStateStoreApplicable(state)

            if (anyStateStore == null && isAnyStateStoreApplicable) {
                anyStateStore = it
            }
        }

        return anyStateStore
    }

    override fun findStateRestore(
        stateName: String,
        stateStores: List<StateRestore>
    ): StateRestore? {

        var anyStateRestore: StateRestore? = null

        stateStores.forEach {
            val isConcreteStateRestoreApplicable =
                !it.isAnyState() && it.isStateRestoreApplicable(stateName)

            if (isConcreteStateRestoreApplicable) {
                return it
            }

            val isAnyStateRestoreApplicable =
                it.isAnyState() && it.isStateRestoreApplicable(stateName)

            if (anyStateRestore == null && isAnyStateRestoreApplicable) {
                anyStateRestore = it
            }
        }

        return anyStateRestore
    }
}