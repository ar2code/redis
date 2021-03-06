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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import ru.ar2code.redis.core.coroutines.DefaultStateStoreSelector
import ru.ar2code.redis.core.SavedStateHandler
import ru.ar2code.redis.core.SavedStateStore
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.coroutines.*

/**
 * Service that can store/restore state with defaults parameters:
 * [dispatcher] = [Dispatchers.Default], [logger] = [DefaultLogger],
 * [reducerSelector] = [DefaultReducerSelector], [stateTriggerSelector] = [DefaultStateTriggerSelector]
 * [stateStoreSelector] = [DefaultStateStoreSelector]
 */

abstract class DefaultRedisCoroutineSavedStateService(
    scope: CoroutineScope,
    reducers: List<StateReducer>,
    savedStateStore: SavedStateStore,
    savedStateHandler: SavedStateHandler,
    stateTriggers: List<StateTrigger>? = null
) :
    RedisCoroutineSavedStateService(
        scope,
        Dispatchers.Default,
        State.Initiated(),
        reducers,
        DefaultReducerSelector(),
        DefaultIntentSelector(),
        stateTriggers,
        DefaultStateTriggerSelector(),
        DefaultLogger(),
        null,
        savedStateStore,
        savedStateHandler,
        DefaultStateStoreSelector()
    )