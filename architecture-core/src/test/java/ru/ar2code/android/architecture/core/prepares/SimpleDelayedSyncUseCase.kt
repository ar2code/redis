package ru.ar2code.android.architecture.core.prepares

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.ar2code.android.architecture.core.models.UseCaseResult
import ru.ar2code.android.architecture.core.usecases.SynchronizedUseCase

class SimpleDelayedSyncUseCase : SynchronizedUseCase<String, String>(
    DefaultSynchronizedUseCaseAwaitConfig(),
    SimpleTestLogger()
) {

    private var flowParam: String? = null

    override fun execute(params: String?): Flow<UseCaseResult<String>> {
        flowParam = params

        return flow {
            emit(UseCaseResult(params))
            delay(1000)
            emit(UseCaseResult(params))
            delay(1000)
            emit(UseCaseResult(params))
            delay(1000)
            emit(UseCaseResult(params))
        }
    }

    override fun getPayloadForAwaitResult(): String? {
        return flowParam
    }
}