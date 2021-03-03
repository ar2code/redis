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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

/**
 * Basic use case with input parameters, single public method run and out result.
 */
abstract class UseCase<TParams, TResult> : IUseCase<TParams, TResult> {

    /**
     * Determines is use case was cancelled.
     */
    protected var isCancelled: Boolean = false
        private set

    override fun run(params: TParams): Flow<TResult> {
        isCancelled = false

        return execute(params)
            .onEach {
                if (isCancelled) {
                    throw UseCaseCancelledException("Use case $this was cancelled")
                }
            }
    }

    /**
     * Cancel use case execution and throw [UseCaseCancelledException] from flow
     */
    override fun cancel() {
        isCancelled = true
        onExecutionCancelled()
    }

    protected abstract fun execute(params: TParams): Flow<TResult>

    /**
     * Method invokes after use case was cancelled.
     * You can do some clean up here.
     */
    protected open fun onExecutionCancelled() {}
}