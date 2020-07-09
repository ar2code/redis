package ru.ar2code.demo.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.ar2code.android.architecture.core.models.UseCaseResult
import ru.ar2code.android.architecture.core.usecases.SynchronizedUseCase

class SimpleUseCase  : SynchronizedUseCase<String, String>() {
    override fun execute(params: String?): Flow<UseCaseResult<String>> {
        return flow {
            emit(UseCaseResult(params))
            kotlinx.coroutines.delay(100)
        }
    }
}