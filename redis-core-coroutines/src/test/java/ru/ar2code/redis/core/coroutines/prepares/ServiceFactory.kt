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
import ru.ar2code.redis.core.StateService
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.coroutines.CoroutineStateService
import ru.ar2code.redis.core.coroutines.SavedStateHandler
import ru.ar2code.redis.core.coroutines.SavedStateStore

@ExperimentalCoroutinesApi
object ServiceFactory {

    private val defaultReducers =
        listOf(SimpleStateReducer(), AnotherStateReducer(), FloatStateReducer(), FlowStateReducer(), AnotherStateFromFlowIntentReducer())

    fun buildSimpleService(scope: CoroutineScope, dispatcher: CoroutineDispatcher): StateService {
        return CoroutineStateService(
            scope,
            dispatcher,
            State.Initiated(),
            defaultReducers,
            null,
            SimpleTestLogger(),
            null,
            null
        )
    }

    fun buildSimpleServiceWithSavedStateStore(
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher,
        stateStore: SavedStateStore,
        stateHandler: SavedStateHandler
    ): StateService {
        return CoroutineStateService(
            scope,
            dispatcher,
            State.Initiated(),
            defaultReducers,
            null,
            SimpleTestLogger(),
            stateStore,
            stateHandler
        )
    }

    fun buildServiceWithCustomInit(
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher
    ): StateService {
        return CoroutineStateService(
            scope,
            dispatcher,
            CustomInitState(),
            emptyList(),
            null,
            SimpleTestLogger(),
            null,
            null
        )
    }

    fun buildServiceWithReducerException(
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher
    ): StateService {
        return CoroutineStateService(
            scope,
            dispatcher,
            State.Initiated(),
            listOf(SimpleExceptionStateReducer()),
            null,
            SimpleTestLogger(),
            null,
            null
        )
    }
}