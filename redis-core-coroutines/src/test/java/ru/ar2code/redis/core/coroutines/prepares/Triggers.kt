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

import ru.ar2code.redis.core.IntentMessage
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.coroutines.StateTrigger
import ru.ar2code.redis.core.test.TestLogger

class InitiatedToAStateTrigger : StateTrigger<State.Initiated, StateA>(TestLogger()) {
    override fun specifyTriggerIntent(oldState: State.Initiated, newState: StateA): IntentMessage? {
        return IntentTypeC()
    }

    override val isAnyOldState: Boolean
        get() = false
    override val isAnyNewState: Boolean
        get() = false

    override fun isTriggerApplicable(oldState: State, newState: State): Boolean {
        return oldState is State.Initiated && newState is StateA
    }
}

class InitiatedToBStateTrigger : StateTrigger<State.Initiated, StateB>(TestLogger()) {
    override fun specifyTriggerIntent(oldState: State.Initiated, newState: StateB): IntentMessage? {
        return IntentTypeFlow()
    }

    override val isAnyOldState: Boolean
        get() = false
    override val isAnyNewState: Boolean
        get() = false

    override fun isTriggerApplicable(oldState: State, newState: State): Boolean {
        return oldState is State.Initiated && newState is StateB
    }
}

class AnyToCStateTrigger : StateTrigger<State, StateC>(TestLogger()) {
    override fun specifyTriggerIntent(oldState: State, newState: StateC): IntentMessage? {
        return IntentTypeFlow()
    }

    override val isAnyOldState: Boolean
        get() = true
    override val isAnyNewState: Boolean
        get() = false

    override fun isTriggerApplicable(oldState: State, newState: State): Boolean {
        return newState is StateC
    }
}

class InitiatedToAnyStateTrigger : StateTrigger<State.Initiated, State>(TestLogger()) {
    override fun specifyTriggerIntent(oldState: State.Initiated, newState: State): IntentMessage? {
        return IntentTypeC()
    }

    override val isAnyOldState: Boolean
        get() = false
    override val isAnyNewState: Boolean
        get() = true

    override fun isTriggerApplicable(oldState: State, newState: State): Boolean {
        return oldState is State.Initiated
    }
}

class TriggerWithActionError : StateTrigger<State.Initiated, State>(TestLogger()) {
    override suspend fun invokeSpecifiedAction(oldState: State.Initiated, newState: State) {
        throw TestException()
    }

    override fun specifyTriggerIntent(oldState: State.Initiated, newState: State): IntentMessage? {
        return IntentTypeC()
    }

    override val isAnyOldState: Boolean
        get() = false
    override val isAnyNewState: Boolean
        get() = true

    override fun isTriggerApplicable(oldState: State, newState: State): Boolean {
        return oldState is State.Initiated
    }
}

class TriggerWithIntentError : StateTrigger<State.Initiated, State>(TestLogger()) {
    override fun specifyTriggerIntent(oldState: State.Initiated, newState: State): IntentMessage? {
        throw TestException()
    }

    override val isAnyOldState: Boolean
        get() = false
    override val isAnyNewState: Boolean
        get() = true

    override fun isTriggerApplicable(oldState: State, newState: State): Boolean {
        return oldState is State.Initiated
    }
}