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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.coroutines.*

@ExperimentalCoroutinesApi
object ServiceFactory {

    val defaultReducers =
        listOf(
            InitiatedStateTypeAReducer(),
            InitiatedStateTypeBReducer(),
            StateATypeCReducer(),
            InitiatedStateTypeFlowReducer(),
            StateBTypeFlowReducer(),
            InitiatedStateTypeDelayFlowReducer(),
            FlowStateTypeFlowReducer(),
            FlowStateTypeDelayFlowReducer(),
            StateBTypeBReducer(),
            InitiatedStateConcurrentTypeReducer(),
            StateAConcurrentTypeReducer()
        )

    val defaultTriggers = listOf(
        InitiatedToAStateTrigger(),
        InitiatedToBStateTrigger()
    )

    val defaultTriggersWithAny = listOf(
        InitiatedToAStateTrigger(),
        InitiatedToBStateTrigger(),
        InitiatedToAnyStateTrigger(),
        AnyToCStateTrigger()
    )

    fun buildSimpleService(
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher
    ): RedisCoroutineStateService {
        return RedisCoroutineStateService(
            scope,
            dispatcher,
            State.Initiated(),
            defaultReducers,
            DefaultReducerSelector(),
            emptyList(),
            DefaultStateTriggerSelector(),
            TestLogger()
        )
    }

    fun buildSimpleServiceWithTriggers(
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher
    ): RedisCoroutineStateService {
        return RedisCoroutineStateService(
            scope,
            dispatcher,
            State.Initiated(),
            defaultReducers,
            DefaultReducerSelector(),
            defaultTriggers,
            DefaultStateTriggerSelector(),
            TestLogger()
        )
    }

    fun buildSimpleServiceWithSavedStateStore(
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher,
        stateStore: SavedStateStore,
        stateHandler: SavedStateHandler
    ): RedisSavedStateService {
        return RedisSavedStateService(
            scope,
            dispatcher,
            State.Initiated(),
            defaultReducers,
            DefaultReducerSelector(),
            emptyList(),
            DefaultStateTriggerSelector(),
            TestLogger(),
            stateStore,
            stateHandler
        )
    }

    fun buildServiceWithCustomInit(
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher
    ): RedisCoroutineStateService {
        return RedisCoroutineStateService(
            scope,
            dispatcher,
            CustomInitState(),
            defaultReducers,
            DefaultReducerSelector(),
            defaultTriggers,
            DefaultStateTriggerSelector(),
            TestLogger()
        )
    }

    fun buildServiceWithReducerException(
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher
    ): RedisCoroutineStateService {
        return RedisCoroutineStateService(
            scope,
            dispatcher,
            State.Initiated(),
            listOf(SimpleExceptionStateReducer()),
            DefaultReducerSelector(),
            defaultTriggers,
            DefaultStateTriggerSelector(),
            TestLogger()
        )
    }
}