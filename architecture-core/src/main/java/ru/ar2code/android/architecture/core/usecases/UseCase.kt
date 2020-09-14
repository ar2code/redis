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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import ru.ar2code.android.architecture.core.models.UseCaseResult

/**
 * Basic use case with in params, single public method run and out result.
 */
abstract class UseCase<TParams, TResult> where TResult : Any {

    /**
     * Determine is use case was cancelled.
     */
    protected var isCancelled: Boolean = false
        private set

    open fun run(params: TParams?): Flow<UseCaseResult<TResult>> {
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
    fun cancel() {
        isCancelled = true
        onExecutionCancelled()
    }

    protected abstract fun execute(params: TParams? = null): Flow<UseCaseResult<TResult>>

    /**
     * Method invokes after use case was cancelled.
     * You can do some clean up here.
     */
    protected open fun onExecutionCancelled() {}
}