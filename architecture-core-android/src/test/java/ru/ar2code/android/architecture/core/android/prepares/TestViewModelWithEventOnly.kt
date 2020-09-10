/*
 * Copyright (c) 2020. Created by Alexey Rozhkov.
 */

package ru.ar2code.android.architecture.core.android.prepares

import ru.ar2code.android.architecture.core.android.ActorViewModel
import ru.ar2code.android.architecture.core.android.impl.ViewModelStateWithEvent
import ru.ar2code.android.architecture.core.models.IntentMessage
import ru.ar2code.android.architecture.core.services.ServiceStateWithResult

class TestViewModelWithEventOnly : ActorViewModel<TestViewModelState, TestViewModelEvent>(SimpleTestLogger()) {
    override suspend fun onIntentMsg(msg: IntentMessage): ServiceStateWithResult<ViewModelStateWithEvent<TestViewModelState, TestViewModelEvent>> {
        return ServiceStateWithResult(
            TestViewModelInternalOkState(),
            ViewModelStateWithEvent.createViewModelServiceResult<TestViewModelState, TestViewModelEvent>(
                null,
                TestViewModelEvent(TestViewModelEvent.TestViewModelEventType())
            )
        )
    }


}