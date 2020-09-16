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

package ru.ar2code.android.redis.core.prepares

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.ar2code.android.redis.core.models.IntentMessage
import ru.ar2code.android.redis.core.services.ActorServiceState
import ru.ar2code.android.redis.core.services.CoroutineActorService
import ru.ar2code.android.redis.core.services.ServiceStateWithResult
import ru.ar2code.android.redis.core.services.StateReducer

@ExperimentalCoroutinesApi
class ServiceWithExceptionInsideIntentHandlingCoroutine(
    scope: CoroutineScope, dispatcher: CoroutineDispatcher
) :
    CoroutineActorService(scope, dispatcher, ActorServiceState.Initiated(), listOf(SimpleExceptionStateReducer()), null, SimpleTestLogger()) {

    class TestException : Exception()

    class SimpleExceptionStateReducer : StateReducer(ActorServiceState.Initiated::class, SimpleServiceCoroutine.SimpleIntentType::class) {

        override fun reduce(
            currentState: ActorServiceState,
            intent: IntentMessage.IntentMessageType<Any>
        ): Flow<ActorServiceState> {
            return flow {
                throw TestException()
            }
        }

    }
}