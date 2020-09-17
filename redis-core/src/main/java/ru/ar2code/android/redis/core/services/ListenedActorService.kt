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

package ru.ar2code.android.redis.core.services

import ru.ar2code.android.redis.core.models.IntentMessage

/**
 * Each [ActorService] can listen another services and dispatch intent to itself if state of listened service was changed
 * @param service listened service
 * @param intentBuilder lambda that returns special [IntentMessage] for newState from listened service.
 */
class ListenedActorService(
    val service: ActorService,
    val intentBuilder : (newState : ActorServiceState) -> IntentMessage
)