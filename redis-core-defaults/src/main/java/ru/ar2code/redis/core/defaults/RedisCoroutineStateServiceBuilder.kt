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

/**
 * @param scope service scope. You can cancel scope to dispose service.
 * @param reducers list of reducers used to change service` state
 */
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

    /**
     * Set state that the service receives after creation
     * Service receives [State.Initiated] by default
     */
    fun setInitialState(initialState: State): RedisCoroutineStateServiceBuilder {
        this.initialState = initialState
        return this
    }

    /**
     * Set service coroutine dispatcher
     */
    fun setCoroutineDispatcher(dispatcher: CoroutineDispatcher): RedisCoroutineStateServiceBuilder {
        this.dispatcher = dispatcher
        return this
    }

    /**
     * Set algorithm how to find reducer for pair state-intent
     * [DefaultReducerSelector] is used by default
     */
    fun setReducerSelector(reducerSelector: ReducerSelector): RedisCoroutineStateServiceBuilder {
        this.reducerSelector = reducerSelector
        return this
    }

    /**
     * Set algorithm how to find reaction for service state changing that current service listens
     * [DefaultIntentSelector] is used by default
     */
    fun setListenedServicesIntentSelector(intentSelector: IntentSelector): RedisCoroutineStateServiceBuilder {
        this.listenedServicesIntentSelector = intentSelector
        return this
    }

    /**
     * Set list of triggers that can be called when service change its state
     */
    fun setTriggers(triggers: List<StateTrigger>): RedisCoroutineStateServiceBuilder {
        this.stateTriggers = triggers
        return this
    }

    /**
     * Set algorithm how to find triggers when service change state
     * [DefaultStateTriggerSelector] is used by default
     */
    fun setTriggerSelector(triggerSelector: StateTriggerSelector): RedisCoroutineStateServiceBuilder {
        this.stateTriggerSelector = triggerSelector
        return this
    }

    /**
     * Set logger for tracing
     */
    fun setLogger(logger: Logger): RedisCoroutineStateServiceBuilder {
        this.logger = logger
        return this
    }

    /**
     * Set state store implementation
     */
    fun setSavedStateStore(savedStateStore: SavedStateStore): RedisCoroutineStateServiceBuilder {
        this.savedStateStore = savedStateStore
        return this
    }

    /**
     * Set object that handle storing/restoring state process
     *
     */
    fun setSavedStateHandler(savedStateHandler: SavedStateHandler): RedisCoroutineStateServiceBuilder {
        this.savedStateHandler = savedStateHandler
        return this
    }

    /**
     * Set algorithm how to find storing logic for current state
     * [DefaultStateStoreSelector] is used by default
     */
    fun setStateStoreSelector(stateStoreSelector: StateStoreSelector): RedisCoroutineStateServiceBuilder {
        this.stateStoreSelector = stateStoreSelector
        return this
    }

    /**
     * Set object name that is used for logging
     */
    fun setServiceLogName(serviceLogName: String?): RedisCoroutineStateServiceBuilder {
        this.serviceLogName = serviceLogName
        return this
    }

    /**
     * Build service
     */
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