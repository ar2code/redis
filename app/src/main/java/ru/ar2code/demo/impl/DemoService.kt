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

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import ru.ar2code.android.redis.core.models.IntentMessage
import ru.ar2code.android.redis.core.models.ServiceResult
import ru.ar2code.android.redis.core.services.ActorServiceState
import ru.ar2code.android.redis.core.services.ServiceStateWithResult

@ExperimentalCoroutinesApi
class DemoService(
    scope: CoroutineScope
) : AbstractDemoService(scope) {

    private var globalData = 0

    private var demoUseCase = SimpleUseCase()

    override suspend fun onIntentMsg(msg: IntentMessage): ServiceStateWithResult<String>? {
        globalData++

        Log.d(
            "ROZHKOV",
            "start handling globalData = $globalData"
        )

        dddd()

        val type = msg.msgType
        val payload = when (type) {
            is ActionOneIntentMsg -> type.payload
            else -> "Unknown"
        }

        demoUseCase.run(payload)
            .collect {
                delay(100)
                val result = StringResult("got from service ${it}")
                broadcastNewStateWithResult(
                    ServiceStateWithResult(
                        ActorServiceState.Same(),
                        result
                    )
                )
            }

        globalData--

        Log.d(
            "ROZHKOV",
            "end handling globalData = $globalData"
        )

        return null
    }

    private suspend fun dddd() = coroutineScope {
        val a1 = async {
            Log.d(
                "ROZHKOV",
                "start dddd 1"
            )
            delay(100)
            Log.d(
                "ROZHKOV",
                "finish dddd 1"
            )
        }

        val a2 = async {
            Log.d(
                "ROZHKOV",
                "start dddd 2"
            )
            delay(100)
            Log.d(
                "ROZHKOV",
                "finish dddd 2"
            )
        }

        awaitAll(a1, a2)
    }

    override fun getResultFotInitializedState(): ServiceResult<String> {
        return ServiceResult.BasicResult("Empty.")
    }
}