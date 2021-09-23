package ru.ar2code.redis.core.coroutines

import ru.ar2code.redis.core.IntentMessage

internal interface ServiceIntentDispatcherListener {

    /**
     * This callback will invoke when service started handling intent
     */
    suspend fun onIntentDispatched(intent: IntentMessage) {
        //This callback will invoke when service started handling intent
    }

    /**
     * This callback will invoke if reducer returned null for this intent
     */
    suspend fun reducerNotFoundForIntent(intent: IntentMessage) {
        //This callback will invoke if reducer returned null for this intent
    }

}