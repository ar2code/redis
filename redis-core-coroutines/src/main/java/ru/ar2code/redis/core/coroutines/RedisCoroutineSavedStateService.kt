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

/**
 * Redis actor service based on kotlin coroutines and can store/restore its state
 * @param savedStateStore state store implementation
 * @param savedStateHandler object that handle storing/restoring state
 * @param stateStoreSelector algorithm how to find storing logic for current state
 */
@ExperimentalCoroutinesApi
open class RedisCoroutineSavedStateService(
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher,
    initialState: State,
    reducers: List<StateReducer>,
    reducerSelector: ReducerSelector,
    listenedServicesIntentSelector: IntentSelector,
    stateTriggers: List<StateTrigger>?,
    stateTriggerSelector: StateTriggerSelector?,
    logger: Logger,
    serviceLogName: String?,
    private val savedStateStore: SavedStateStore?,
    private val savedStateHandler: SavedStateHandler?,
    private val stateStoreSelector: StateStoreSelector?
) : RedisCoroutineStateService(
    scope,
    dispatcher,
    initialState,
    reducers,
    reducerSelector,
    listenedServicesIntentSelector,
    stateTriggers,
    stateTriggerSelector,
    logger,
    serviceLogName
) {

    private var lastRestoredStateIntent: RestoredStateIntent? = null

    override suspend fun onStateChanged(old: State, new: State) {
        super.onStateChanged(old, new)

        savedStateHandler?.let {
            val stateStore = stateStoreSelector?.findStateStore(new, it.stateStores)
            stateStore?.let { store ->
                logger.info("$objectLogName store state with ${store.objectLogName}")

                store.store(new, savedStateStore)
            }
        }
    }

    override suspend fun onInitialized() {
        super.onInitialized()

        dispatchIntentAfterInitializing()

        lastRestoredStateIntent = null
    }

    override suspend fun getInitialState(): State {
        savedStateHandler?.let { handler ->
            val storedStateName = savedStateStore?.get<String>(handler.stateStoreKeyName)

            storedStateName?.let {
                val stateRestore = stateStoreSelector?.findStateRestore(it, handler.stateRestores)
                stateRestore?.let { restore ->
                    logger.info("$objectLogName restore state with ${restore.objectLogName}")
                    lastRestoredStateIntent = restore.restoreState(savedStateStore)
                }
            }
        }

        return lastRestoredStateIntent?.state ?: super.getInitialState()
    }

    private fun dispatchIntentAfterInitializing() {
        lastRestoredStateIntent?.intentMessage?.let {
            dispatch(it)
        }
    }
}