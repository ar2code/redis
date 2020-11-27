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

import kotlin.reflect.KClass

/**
 * Describes mechanism og storing specified state [expectState]
 *
 * You should create a list of StateStore items to provide a mechanism of storing for each service state.
 * But you can set only single StateStore that handles all service states. For it just create [StateStore] with [StateStore.expectState] is null.
 */
abstract class StateStore(private val expectState: KClass<out State>?) {

    abstract suspend fun store(state: State, store: SavedStateStore?)

    fun isStateStoreApplicable(state: State): Boolean {
        return isAnyState() || expectState?.isInstance(state) == true
    }

    /**
     * Is universal StateStore for any states?
     */
    fun isAnyState(): Boolean {
        return expectState == null
    }
}