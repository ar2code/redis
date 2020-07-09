package ru.ar2code.android.architecture.core.prepares

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.ar2code.android.architecture.core.models.UseCaseResult
import ru.ar2code.android.architecture.core.usecases.SynchronizedUseCase

class SimpleExceptionUseCase : SynchronizedUseCase<String, Int>() {
    override fun execute(params: String?): Flow<UseCaseResult<Int>> {
        return flow {
            throw Exception(params)
        }
    }
}