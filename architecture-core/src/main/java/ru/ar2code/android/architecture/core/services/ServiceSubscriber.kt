package ru.ar2code.android.architecture.core.services

import ru.ar2code.android.architecture.core.models.ServiceResult

interface ServiceSubscriber<TResult> where TResult : Any {
    fun onReceive(result: ServiceResult<TResult>?)
}