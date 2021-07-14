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

package ru.ar2code.redis.core.android.prepares

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.ar2code.redis.core.IntentMessage
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.android.ViewStateReducer
import ru.ar2code.redis.core.test.TestLogger

val defaultReducers = listOf(
    InitiatedStateTypeAReducer(),
    InitiatedStateTypeBReducer(),
    InitiatedStateUiViewStateOnlyReducer(),
    InitiatedStateUiEventOnlyReducer(),
    InitiatedStateUiViewWithEventReducer()
)

class InitiatedStateTypeAReducer : ViewStateReducer<ViewModelInitiatedState, IntentUiTypeA>(
    TestLogger()
) {
    override fun reduce(
        currentState: ViewModelInitiatedState,
        intent: IntentUiTypeA
    ): Flow<State> {
        return flow {
            emit(ViewModelTypeAState(null, null))
        }
    }

    override val isAnyState: Boolean
        get() = false
    override val isAnyIntent: Boolean
        get() = false

    override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
        return currentState is ViewModelInitiatedState && intent is IntentUiTypeA
    }
}

class InitiatedStateTypeBReducer : ViewStateReducer<ViewModelInitiatedState, IntentUiTypeB>(
    TestLogger()
) {
    override fun reduce(
        currentState: ViewModelInitiatedState,
        intent: IntentUiTypeB
    ): Flow<State> {
        return flow {
            emit(ViewModelTypeBState(null, null))
        }
    }

    override val isAnyState: Boolean
        get() = false
    override val isAnyIntent: Boolean
        get() = false

    override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
        return currentState is ViewModelInitiatedState && intent is IntentUiTypeB
    }
}

class InitiatedStateUiViewStateOnlyReducer :
    ViewStateReducer<ViewModelInitiatedState, IntentUiViewStateOnly>(
        TestLogger()
    ) {

    override fun reduce(
        currentState: ViewModelInitiatedState,
        intent: IntentUiViewStateOnly
    ): Flow<State> {
        return flow {
            emit(ViewModelViewOnlyState(TestViewModelState()))
        }
    }

    override val isAnyState: Boolean
        get() = false
    override val isAnyIntent: Boolean
        get() = false

    override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
        return currentState is ViewModelInitiatedState && intent is IntentUiViewStateOnly
    }
}

class InitiatedStateUiEventOnlyReducer :
    ViewStateReducer<ViewModelInitiatedState, IntentUiViewStateOnly>(
        TestLogger()
    ) {
    override fun reduce(
        currentState: ViewModelInitiatedState,
        intent: IntentUiViewStateOnly
    ): Flow<State> {
        return flow {
            emit(ViewModelEventOnlyState(TestViewModelEvent()))
        }
    }

    override val isAnyState: Boolean
        get() = false
    override val isAnyIntent: Boolean
        get() = false

    override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
        return currentState is ViewModelInitiatedState && intent is IntentUiViewStateOnly
    }
}

class InitiatedStateUiViewWithEventReducer :
    ViewStateReducer<ViewModelInitiatedState, IntentUiViewStateWithEvent>(
        TestLogger()
    ) {
    override fun reduce(
        currentState: ViewModelInitiatedState,
        intent: IntentUiViewStateWithEvent
    ): Flow<State> {
        return flow {
            emit(
                ViewModelViewWithEventState(
                    TestViewModelState(), TestViewModelEvent()
                )
            )
        }
    }

    override val isAnyState: Boolean
        get() = false
    override val isAnyIntent: Boolean
        get() = false

    override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
        return currentState is ViewModelInitiatedState && intent is IntentUiViewStateWithEvent
    }
}