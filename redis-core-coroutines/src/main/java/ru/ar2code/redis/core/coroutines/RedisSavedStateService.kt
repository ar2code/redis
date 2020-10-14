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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import ru.ar2code.redis.core.*
import ru.ar2code.utils.Logger

@ExperimentalCoroutinesApi
open class RedisSavedStateService(
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher,
    initialState: State,
    reducers: List<StateReducer>,
    reducerSelector: ReducerSelector,
    stateTriggers: List<StateTrigger>?,
    stateTriggerSelector: StateTriggerSelector?,
    logger: Logger,
    private val savedStateStore: SavedStateStore?,
    private val savedStateHandler: SavedStateHandler?
) : RedisCoroutineStateService(
    scope,
    dispatcher,
    initialState,
    reducers,
    reducerSelector,
    stateTriggers,
    stateTriggerSelector,
    logger
) {

    override suspend fun onStateChanged(old: State, new: State) {
        super.onStateChanged(old, new)
        savedStateHandler?.storeState(new, savedStateStore)
    }

    override suspend fun onInitialized() {
        super.onInitialized()

        restoreStateWithIntent()
    }

    private suspend fun restoreStateWithIntent() {
        val stateWithIntent = savedStateHandler?.restoreState(savedStateStore)
        stateWithIntent?.state?.let {
            broadcastNewState(it)
        }
        stateWithIntent?.intentMessage?.let {
            dispatch(it)
        }
    }
}