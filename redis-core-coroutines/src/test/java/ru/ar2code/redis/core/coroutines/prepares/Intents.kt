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
import ru.ar2code.redis.core.StateIntentMessageBuilder

class IntentTypeA(val payload: String? = null) :
    IntentMessage()

class IntentTypeB(val payload: Int? = null) :
    IntentMessage()

class IntentTypeC(val payload: Float? = null) :
    IntentMessage()

class IntentTypeFlow(val payload: Int? = null) :
    IntentMessage()

class IntentTypeDelayFlow(val payload: Int? = null) :
    IntentMessage()

class IntentTypeConcurrentTest(val payload: Int? = null) :
    IntentMessage()

class FinishIntent() : IntentMessage()

class CircleIntent : IntentMessage()

class IntentTypeABuilder : StateIntentMessageBuilder {
    override fun build(state: State): IntentMessage {
        return IntentTypeA()
    }
}

class IntentTypeBBuilder : StateIntentMessageBuilder {
    override fun build(state: State): IntentMessage {
        return IntentTypeB()
    }
}

class IntentTypeCBuilder : StateIntentMessageBuilder {
    override fun build(state: State): IntentMessage {
        return IntentTypeC()
    }
}

class IntentTypeFlowBuilder : StateIntentMessageBuilder {
    override fun build(state: State): IntentMessage {
        return IntentTypeFlow()
    }
}
