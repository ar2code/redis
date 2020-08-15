package ru.ar2code.android.architecture.core.android.impl

import ru.ar2code.android.architecture.core.android.BaseViewEvent
import ru.ar2code.android.architecture.core.android.BaseViewState
import ru.ar2code.android.architecture.core.models.ServiceResult

class ViewModelStateWithEvent<ViewState, ViewEvent>(
    val viewState: ViewState?,
    val viewEvent: ViewEvent?
) where ViewState : BaseViewState, ViewEvent : BaseViewEvent

class ViewModelServiceResult<ViewState, ViewEvent>(payload: ViewModelStateWithEvent<ViewState, ViewEvent>) :
    ServiceResult<ViewModelStateWithEvent<ViewState, ViewEvent>>(payload) where ViewState : BaseViewState, ViewEvent : BaseViewEvent