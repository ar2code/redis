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

package ru.ar2code.redis.core.coroutines

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import ru.ar2code.redis.core.RedisStateService
import ru.ar2code.redis.core.ServiceSubscriber
import ru.ar2code.redis.core.State
import kotlin.reflect.KClass

/**
 * Returns flow that subscribe to a redis service and collects single element equals to [expectState]
 * This flow never stops until expected state will be received or service becomes disposed.
 */
fun RedisStateService.awaitStateAsFlow(expectState: KClass<out State>?) =
    callbackFlow<State> {
        val subscriber = object : ServiceSubscriber {
            override suspend fun onReceive(newState: State) {
                val isExpectedState = expectState == null || expectState.isInstance(newState)
                val isDisposed = newState is State.Disposed

                if (isExpectedState || isDisposed) {
                    channel.runCatching {
                        if (!channel.isClosedForSend) {
                            send(newState)
                        }
                    }
                    channel.close()
                }
            }
        }

        this@awaitStateAsFlow.subscribe(subscriber)

        awaitClose {
            this@awaitStateAsFlow.unsubscribe(subscriber)
        }
    }

/**
 *
 */
suspend fun RedisStateService.awaitFirstState(): State {
    return this.awaitStateAsFlow(null)
        .first()
}

/**
 * Suspend and await expected state type [expectState] from service [this] with timeout.
 * Extension will be suspended until expected state will be received or service becomes disposed or timeout.
 *
 * It timeout AwaitStateTimeoutException will be thrown
 *
 * If service becomes disposed and expected state was not [State.Disposed] AwaitStateTimeoutException will be thrown
 */
suspend fun RedisStateService.awaitStateWithTimeout(
    timeoutMs: Long,
    expectState: KClass<out State>
): State {
    val awaitedState = withTimeoutOrNull(timeoutMs) {
        awaitStateAsFlow(expectState).firstOrNull()
    }
        ?: throw AwaitStateTimeoutException("await state $expectState finished with $timeoutMs ms timeout. CurrentState = ${this.serviceState}")

    SubscriberExtensionsUtil.throwErrorIfStateDisposedNotExpected(this, awaitedState, expectState)

    return awaitedState
}

private object SubscriberExtensionsUtil {

    fun throwErrorIfStateDisposedNotExpected(
        service: RedisStateService,
        awaitedState: State,
        expectState: KClass<out State>
    ) {
        if (awaitedState is State.Disposed && !expectState.isInstance(State.Disposed())) {
            throw AwaitStateTimeoutException("Service [${service.objectLogName}] is disposed.")
        }
    }
}