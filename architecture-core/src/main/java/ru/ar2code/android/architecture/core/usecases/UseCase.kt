/*
 * Copyright (c) 2020. Created by Alexey Rozhkov.
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