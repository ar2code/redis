package ru.ar2code.redis.core.android

import ru.ar2code.redis.core.IntentMessage
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.StateIntentMessageBuilder
import ru.ar2code.redis.core.coroutines.cast

class OnServiceErrorIntent(val errorState: State.ErrorOccurred) :
    IntentMessage() {
    companion object {
        fun createBuilder(): StateIntentMessageBuilder {
            return object : StateIntentMessageBuilder {
                override fun build(state: State): IntentMessage {
                    return OnServiceErrorIntent(state.cast())
                }
            }
        }
    }
}