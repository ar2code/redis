package ru.ar2code.demo.impl

import ru.ar2code.android.architecture.core.models.IntentMessage

class ActionOneIntentMsg(payload : String?) : IntentMessage.IntentMessageType<String>(payload)