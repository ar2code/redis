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
import ru.ar2code.redis.core.android.ViewModelStateWithEvent
import ru.ar2code.redis.core.coroutines.cast
import ru.ar2code.redis.core.coroutines.castOrNull

class ViewModelInitiatedState(
    viewState: TestViewModelState? = null,
    viewEvent: TestViewModelEvent? = null
) :
    ViewModelStateWithEvent(
        viewState, viewEvent
    ) {
    override fun clone(): State {
        return ViewModelInitiatedState(
            viewState.castOrNull(),
            viewEvent.castOrNull()
        )
    }
}

class ViewModelTypeAState(viewState: TestViewModelState?, viewEvent: TestViewModelEvent?) :
    ViewModelStateWithEvent(
        viewState, viewEvent
    ) {
    override fun clone(): State {
        return ViewModelTypeAState(
            viewState.castOrNull(),
            viewEvent.castOrNull()
        )
    }
}

class ViewModelTypeBState(viewState: TestViewModelState?, viewEvent: TestViewModelEvent?) :
    ViewModelStateWithEvent(
        viewState, viewEvent
    ) {
    override fun clone(): State {
        return ViewModelTypeBState(
            viewState.castOrNull(),
            viewEvent.castOrNull()
        )
    }
}

class ViewModelViewOnlyState(viewState: TestViewModelState) :
    ViewModelStateWithEvent(
        viewState, null
    ) {
    override fun clone(): State {
        return ViewModelViewOnlyState(
            viewState!!.cast()
        )
    }
}

class ViewModelEventOnlyState(viewEvent: TestViewModelEvent) :
    ViewModelStateWithEvent(
        null, viewEvent
    ) {
    override fun clone(): State {
        return ViewModelEventOnlyState(viewEvent!!.cast())
    }
}

class ViewModelViewWithEventState(viewState: TestViewModelState, viewEvent: TestViewModelEvent) :
    ViewModelStateWithEvent(
        viewState, viewEvent
    ) {
    override fun clone(): State {
        return ViewModelViewWithEventState(
            viewState!!.cast(),
            viewEvent!!.cast()
        )
    }
}