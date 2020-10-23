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

class InitiatedToAStateTrigger : StateTrigger(State.Initiated::class, StateA::class, TestLogger()) {

    override val triggerIntent: IntentMessage
        get() = IntentTypeC()
}

class InitiatedToBStateTrigger : StateTrigger(State.Initiated::class, StateB::class, TestLogger()) {

    override val triggerIntent: IntentMessage
        get() = IntentTypeFlow()
}

class AnyToCStateTrigger : StateTrigger(null, StateC::class, TestLogger()) {

    override val triggerIntent: IntentMessage
        get() = IntentTypeFlow()
}

class InitiatedToAnyStateTrigger : StateTrigger(State.Initiated::class, null, TestLogger()) {

    override val triggerIntent: IntentMessage
        get() = IntentTypeC()
}