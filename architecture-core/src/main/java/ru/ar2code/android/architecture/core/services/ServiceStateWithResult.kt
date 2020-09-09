package ru.ar2code.android.architecture.core.services

import ru.ar2code.android.architecture.core.models.ServiceResult

data class ServiceStateWithResult<TResult>(
    val newServiceState: ActorServiceState,
    val result: ServiceResult<TResult>
) where TResult : Any {

    companion object {

        fun <TResult> createBasicResultFrom(
            newState: ActorServiceState,
            result: TResult
        ): ServiceStateWithResult<TResult> where TResult : Any {
            return ServiceStateWithResult(newState, ServiceResult.BasicResult(result))
        }

    }

}