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

package ru.ar2code.demo.impl

import kotlinx.coroutines.delay
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.inject
import ru.ar2code.android.redis.core.android.ActorViewModel
import ru.ar2code.redis.core.android.BaseViewEvent
import ru.ar2code.redis.core.android.ChangeableState
import ru.ar2code.redis.core.android.ViewEventType
import ru.ar2code.redis.core.android.impl.ViewModelServiceResult
import ru.ar2code.redis.core.android.ViewModelStateWithEvent
import ru.ar2code.redis.core.IntentMessage
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.services.ServiceStateWithResult
import ru.ar2code.defaults.DefaultLogger

class DemoViewModel :
    ActorViewModel<DemoViewModel.DemoViewState, DemoViewModel.DemoViewEvent>(DefaultLogger()) {

    class DemoViewState(
        state: String, isChangedSincePrevious: Boolean
    ) : ChangeableState<String>(state, isChangedSincePrevious)

    class DemoViewEvent(viewEventType: DemoViewEventType) : BaseViewEvent(viewEventType)

    class DemoViewEventType : ViewEventType<String>()

    val injService : AbstractDemoServiceCoroutine by inject(AbstractDemoServiceCoroutine::class.java) { parametersOf(viewModelScope) }

    init {
        val state = injService.serviceState
        viewModelScope.launch {
            sendIntent(IntentMessage(ActionOneIntentMsg("1")))
            repeat(10){
                delay(1000)
                sendIntent(IntentMessage(ActionOneIntentMsg("1")))
            }
            delay(1000)
            sendIntent(IntentMessage(ActionOneIntentMsg("2")))
        }
    }

    override suspend fun onIntentMsg(msg: IntentMessage): ServiceStateWithResult<ViewModelStateWithEvent<DemoViewState, DemoViewEvent>> {
        logger.info("rozhkov DemoViewModel onIntentMsg = $msg")

        val newState = (msg.msgType as ActionOneIntentMsg).payload.orEmpty()
        val lastState = this.viewStateLive.value
        val isChanged = lastState == null || lastState.state != newState

        val sr = ViewModelServiceResult(
            ViewModelStateWithEvent<DemoViewState, DemoViewEvent>(
                DemoViewState(newState, isChanged),
                null
            )
        )
        return ServiceStateWithResult(State.Same(), sr)
    }
}