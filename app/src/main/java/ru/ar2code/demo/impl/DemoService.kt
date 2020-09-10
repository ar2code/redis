/*
 * Copyright (c) 2020. Created by Alexey Rozhkov.
 */

package ru.ar2code.demo.impl

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import ru.ar2code.android.architecture.core.models.IntentMessage
import ru.ar2code.android.architecture.core.models.ServiceResult
import ru.ar2code.android.architecture.core.services.ActorServiceState
import ru.ar2code.android.architecture.core.services.ServiceStateWithResult

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
                val result = StringResult("got from service ${it.payload}")
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
        return ServiceResult.EmptyResult("Empty.")
    }
}