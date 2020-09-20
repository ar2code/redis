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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.ar2code.redis.core.IntentMessage
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.coroutines.StateReducer

class SimpleExceptionStateReducer : StateReducer(State.Initiated::class, SimpleIntentType::class) {

    override fun reduce(
        currentState: State,
        intent: IntentMessage.IntentMessageType<Any>
    ): Flow<State> {
        return flow {
            throw TestException()
        }
    }
}

class SimpleStateReducer :
    StateReducer(State.Initiated::class, SimpleIntentType::class) {

    override fun reduce(
        currentState: State,
        intent: IntentMessage.IntentMessageType<Any>
    ): Flow<State> {
        return flow {
            emit(SimpleState())
        }
    }
}

class AnotherStateReducer :
    StateReducer(State.Initiated::class, AnotherIntentType::class) {

    override fun reduce(
        currentState: State,
        intent: IntentMessage.IntentMessageType<Any>
    ): Flow<State> {
        return flow {
            emit(AnotherState((intent as AnotherIntentType).payload ?: 0))
        }
    }
}

class AnotherStateFromFlowIntentReducer :
    StateReducer(AnotherState::class, FlowIntentType::class) {

    override fun reduce(
        currentState: State,
        intent: IntentMessage.IntentMessageType<Any>
    ): Flow<State> {
        return flow {
            emit(FlowFirstState(FlowFirstState.NAME))
        }
    }
}


class FloatStateReducer :
    StateReducer(SimpleState::class, FloatIntentType::class) {

    override fun reduce(
        currentState: State,
        intent: IntentMessage.IntentMessageType<Any>
    ): Flow<State> {
        return flow {
            emit(FloatState())
        }
    }
}

class FlowStateReducer :
    StateReducer(State.Initiated::class, FlowIntentType::class) {

    override fun reduce(
        currentState: State,
        intent: IntentMessage.IntentMessageType<Any>
    ): Flow<State> {
        return flow {
            emit(FlowFirstState(FlowFirstState.NAME))
            emit(FlowSecondState(FlowSecondState.NAME))
        }
    }
}