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

package ru.ar2code.android.redis.core.android.impl

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import ru.ar2code.android.redis.core.android.BaseViewEvent
import ru.ar2code.android.redis.core.android.BaseViewState
import ru.ar2code.android.redis.core.models.IntentMessage
import ru.ar2code.android.redis.core.models.ServiceResult
import ru.ar2code.android.redis.core.services.ActorService
import ru.ar2code.android.redis.core.services.ActorServiceState
import ru.ar2code.android.redis.core.services.ServiceStateWithResult
import ru.ar2code.utils.Logger

@ExperimentalCoroutinesApi
internal class ActorViewModelService<ViewState, ViewEvent>(
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher,
    logger: Logger,
    private val onIntentMsgCallback: (suspend (IntentMessage) -> ServiceStateWithResult<ViewModelStateWithEvent<ViewState, ViewEvent>>)?,
    private val canChangeStateCallback: ((ActorServiceState, ServiceResult<ViewModelStateWithEvent<ViewState, ViewEvent>>) -> Boolean)?
) :
    ActorService<ViewModelStateWithEvent<ViewState, ViewEvent>>(
        scope, dispatcher, null, logger
    ) where ViewState : BaseViewState, ViewEvent : BaseViewEvent {

    override suspend fun onIntentMsg(msg: IntentMessage): ServiceStateWithResult<ViewModelStateWithEvent<ViewState, ViewEvent>>? {
        return onIntentMsgCallback?.invoke(msg)
    }

    override fun canChangeState(
        newServiceState: ActorServiceState,
        result: ServiceResult<ViewModelStateWithEvent<ViewState, ViewEvent>>
    ): Boolean {
        return canChangeStateCallback?.invoke(newServiceState, result) ?: true
    }
}