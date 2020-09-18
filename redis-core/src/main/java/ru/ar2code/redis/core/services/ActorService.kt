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

package ru.ar2code.redis.core.services

import ru.ar2code.redis.core.models.IntentMessage
import ru.ar2code.utils.Logger

interface ActorService {

    val serviceState: ActorServiceState

    /**
     * Dispatch intent to service for doing some action
     */
    fun dispatch(msg: IntentMessage)

    /**
     * After disposing service can not get intents and send results.
     */
    fun dispose()

    /**
     * @return if true service can not get intents and send results.
     */
    fun isDisposed(): Boolean

    /**
     * Subscribe to service's results.
     * Subscribing is alive while service is not disposed [isDisposed] and [scope] not cancelled
     */
    fun subscribe(subscriber: ServiceSubscriber)

    /**
     * Stop listening service`s result by this [subscriber]
     */
    fun unsubscribe(subscriber: ServiceSubscriber)

    /**
     * @return count of active subscribers
     */
    fun getSubscribersCount(): Int
}