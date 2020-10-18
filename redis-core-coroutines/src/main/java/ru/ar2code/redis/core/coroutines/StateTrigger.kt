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
import ru.ar2code.utils.Logger
import kotlin.reflect.KClass

abstract class StateTrigger(
    private val expectOldState: KClass<*>?,
    private val expectNewState: KClass<*>?,
    protected val logger: Logger
) {
    abstract val triggerIntent: IntentMessage

    fun isTriggerApplicable(
        oldState: State,
        newState: State
    ): Boolean {

        val isOldApplicable = isAnyOldState() || expectOldState?.isInstance(oldState) == true
        val isNewApplicable = isAnyNewState() || expectNewState?.isInstance(newState) == true

        return isOldApplicable && isNewApplicable
    }

    fun isStatesSpecified(): Boolean {
        return !isAnyNewState() && !isAnyOldState()
    }

    fun isAnyOldState(): Boolean {
        return expectOldState == null
    }

    fun isAnyNewState(): Boolean {
        return expectNewState == null
    }
}