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

import kotlinx.coroutines.flow.Flow
import ru.ar2code.redis.core.IntentMessage
import ru.ar2code.redis.core.State
import ru.ar2code.utils.LoggableObject
import ru.ar2code.utils.Logger
import kotlin.reflect.KClass

/**
 * This is a reducer - a function (wrapped to class) that takes a current state value and an
 * action object describing "what happened", and returns a new state value as flow.
 * A reducer's function signature is: (state, action) => Flow<newState>
 */
abstract class StateReducer<S, I>(
    protected val logger: Logger
) : LoggableObject where S : State, I : IntentMessage {

    @Suppress("UNCHECKED_CAST")
    fun reduceState(
        currentState: State,
        intent: IntentMessage
    ): Flow<State>? {
        return reduce(currentState as S, intent as I)
    }

    fun isStateWithIntentSpecified(): Boolean {
        return !isAnyState && !isAnyIntent
    }

    abstract val isAnyState: Boolean

    abstract val isAnyIntent: Boolean

    abstract fun reduce(currentState: S, intent: I): Flow<State>?

    abstract fun isReducerApplicable(
        currentState: State,
        intent: IntentMessage
    ): Boolean
}