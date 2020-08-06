package ru.ar2code.android.architecture.core.android.impl

import ru.ar2code.android.architecture.core.android.BaseViewEvent
import ru.ar2code.android.architecture.core.android.EmptyViewState
import ru.ar2code.android.architecture.core.models.ServiceResult

class ActorViewModelServiceResult<ViewState, ViewEvent>(
    val viewState: ViewState,
    val viewEvent: ViewEvent
) where ViewState : EmptyViewState, ViewEvent : BaseViewEvent

class ActorViewModelServiceResultValue<ViewState, ViewEvent>(payload: ActorViewModelServiceResult<ViewState, ViewEvent>) :
    ServiceResult<ActorViewModelServiceResult<ViewState, ViewEvent>>(payload) where ViewState : EmptyViewState, ViewEvent : BaseViewEvent