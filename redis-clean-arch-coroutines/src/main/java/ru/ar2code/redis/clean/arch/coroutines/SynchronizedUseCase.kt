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

package ru.ar2code.redis.clean.arch.coroutines

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeout
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

abstract class SynchronizedUseCase<TParams, TResult>(
    protected open val awaitConfig: SynchronizedUseCaseAwaitConfig,
    protected open val logger : Logger
) :
    UseCase<TParams, TResult>() where TResult : Any {

    private val isExecuting = AtomicBoolean(false)

    override fun run(params: TParams?): Flow<TResult> {
        return super.run(params)
            .onStart {
                awaitPreviousFlow()
            }
            .onCompletion { ex ->
                onExecutionFinished()
                propagateExceptionIfOccurred(ex)
            }
    }

    private suspend fun awaitPreviousFlow() {
        suspend fun awaitIsExecutingBecomesFree() {
            withTimeout(awaitConfig.awaitTimeoutMs) {
                while (!isExecuting.compareAndSet(false, true)) {
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