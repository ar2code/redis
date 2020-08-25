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

