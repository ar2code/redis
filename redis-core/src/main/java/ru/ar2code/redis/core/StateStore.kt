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

package ru.ar2code.redis.core

import ru.ar2code.utils.LoggableObject
import ru.ar2code.utils.Logger

/**
 * Describes mechanism og storing specified state [S]
 *
 * You should create a list of StateStore items to provide a mechanism of storing for each service state.
 *
 * But you can set only single StateStore that handles all service states.
 *
 * @param storedStateName - this string will be used as State value inside [SavedStateStore]. By this name you can recognize and restore specified state.
 */
@Suppress("UNCHECKED_CAST")
abstract class StateStore<S>(
    val storedStateName: String,
    protected val logger: Logger
) : LoggableObject where S : State {

    suspend fun storeStateData(state: State, store: SavedStateStore?) {
        storeSpecifiedStateData(state as S, store)
    }

    /**
     * Store only state data.
     * Don't store state name inside this method.
     */
    abstract suspend fun storeSpecifiedStateData(state: S, store: SavedStateStore?)

    /**
     * Is this StateStore applicable for storing specified [state]
     */
    abstract fun isStateStoreApplicable(state: State): Boolean

    /**
     * Is universal StateStore for any states?
     *
     * If true this StateStore can be used for storing any type of state.
     */
    abstract val isAnyState: Boolean
}