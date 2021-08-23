/*
 * Copyright (c) 2020.  The Redis Open Source Project
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

package ru.ar2code.redis.core.android.prepares

import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.android.ViewModelErrorStateWithEvent
import ru.ar2code.redis.core.android.ViewModelStateWithEvent

class ViewModelInitiatedState(
    viewState: TestViewModelState? = null,
    viewEvent: TestViewModelEvent? = null
) :
    ViewModelErrorStateWithEvent<TestViewModelState, TestViewModelEvent>(
        viewState, viewEvent
    ) {
    override fun clone(): State {
        return ViewModelInitiatedState(
            viewState,
            viewEvent
        )
    }
}

class ViewModelTypeAState(viewState: TestViewModelState?, viewEvent: TestViewModelEvent?) :
    ViewModelErrorStateWithEvent<TestViewModelState, TestViewModelEvent>(
        viewState, viewEvent
    ) {
    override fun clone(): State {
        return ViewModelTypeAState(
            viewState,
            viewEvent
        )
    }
}

class ViewModelTypeBState(viewState: TestViewModelState?, viewEvent: TestViewModelEvent?) :
    ViewModelErrorStateWithEvent<TestViewModelState, TestViewModelEvent>(
        viewState, viewEvent
    ) {
    override fun clone(): State {
        return ViewModelTypeBState(
            viewState,
            viewEvent
        )
    }
}

class ViewModelViewOnlyState(viewState: TestViewModelState) :
    ViewModelErrorStateWithEvent<TestViewModelState, TestViewModelEvent>(
        viewState, null
    ) {
    override fun clone(): State {
        return ViewModelViewOnlyState(
            viewState!!
        )
    }
}

class ViewModelEventOnlyState(viewEvent: TestViewModelEvent) :
    ViewModelErrorStateWithEvent<TestViewModelState, TestViewModelEvent>(
        null, viewEvent
    ) {
    override fun clone(): State {
        return ViewModelEventOnlyState(viewEvent!!)
    }
}

class ViewModelViewWithEventState(viewState: TestViewModelState, viewEvent: TestViewModelEvent) :
    ViewModelErrorStateWithEvent<TestViewModelState, TestViewModelEvent>(
        viewState, viewEvent
    ) {
    override fun clone(): State {
        return ViewModelViewWithEventState(
            viewState!!,
            viewEvent!!
        )
    }
}