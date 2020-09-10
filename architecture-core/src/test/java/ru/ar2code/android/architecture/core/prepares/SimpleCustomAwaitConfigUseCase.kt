package ru.ar2code.android.architecture.core.prepares

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.ar2code.android.architecture.core.interfaces.SynchronizedUseCaseAwaitConfig
import ru.ar2code.android.architecture.core.models.UseCaseResult
import ru.ar2code.android.architecture.core.usecases.SynchronizedUseCase

@ExperimentalCoroutinesApi
class SimpleCustomAwaitConfigUseCase(private val timeoutMs :Long = 60_000L) : SynchronizedUseCase<String, String>(
    DefaultSynchronizedUseCaseAwaitConfig(),
    SimpleTestLogger()
) {

    override val awaitConfig: SynchronizedUseCaseAwaitConfig
        get() = object : SynchronizedUseCaseAwaitConfig {
            override val awaitStepDelayMs: Long
                get() = 10
            override val awaitTimeoutMs: Long
                get() = timeoutMs
            override val shouldEmitAwaitState: Boolean
                get() = false

        }

    override fun execute(params: String?): Flow<UseCaseResult<String>> {
        return flow {
            emit(UseCaseResult(params))
            delay(1000)
            emit(UseCaseResult(params))
        }
    }
}