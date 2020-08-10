package ru.ar2code.android.architecture.core.services

import ru.ar2code.android.architecture.core.models.ServiceResult

data class ServiceStateWithResult<TResult>(val newServiceState: ActorServiceState, val result : ServiceResult<TResult>) where TResult : Any