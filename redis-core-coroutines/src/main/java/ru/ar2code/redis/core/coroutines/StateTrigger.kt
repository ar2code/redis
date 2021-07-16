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
import ru.ar2code.utils.LoggableObject
import ru.ar2code.utils.Logger
import kotlin.reflect.KClass

/**
 * You can specify some triggers that should fire when service change state from A to B
 * Trigger can do some simple action or dispatch an intent to its service.
 */
@Suppress("UNCHECKED_CAST")
abstract class StateTrigger<O, N>(
    protected val logger: Logger
) : LoggableObject where O : State, N : State {

    abstract val isAnyOldState: Boolean

    abstract val isAnyNewState: Boolean

    fun getTriggerIntent(oldState: State, newState: State): IntentMessage? {
        return specifyTriggerIntent(oldState as O, newState as N)
    }

    suspend fun invokeAction(oldState: State, newState: State) {
        invokeSpecifiedAction(oldState as O, newState as N)
    }

    open fun specifyTriggerIntent(oldState: O, newState: N): IntentMessage? = null

    open suspend fun invokeSpecifiedAction(oldState: O, newState: N) {}

    abstract fun isTriggerApplicable(
        oldState: State,
        newState: State
    ): Boolean

    fun isStatesSpecified(): Boolean {
        return !isAnyNewState && !isAnyOldState
    }

}