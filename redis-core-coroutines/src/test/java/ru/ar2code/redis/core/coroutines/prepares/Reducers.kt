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

class SimpleExceptionStateReducer : StateReducer<State.Initiated, IntentTypeA>(
    TestLogger()
) {
    override val isAnyIntent: Boolean
        get() = false

    override val isAnyState: Boolean
        get() = false

    override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
        return currentState is State.Initiated && intent is IntentTypeA
    }

    override fun reduce(currentState: State.Initiated, intent: IntentTypeA): Flow<State>? {
        return flow {
            throw TestException()
        }
    }
}

class InitiatedStateConcurrentTypeReducer :
    StateReducer<State.Initiated, IntentTypeConcurrentTest>(TestLogger()) {

    override val isAnyIntent: Boolean
        get() = false

    override val isAnyState: Boolean
        get() = false

    override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
        return currentState is State.Initiated && intent is IntentTypeConcurrentTest
    }

    override fun reduce(
        currentState: State.Initiated,
        intent: IntentTypeConcurrentTest
    ): Flow<State> {
        return flow {
            emit(StateA())
        }
    }
}

class StateAConcurrentTypeReducer :
    StateReducer<StateA, IntentTypeConcurrentTest>(TestLogger()) {

    override val isAnyIntent: Boolean
        get() = false

    override val isAnyState: Boolean
        get() = false

    override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
        return currentState is StateA && intent is IntentTypeConcurrentTest
    }

    override fun reduce(currentState: StateA, intent: IntentTypeConcurrentTest): Flow<State> {
        return flow {
            emit(StateA())
        }
    }
}

class InitiatedStateTypeAReducer :
    StateReducer<State.Initiated, IntentTypeA>(TestLogger()) {

    override val isAnyIntent: Boolean
        get() = false

    override val isAnyState: Boolean
        get() = false

    override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
        return currentState is State.Initiated && intent is IntentTypeA
    }

    override fun reduce(currentState: State.Initiated, intent: IntentTypeA): Flow<State> {
        return flow {
            emit(StateA())
        }
    }
}

class InitiatedStateTypeBReducer :
    StateReducer<State.Initiated, IntentTypeB>(TestLogger()) {

    override val isAnyIntent: Boolean
        get() = false

    override val isAnyState: Boolean
        get() = false

    override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
        return currentState is State.Initiated && intent is IntentTypeB
    }

    override fun reduce(currentState: State.Initiated, intent: IntentTypeB): Flow<State>? {
        return flow {
            emit(StateB(intent.payload ?: 0))
        }
    }
}

class StateBTypeBReducer :
    StateReducer<StateB, IntentTypeB>(TestLogger()) {

    override val isAnyIntent: Boolean
        get() = false

    override val isAnyState: Boolean
        get() = false

    override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
        return currentState is StateB && intent is IntentTypeB
    }

    override fun reduce(currentState: StateB, intent: IntentTypeB): Flow<State>? {
        return flow {
            emit(StateB((intent as IntentTypeB).payload ?: 0))
        }
    }
}

class FlowStateDTypeBReducer :
    StateReducer<FlowStateD, IntentTypeB>(TestLogger()) {

    override val isAnyIntent: Boolean
        get() = false

    override val isAnyState: Boolean
        get() = false

    override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
        return currentState is FlowStateD && intent is IntentTypeB
    }

    override fun reduce(currentState: FlowStateD, intent: IntentTypeB): Flow<State>? {
        return flow {
            emit(StateB((intent as IntentTypeB).payload ?: 0))
        }
    }
}

class StateBTypeFlowReducer :
    StateReducer<StateB, IntentTypeFlow>(TestLogger()) {

    override val isAnyIntent: Boolean
        get() = false

    override val isAnyState: Boolean
        get() = false

    override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
        return currentState is StateB && intent is IntentTypeFlow
    }

    override fun reduce(currentState: StateB, intent: IntentTypeFlow): Flow<State>? {
        return flow {
            emit(FlowStateD(FlowStateD.NAME))
        }
    }
}


class StateATypeCReducer :
    StateReducer<StateA, IntentTypeC>(TestLogger()) {

    override val isAnyIntent: Boolean
        get() = false

    override val isAnyState: Boolean
        get() = false

    override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
        return currentState is StateA && intent is IntentTypeC
    }

    override fun reduce(currentState: StateA, intent: IntentTypeC): Flow<State>? {
        return flow {
            emit(StateC())
        }
    }
}

class InitiatedStateTypeFlowReducer :
    StateReducer<State.Initiated, IntentTypeFlow>(TestLogger()) {

    override val isAnyIntent: Boolean
        get() = false

    override val isAnyState: Boolean
        get() = false

    override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
        return currentState is State.Initiated && intent is IntentTypeFlow
    }

    override fun reduce(currentState: State.Initiated, intent: IntentTypeFlow): Flow<State>? {
        return flow {
            emit(FlowStateD(FlowStateD.NAME))
            emit(FlowStateF(FlowStateF.NAME))
        }
    }
}

