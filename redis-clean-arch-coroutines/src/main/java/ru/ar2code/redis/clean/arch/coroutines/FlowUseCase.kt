package ru.ar2code.redis.clean.arch.coroutines

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface FlowUseCase<TParams, TResult> {
    open fun run(params: TParams): Flow<TResult>

    /**
     * Cancel use case execution and throw [UseCaseCancelledException] from flow
     */
    fun cancel()

    /**
     * Return first() result from [run] flow
     */
    suspend fun runSingle(params: TParams): TResult {
        return run(params).first()
    }
}