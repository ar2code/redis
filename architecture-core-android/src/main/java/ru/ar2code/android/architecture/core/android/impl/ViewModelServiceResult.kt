/*
 * Copyright (c) 2020. Created by Alexey Rozhkov.
 */

package ru.ar2code.android.architecture.core.android.impl

import ru.ar2code.android.architecture.core.android.BaseViewEvent
import ru.ar2code.android.architecture.core.android.BaseViewState
import ru.ar2code.android.architecture.core.models.ServiceResult

class ViewModelServiceResult<ViewState, ViewEvent>(payload: ViewModelStateWithEvent<ViewState, ViewEvent>) :
    ServiceResult<ViewModelStateWithEvent<ViewState, ViewEvent>>(payload) where ViewState : BaseViewState, ViewEvent : BaseViewEvent