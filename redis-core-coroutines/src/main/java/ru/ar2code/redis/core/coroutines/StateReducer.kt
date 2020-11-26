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
abstract class StateReducer(
    private val expectState: KClass<out State>?,
    private val expectIntentType: KClass<out IntentMessage>?,
    protected val logger: Logger
) : LoggableObject {
    abstract fun reduce(
        currentState: State,
        intent: IntentMessage
    ): Flow<State>?

    fun isReducerApplicable(
        currentState: State,
        intent: IntentMessage
    ): Boolean {
        val isExpectedOrAnyState = isAnyState() || expectState?.isInstance(currentState) == true
        val isExpectedOrAnyIntent =
            isAnyIntentType() || expectIntentType?.isInstance(intent) == true

        return isExpectedOrAnyState && isExpectedOrAnyIntent
    }

    fun isStateWithIntentSpecified(): Boolean {
        return !isAnyState() && !isAnyIntentType()
    }

    fun isAnyState(): Boolean {
        return expectState == null
    }

    fun isAnyIntentType(): Boolean {
        return expectIntentType == null
    }
}