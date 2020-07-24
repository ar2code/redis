package ru.ar2code.android.architecture.core.models

class IntentMessage(val msgType: IntentMessageType<Any>) {
    abstract class IntentMessageType<out T>(val payload: T? = null) where T : Any
}