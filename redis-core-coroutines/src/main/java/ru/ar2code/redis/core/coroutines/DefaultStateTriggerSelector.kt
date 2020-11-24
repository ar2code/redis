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

import ru.ar2code.redis.core.State

/**
 * Default trigger selector that searches specified trigger.
 * If specified trigger was not found try to find any applicable trigger.
 * If nothing found returns null.
 */
class DefaultStateTriggerSelector : StateTriggerSelector {

    override fun findTrigger(triggers: List<StateTrigger>?, old: State, new: State): StateTrigger? {

        var anyTrigger: StateTrigger? = null

        triggers?.forEach {
            val isConcreteTriggerApplicable =
                it.isStatesSpecified() && it.isTriggerApplicable(old, new)

            if (isConcreteTriggerApplicable) {
                return it
            }

            val isAnyTriggerApplicable =
                !it.isStatesSpecified() && it.isTriggerApplicable(old, new)

            if (anyTrigger == null && isAnyTriggerApplicable) {
                anyTrigger = it
            }
        }

        return anyTrigger
    }
}