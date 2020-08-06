package ru.ar2code.android.architecture.core.services

import ru.ar2code.android.architecture.core.models.ServiceResult

data class ServiceResultWithState<T>(
    val state: ActorServiceState,
    val result: ServiceResult<T>
) where T : Any
