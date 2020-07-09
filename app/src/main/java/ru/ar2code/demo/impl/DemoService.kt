package ru.ar2code.demo.impl

import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import ru.ar2code.android.architecture.core.models.IntentMessage
import ru.ar2code.android.architecture.core.services.ActorService
import ru.ar2code.android.architecture.core.services.ActorServiceState

@ExperimentalCoroutinesApi
class DemoService : ActorService<String>() {

    private var demoUseCase = SimpleUseCase()

    override suspend fun onIntentMsg(msg: IntentMessage) {
        demoUseCase.run("test")
            .collect {
                val result = StringResult("got from service ${it.payload}")
                provideResult(ActorServiceState.Ready(), result)
            }
    }

}