package ru.ar2code.android.architecture.core.android.prepares

import ru.ar2code.android.architecture.core.android.BaseViewEvent
import ru.ar2code.android.architecture.core.android.ViewEventType

class TestViewModelEvent(eventType: TestViewModelEventType): BaseViewEvent(viewEventType = eventType) {

    class TestViewModelEventType : ViewEventType<String>()
}