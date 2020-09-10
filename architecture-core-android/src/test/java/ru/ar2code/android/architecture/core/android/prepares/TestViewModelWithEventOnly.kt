/*
 * Copyright (c) 2020.  The Redim Open Source Project
 * Author: Alexey Rozhkov https://github.com/ar2code
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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