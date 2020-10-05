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
import ru.ar2code.redis.core.defaults.DefaultLogger

val defaultReducers = listOf(
    InitiatedStateTypeAReducer(),
    InitiatedStateTypeBReducer(),
    InitiatedStateUiViewStateOnlyReducer(),
    InitiatedStateUiEventOnlyReducer(),
    InitiatedStateUiViewWithEventReducer()
)

class InitiatedStateTypeAReducer : ViewStateReducer<TestViewModelState, TestViewModelEvent>(
    ViewModelInitiatedState::class, IntentUiTypeA::class, SimpleTestLogger()
) {
    override fun reduce(currentState: State, intent: IntentMessage): Flow<State> {
        return flow {
            emit(ViewModelTypeAState(null, null))
        }
    }
}

class InitiatedStateTypeBReducer : ViewStateReducer<TestViewModelState, TestViewModelEvent>(
    ViewModelInitiatedState::class, IntentUiTypeB::class, SimpleTestLogger()
) {
    override fun reduce(currentState: State, intent: IntentMessage): Flow<State> {
        return flow {
            emit(ViewModelTypeBState(null, null))
        }
    }
}

class InitiatedStateUiViewStateOnlyReducer :
    ViewStateReducer<TestViewModelState, TestViewModelEvent>(
        ViewModelInitiatedState::class, IntentUiViewStateOnly::class, SimpleTestLogger()
    ) {
    override fun reduce(currentState: State, intent: IntentMessage): Flow<State> {
        return flow {
            emit(ViewModelViewOnlyState(TestViewModelState()))
        }
    }
}

class InitiatedStateUiEventOnlyReducer : ViewStateReducer<TestViewModelState, TestViewModelEvent>(
    ViewModelInitiatedState::class, IntentUiViewEventOnly::class, SimpleTestLogger()
) {
    override fun reduce(currentState: State, intent: IntentMessage): Flow<State> {
        return flow {
            emit(ViewModelEventOnlyState(TestViewModelEvent(TestViewModelEvent.TestViewModelEventType())))
        }
    }
}

class InitiatedStateUiViewWithEventReducer :
    ViewStateReducer<TestViewModelState, TestViewModelEvent>(
        ViewModelInitiatedState::class, IntentUiViewStateWithEvent::class, SimpleTestLogger()
    ) {
    override fun reduce(currentState: State, intent: IntentMessage): Flow<State> {
        return flow {
            emit(
                ViewModelViewWithEventState(
                    TestViewModelState(), TestViewModelEvent(
                        TestViewModelEvent.TestViewModelEventType()
                    )
                )
            )
        }
    }
}