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

package ru.ar2code.redis.core.android

import ru.ar2code.redis.core.IntentMessage
import ru.ar2code.redis.core.coroutines.StateReducer
import ru.ar2code.utils.Logger
import kotlin.reflect.KClass

/**
 * ViewStateReducer is a [RedisViewModel] special [StateReducer] that works only with [ViewModelStateWithEvent]
 */
abstract class ViewStateReducer<ViewState, ViewEvent>(
    expectState: KClass<out ViewModelStateWithEvent<ViewState, ViewEvent>>?,
    expectIntentType: KClass<out IntentMessage>?,
    logger: Logger
) : StateReducer(
    expectState,
    expectIntentType, logger
) where ViewState : BaseViewState, ViewEvent : BaseViewEvent