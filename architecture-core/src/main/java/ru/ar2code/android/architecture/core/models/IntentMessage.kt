package ru.ar2code.android.architecture.core.models

class IntentMessage(val msgType : IntentMessageType, val payload: Any? = null) {

    abstract class IntentMessageType()

}