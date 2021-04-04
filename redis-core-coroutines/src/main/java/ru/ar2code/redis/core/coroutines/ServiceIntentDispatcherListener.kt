package ru.ar2code.redis.core.coroutines

import ru.ar2code.redis.core.IntentMessage

internal interface ServiceIntentDispatcherListener {

    suspend fun onIntentDispatched(intent: IntentMessage)

}