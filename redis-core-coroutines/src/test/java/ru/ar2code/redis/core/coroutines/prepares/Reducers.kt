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

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.ar2code.redis.core.IntentMessage
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.coroutines.StateReducer

class SimpleExceptionStateReducer : StateReducer(State.Initiated::class, IntentTypeA::class) {

    override fun reduce(
        currentState: State,
        intent: IntentMessage.IntentMessageType<Any>
    ): Flow<State> {
        return flow {
            throw TestException()
        }
    }
}

class InitiatedStateTypeAReducer :
    StateReducer(State.Initiated::class, IntentTypeA::class) {

    override fun reduce(
        currentState: State,
        intent: IntentMessage.IntentMessageType<Any>
    ): Flow<State> {
        return flow {
            emit(StateA())
        }
    }
}

class InitiatedStateTypeBReducer :
    StateReducer(State.Initiated::class, IntentTypeB::class) {

    override fun reduce(
        currentState: State,
        intent: IntentMessage.IntentMessageType<Any>
    ): Flow<State> {
        return flow {
            emit(StateB((intent as IntentTypeB).payload ?: 0))
        }
    }
}

class StateBTypeFlowReducer :
    StateReducer(StateB::class, IntentTypeFlow::class) {

    override fun reduce(
        currentState: State,
        intent: IntentMessage.IntentMessageType<Any>
    ): Flow<State> {
        return flow {
            emit(FlowStateD(FlowStateD.NAME))
        }
    }
}


class StateATypeCReducer :
    StateReducer(StateA::class, IntentTypeC::class) {

    override fun reduce(
        currentState: State,
        intent: IntentMessage.IntentMessageType<Any>
    ): Flow<State> {
        return flow {
            emit(StateC())
        }
    }
}

class InitiatedStateTypeFlowReducer :
    StateReducer(State.Initiated::class, IntentTypeFlow::class) {

    override fun reduce(
        currentState: State,
        intent: IntentMessage.IntentMessageType<Any>
    ): Flow<State> {
        return flow {
            emit(FlowStateD(FlowStateD.NAME))
            emit(FlowStateF(FlowStateF.NAME))
        }
    }
}

class FlowStateTypeFlowReducer :
    StateReducer(FlowState::class, IntentTypeFlow::class) {

    override fun reduce(
        currentState: State,
        intent: IntentMessage.IntentMessageType<Any>
    ): Flow<State> {
        return flow {
            emit(FlowStateD(FlowStateD.NAME))
            emit(FlowStateF(FlowStateF.NAME))
        }
    }
}

class InitiatedStateTypeDelayFlowReducer :
    StateReducer(State.Initiated::class, IntentTypeDelayFlow::class) {

    override fun reduce(
        currentState: State,
        intent: IntentMessage.IntentMessageType<Any>
    ): Flow<State> {
        return flow {
            emit(FlowStateF(FlowStateF.NAME))
            delay(25)
            emit(FlowStateD(FlowStateD.NAME))
        }
    }
}

class FlowStateTypeDelayFlowReducer :
    StateReducer(FlowState::class, IntentTypeDelayFlow::class) {

    override fun reduce(
        currentState: State,
        intent: IntentMessage.IntentMessageType<Any>
    ): Flow<State> {
        return flow {
            emit(FlowStateF(FlowStateF.NAME))
            delay(25)
            emit(FlowStateD(FlowStateD.NAME))
        }
    }
}