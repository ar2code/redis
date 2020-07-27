package ru.ar2code.demo.impl

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import ru.ar2code.android.architecture.core.models.IntentMessage
import ru.ar2code.android.architecture.core.models.ServiceResult
import ru.ar2code.android.architecture.core.services.ActorService
import ru.ar2code.android.architecture.core.services.ActorServiceState
import ru.ar2code.android.architecture.core.services.DefaultActorService

@ExperimentalCoroutinesApi
class DemoService(scope: CoroutineScope
) : DefaultActorService<String>(scope) {

    private var globalData = 1

    private var demoUseCase = SimpleUseCase()

    override suspend fun onIntentMsg(msg: IntentMessage) {
        globalData++

        Log.d(
            "ROZHKOV",
            "start handling globalData = $globalData"
        )

        val type = msg.msgType
        val payload = when (type) {
            is ActionOneIntentMsg -> type.payload
            else -> "Unknown"
        }

        if (serviceState is ActorServiceState.Disposed)
            return

        demoUseCase.run(payload)
            .collect {
                delay(100)
                val result = StringResult("got from service ${it.payload}")
                provideResult(ActorServiceState.Ready(), result)
            }
    }

    override fun onIntentHandlingFinished() {
        super.onIntentHandlingFinished()
        globalData--

        Log.d(
            "ROZHKOV",
            "end handling globalData = $globalData"
        )
    }

    override fun getResultFotInitializedState(): ServiceResult<String> {
        return ServiceResult.EmptyResult("Empty.")
    }
}