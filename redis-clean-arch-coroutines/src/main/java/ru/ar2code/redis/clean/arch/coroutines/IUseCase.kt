package ru.ar2code.redis.clean.arch.coroutines

import kotlinx.coroutines.flow.Flow

interface IUseCase<TParams, TResult> {
    open fun run(params: TParams): Flow<TResult>

    /**
     * Cancel use case execution and throw [UseCaseCancelledException] from flow
     */
    fun cancel()
}