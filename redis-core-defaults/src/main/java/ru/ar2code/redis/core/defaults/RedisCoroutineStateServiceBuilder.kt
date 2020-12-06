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

package ru.ar2code.redis.core.defaults

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import ru.ar2code.redis.core.SavedStateHandler
import ru.ar2code.redis.core.SavedStateStore
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.StateStoreSelector
import ru.ar2code.redis.core.coroutines.*
import ru.ar2code.utils.Logger

open class RedisCoroutineStateServiceBuilder(
    private val scope: CoroutineScope,
    private val reducers: List<StateReducer>
) {
    private var dispatcher: CoroutineDispatcher? = null
    private var initialState: State? = null
    private var reducerSelector: ReducerSelector? = null
    private var listenedServicesIntentSelector: IntentSelector? = null
    private var stateTriggers: List<StateTrigger>? = null
    private var stateTriggerSelector: StateTriggerSelector? = null
    private var logger: Logger? = null
    private var savedStateStore: SavedStateStore? = null
    private var savedStateHandler: SavedStateHandler? = null
    private var stateStoreSelector: StateStoreSelector? = null
    private var serviceLogName: String? = null

    fun setInitialState(initialState: State): RedisCoroutineStateServiceBuilder {
        this.initialState = initialState
        return this
    }

    fun setCoroutineDispatcher(dispatcher: CoroutineDispatcher): RedisCoroutineStateServiceBuilder {
        this.dispatcher = dispatcher
        return this
    }

    fun setReducerSelector(reducerSelector: ReducerSelector): RedisCoroutineStateServiceBuilder {
        this.reducerSelector = reducerSelector
        return this
    }

    fun setListenedServicesIntentSelector(intentSelector: IntentSelector): RedisCoroutineStateServiceBuilder {
        this.listenedServicesIntentSelector = intentSelector
        return this
    }

    fun setTriggers(triggers: List<StateTrigger>): RedisCoroutineStateServiceBuilder {
        this.stateTriggers = triggers
        return this
    }

    fun setTriggerSelector(triggerSelector: StateTriggerSelector): RedisCoroutineStateServiceBuilder {
        this.stateTriggerSelector = triggerSelector
        return this
    }

    fun setLogger(logger: Logger): RedisCoroutineStateServiceBuilder {
        this.logger = logger
        return this
    }

    fun setSavedStateStore(savedStateStore: SavedStateStore): RedisCoroutineStateServiceBuilder {
        this.savedStateStore = savedStateStore
        return this
    }

    fun setSavedStateHandler(savedStateHandler: SavedStateHandler): RedisCoroutineStateServiceBuilder {
        this.savedStateHandler = savedStateHandler
        return this
    }

    fun setStateStoreSelector(stateStoreSelector: StateStoreSelector): RedisCoroutineStateServiceBuilder {
        this.stateStoreSelector = stateStoreSelector
        return this
    }

    fun setServiceLogName(serviceLogName: String?): RedisCoroutineStateServiceBuilder {
        this.serviceLogName = serviceLogName
        return this
    }

    fun build(): RedisCoroutineStateService {
        return RedisCoroutineSavedStateService(
            scope,
            dispatcher ?: Dispatchers.Default,
            initialState ?: State.Initiated(),
            reducers,
            reducerSelector
                ?: DefaultReducerSelector(),
            listenedServicesIntentSelector
                ?: DefaultIntentSelector(),
            stateTriggers,
            stateTriggerSelector
                ?: DefaultStateTriggerSelector(),
            logger
                ?: DefaultLogger(),
            serviceLogName,
            savedStateStore,
            savedStateHandler,
            stateStoreSelector ?: DefaultStateStoreSelector()
        )
    }
}