class FlowStateTypeFlowReducer :
    StateReducer<FlowState, IntentTypeFlow>(TestLogger()) {

    override val isAnyIntent: Boolean
        get() = false

    override val isAnyState: Boolean
        get() = false

    override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
        return currentState is FlowState && intent is IntentTypeFlow
    }

    override fun reduce(currentState: FlowState, intent: IntentTypeFlow): Flow<State>? {
        return flow {
            emit(FlowStateD(FlowStateD.NAME))
            emit(FlowStateF(FlowStateF.NAME))
        }
    }
}

class InitiatedStateTypeDelayFlowReducer :
    StateReducer<State.Initiated, IntentTypeDelayFlow>(TestLogger()) {

    override val isAnyIntent: Boolean
        get() = false

    override val isAnyState: Boolean
        get() = false

    override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
        return currentState is State.Initiated && intent is IntentTypeDelayFlow
    }

    override fun reduce(currentState: State.Initiated, intent: IntentTypeDelayFlow): Flow<State>? {
        return flow {
            emit(FlowStateG(FlowStateG.NAME))
            delay(25)
            emit(FlowStateH(FlowStateH.NAME))
        }
    }
}

class FlowStateTypeDelayFlowReducer :
    StateReducer<FlowState, IntentTypeDelayFlow>(TestLogger()) {

    override val isAnyIntent: Boolean
        get() = false

    override val isAnyState: Boolean
        get() = false

    override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
        return currentState is FlowState && intent is IntentTypeDelayFlow
    }

    override fun reduce(currentState: FlowState, intent: IntentTypeDelayFlow): Flow<State>? {
        return flow {
            emit(FlowStateF(FlowStateF.NAME))
            delay(25)
            emit(FlowStateD(FlowStateD.NAME))
        }
    }
}

class AnyStateTypeCReducer :
    StateReducer<State, IntentTypeC>(TestLogger()) {

    override val isAnyIntent: Boolean
        get() = false

    override val isAnyState: Boolean
        get() = true

    override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
        return intent is IntentTypeC
    }

    override fun reduce(currentState: State, intent: IntentTypeC): Flow<State>? {
        return flow {
            emit(FlowStateF(FlowStateF.NAME))
            delay(25)
            emit(FlowStateD(FlowStateD.NAME))
        }
    }
}

class AnyStateAnyTypeReducer :
    StateReducer<State, IntentMessage>(TestLogger()) {

    override val isAnyIntent: Boolean
        get() = true

    override val isAnyState: Boolean
        get() = true

    override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
        return true
    }

    override fun reduce(currentState: State, intent: IntentMessage): Flow<State>? {
        return flow {
            emit(FlowStateF(FlowStateF.NAME))
            delay(25)
            emit(FlowStateD(FlowStateD.NAME))
        }
    }
}

class StateCAnyTypeReducer :
    StateReducer<StateC, IntentMessage>(TestLogger()) {

    override val isAnyIntent: Boolean
        get() = true

    override val isAnyState: Boolean
        get() = false

    override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
        return currentState is StateC
    }

    override fun reduce(currentState: StateC, intent: IntentMessage): Flow<State>? {
        return flow {
            emit(FlowStateF(FlowStateF.NAME))
            delay(25)
            emit(FlowStateD(FlowStateD.NAME))
        }
    }
}

class AnyStateFinishIntentReducer : StateReducer<State, FinishIntent>(TestLogger()) {
    override val isAnyIntent: Boolean
        get() = false

    override val isAnyState: Boolean
        get() = true

    override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
        return intent is FinishIntent
    }

    override fun reduce(currentState: State, intent: FinishIntent): Flow<State> {
        return flow {
            emit(FinishState())
        }
    }
}

class FinishStateAnyIntentReducer : StateReducer<FinishState, IntentMessage>(TestLogger()) {
    override val isAnyIntent: Boolean
        get() = true

    override val isAnyState: Boolean
        get() = false

    override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
        return currentState is FinishState
    }

    override fun reduce(currentState: FinishState, intent: IntentMessage): Flow<State>? {
        return null
    }
}

class DisposedStateAnyIntentReducer : StateReducer<State.Disposed, IntentMessage>(TestLogger()) {
    override val isAnyIntent: Boolean
        get() = true

    override val isAnyState: Boolean
        get() = false

    override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
        return currentState is State.Disposed
    }

    override fun reduce(currentState: State.Disposed, intent: IntentMessage): Flow<State>? {
        return null
    }
}

class AnyStateCircleIntentReducer : StateReducer<State, CircleIntent>(TestLogger()) {
    override val isAnyIntent: Boolean
        get() = false

    override val isAnyState: Boolean
        get() = true

    override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
        return intent is CircleIntent
    }

    override fun reduce(currentState: State, intent: CircleIntent): Flow<State> {
        return flow {
            emit(StateA())
        }
    }
}

class ErrorStateIntentAReducer :
    StateReducer<State.ErrorOccurred, IntentTypeA>(TestLogger()) {

    override val isAnyIntent: Boolean
        get() = false

    override val isAnyState: Boolean
        get() = false

    override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
        return currentState is State.ErrorOccurred && intent is IntentTypeA
    }

    override fun reduce(currentState: State.ErrorOccurred, intent: IntentTypeA): Flow<State>? {
        return flow {
            emit(StateA())
        }
    }
}