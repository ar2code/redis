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

package ru.ar2code.android.architecture.core.android.impl

import ru.ar2code.android.architecture.core.android.BaseViewEvent
import ru.ar2code.android.architecture.core.android.BaseViewState

class ViewModelStateWithEvent<ViewState, ViewEvent>(
    val viewState: ViewState?,
    val viewEvent: ViewEvent?
) where ViewState : BaseViewState, ViewEvent : BaseViewEvent {


    companion object {

        fun <ViewState, ViewEvent> createViewModelServiceResult(
            viewState: ViewState?,
            viewEvent: ViewEvent?
        ): ViewModelServiceResult<ViewState, ViewEvent> where ViewState : BaseViewState, ViewEvent : BaseViewEvent {
            return ViewModelStateWithEvent(viewState, viewEvent).toViewModelServiceResult()
        }

    }


    fun toViewModelServiceResult(): ViewModelServiceResult<ViewState, ViewEvent> {
        return ViewModelServiceResult(this)
    }

}

