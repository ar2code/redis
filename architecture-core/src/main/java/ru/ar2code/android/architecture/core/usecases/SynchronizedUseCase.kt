/*
 * Copyright (c) 2020.  The Redis Open Source Project
 * Author: Alexey Rozhkov https://github.com/ar2code
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.ar2code.android.architecture.core.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeout
import ru.ar2code.android.architecture.core.interfaces.SynchronizedUseCaseAwaitConfig
import ru.ar2code.android.architecture.core.models.UseCaseResult
import ru.ar2code.utils.Logger
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Use case that run execution in sync mode.
 * If you run use case flow several times execution will go sequentially.
 * All next flows will await when previous flow finishes.
 * This allow you to consider Use case state as Local State.
 *
 * @param awaitConfig synchronization parameters
 */
@ExperimentalCoroutinesApi
abstract class SynchronizedUseCase<TParams, TResult>(
    protected open val awaitConfig: SynchronizedUseCaseAwaitConfig,
    protected open val logger : Logger
) :
    UseCase<TParams, TResult>() where TResult : Any {

    private val isExecuting = AtomicBoolean(false)

    override fun run(params: TParams?): Flow<UseCaseResult<TResult>> {
        return super.run(params)
            .onStart {
                awaitPreviousFlow(this)
            }
            .onCompletion { ex ->
                onExecutionFinished()
                propagateExceptionIfOccurred(ex)
            }
    }

    /**
     * When flow is awaiting previous execution it may emit [UseCaseResult.AwaitAnotherRunFinish].
     * You can provide payload for it.
     */
    protected open fun getPayloadForAwaitResult(): TResult? = null

    private suspend fun awaitPreviousFlow(flowCollector: FlowCollector<UseCaseResult<TResult>>) {

        suspend fun emitAwaitAnotherRunFinish(alreadyEmitAwait: Boolean): Boolean {
            if (awaitConfig.shouldEmitAwaitState && !alreadyEmitAwait) {
                flowCollector.emit(UseCaseResult.AwaitAnotherRunFinish(getPayloadForAwaitResult()))
            }
            return true
        }

        suspend fun awaitIsExecutingBecomesFree() {
            withTimeout(awaitConfig.awaitTimeoutMs) {
                var alreadyEmitAwait = false
                while (!isExecuting.compareAndSet(false, true)) {
                    alreadyEmitAwait = emitAwaitAnotherRunFinish(alreadyEmitAwait)
                    delay(awaitConfig.awaitStepDelayMs)
                }
            }
        }

        awaitIsExecutingBecomesFree()
    }

    private fun onExecutionFinished() {
        isExecuting.set(false)
    }

    private fun propagateExceptionIfOccurred(throwable: Throwable?) {
        if (throwable != null) {
            throw throwable
        }
    }
}