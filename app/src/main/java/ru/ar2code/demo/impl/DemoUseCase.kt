package ru.ar2code.demo.impl

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.ar2code.android.architecture.core.usecases.SynchronizedUseCase
import ru.ar2code.android.architecture.core.interfaces.SynchronizedUseCaseAwaitConfig
import ru.ar2code.android.architecture.core.models.UseCaseResult

@ExperimentalCoroutinesApi
class DemoUseCase : SynchronizedUseCase<String, String>() {

    override val awaitConfig: SynchronizedUseCaseAwaitConfig
        get() = object :
            SynchronizedUseCaseAwaitConfig {
            override val awaitStepDelayMs: Long
                get() = 10
            override val awaitTimeoutMs: Long
                get() = 10000
            override val shouldEmitAwaitState: Boolean
                get() = true
        }

    private var i = 0

    override fun execute(params: String?): Flow<UseCaseResult<String>> = flow {
        emit(UseCaseResult("Start flow $params from class = ${this@DemoUseCase}"))

        delay(1000 * (4 - (params?.toLongOrNull() ?: 1)))

        emit(UseCaseResult("flow $params increment i"))

        i++

        delay(1000 * (4 - (params?.toLongOrNull() ?: 1)))

        emit(UseCaseResult("flow $params i = $i"))

        delay(1000 * (4 - (params?.toLongOrNull() ?: 1)))

        emit(UseCaseResult("End flow $params"))

    }

    override fun getPayloadForAwaitResult(): String? {
        return "demo await at ${System.currentTimeMillis()}"
    }
}