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
import ru.ar2code.redis.core.test.TestLogger

class SimpleExceptionStateReducer : StateReducer(
    State.Initiated::class, IntentTypeA::class,
    TestLogger()
) {

    override fun reduce(
        currentState: State,
        intent: IntentMessage
    ): Flow<State> {
        return flow {
            throw TestException()
        }
    }
}

class InitiatedStateConcurrentTypeReducer :
    StateReducer(State.Initiated::class, IntentTypeConcurrentTest::class, TestLogger()) {

    override fun reduce(
        currentState: State,
        intent: IntentMessage
    ): Flow<State> {
        return flow {
            emit(StateA())
        }
    }
}

class StateAConcurrentTypeReducer :
    StateReducer(StateA::class, IntentTypeConcurrentTest::class, TestLogger()) {

    override fun reduce(
        currentState: State,
        intent: IntentMessage
    ): Flow<State> {
        return flow {
            emit(StateA())
        }
    }
}

class InitiatedStateTypeAReducer :
    StateReducer(State.Initiated::class, IntentTypeA::class, TestLogger()) {

    override fun reduce(
        currentState: State,
        intent: IntentMessage
    ): Flow<State> {
        return flow {
            emit(StateA())
        }
    }
}

class InitiatedStateTypeBReducer :
    StateReducer(State.Initiated::class, IntentTypeB::class, TestLogger()) {

    override fun reduce(
        currentState: State,
        intent: IntentMessage
    ): Flow<State> {
        return flow {
            emit(StateB((intent as IntentTypeB).payload ?: 0))
        }
    }
}

class StateBTypeBReducer :
    StateReducer(StateB::class, IntentTypeB::class, TestLogger()) {

    override fun reduce(
        currentState: State,
        intent: IntentMessage
    ): Flow<State> {
        return flow {
            emit(StateB((intent as IntentTypeB).payload ?: 0))
        }
    }
}

class StateBTypeFlowReducer :
    StateReducer(StateB::class, IntentTypeFlow::class, TestLogger()) {

    override fun reduce(
        currentState: State,
        intent: IntentMessage
    ): Flow<State> {
        return flow {
            emit(FlowStateD(FlowStateD.NAME))
        }
    }
}


class StateATypeCReducer :
    StateReducer(StateA::class, IntentTypeC::class, TestLogger()) {

    override fun reduce(
        currentState: State,
        intent: IntentMessage
    ): Flow<State> {
        return flow {
            emit(StateC())
        }
    }
}

class InitiatedStateTypeFlowReducer :
    StateReducer(State.Initiated::class, IntentTypeFlow::class, TestLogger()) {

    override fun reduce(
        currentState: State,
        intent: IntentMessage
    ): Flow<State> {
        return flow {
            emit(FlowStateD(FlowStateD.NAME))
            emit(FlowStateF(FlowStateF.NAME))
        }
    }
}

class FlowStateTypeFlowReducer :
    StateReducer(FlowState::class, IntentTypeFlow::class, TestLogger()) {

    override fun reduce(
        currentState: State,
        intent: IntentMessage
    ): Flow<State> {
        return flow {
            emit(FlowStateD(FlowStateD.NAME))
            emit(FlowStateF(FlowStateF.NAME))
        }
    }
}

class InitiatedStateTypeDelayFlowReducer :
    StateReducer(State.Initiated::class, IntentTypeDelayFlow::class, TestLogger()) {

    override fun reduce(
        currentState: State,
        intent: IntentMessage
    ): Flow<State> {
        return flow {
            emit(FlowStateG(FlowStateG.NAME))
            delay(25)
            emit(FlowStateH(FlowStateH.NAME))
        }
    }
}

class FlowStateTypeDelayFlowReducer :
    StateReducer(FlowState::class, IntentTypeDelayFlow::class, TestLogger()) {

    override fun reduce(
        currentState: State,
        intent: IntentMessage
    ): Flow<State> {
        return flow {
            emit(FlowStateF(FlowStateF.NAME))
            delay(25)
            emit(FlowStateD(FlowStateD.NAME))
        }
    }
}

class AnyStateTypeCReducer :
    StateReducer(null, IntentTypeC::class, TestLogger()) {

    override fun reduce(
        currentState: State,
        intent: IntentMessage
    ): Flow<State> {
        return flow {
            emit(FlowStateF(FlowStateF.NAME))
            delay(25)
            emit(FlowStateD(FlowStateD.NAME))
        }
    }
}

class AnyStateAnyTypeReducer :
    StateReducer(null, null, TestLogger()) {

    override fun reduce(
        currentState: State,
        intent: IntentMessage
    ): Flow<State> {
        return flow {
            emit(FlowStateF(FlowStateF.NAME))
            delay(25)
            emit(FlowStateD(FlowStateD.NAME))
        }
    }
}

class StateCAnyTypeReducer :
    StateReducer(StateC::class, null, TestLogger()) {

    override fun reduce(
        currentState: State,
        intent: IntentMessage
    ): Flow<State> {
        return flow {
            emit(FlowStateF(FlowStateF.NAME))
            delay(25)
            emit(FlowStateD(FlowStateD.NAME))
        }
    }
}

class AnyStateFinishIntentReducer : StateReducer(null, FinishIntent::class, TestLogger()) {
    override fun reduce(currentState: State, intent: IntentMessage): Flow<State>? {
        return flow {
            emit(FinishState())
        }
    }
}

class FinishStateAnyIntentReducer : StateReducer(FinishState::class, null, TestLogger()) {
    override fun reduce(currentState: State, intent: IntentMessage): Flow<State>? {
        return null
    }
}

class DisposedStateAnyIntentReducer : StateReducer(State.Disposed::class, null, TestLogger()) {
    override fun reduce(currentState: State, intent: IntentMessage): Flow<State>? {
        return null
    }
}

class AnyStateCircleIntentReducer : StateReducer(null, CircleIntent::class, TestLogger()) {
    override fun reduce(currentState: State, intent: IntentMessage): Flow<State> {
        return flow {
            emit(StateA())
        }
    }
}

class ErrorStateIntentAReducer :
    StateReducer(State.ErrorOccurred::class, IntentTypeA::class, TestLogger()) {
    override fun reduce(currentState: State, intent: IntentMessage): Flow<State> {
        return flow {
            emit(StateA())
        }
    }
}