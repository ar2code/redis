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

import ru.ar2code.redis.core.IntentMessage
import ru.ar2code.redis.core.State

/**
 * Default reducer selector that searches concrete reducer for state and intentMessageType.
 * If specified reducer was not found try to find any applicable reducer (any state or any intent)
 * If nothing found throws IllegalArgumentException
 */
class DefaultReducerSelector : ReducerSelector {

    override fun findReducer(
        reducers: List<StateReducer>,
        state: State,
        intentMessage: IntentMessage
    ): StateReducer {

        var anyReducer: StateReducer? = null

        reducers.forEach {
            val isConcreteReducerApplicable =
                it.isStateWithIntentSpecified() && it.isReducerApplicable(state, intentMessage)

            if (isConcreteReducerApplicable) {
                return it
            }

            val isAnyReducerApplicable =
                !it.isStateWithIntentSpecified() && it.isReducerApplicable(state, intentMessage)

            if (anyReducer == null && isAnyReducerApplicable) {
                anyReducer = it
            }
        }

        return anyReducer
            ?: throw ReducerNotFoundException("Reducer for (${state.objectLogName},${intentMessage.objectLogName}) did not found.")
    }
}