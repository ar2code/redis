package ru.ar2code.android.architecture.core.usecases

import kotlinx.coroutines.flow.Flow
import ru.ar2code.android.architecture.core.models.UseCaseResult

/**
 * Basic use case with in params, single public method run and out result.
 */
abstract class UseCase<TParams, TResult> where TResult : Any {

    open fun run(params: TParams?): Flow<UseCaseResult<TResult>> {
        return execute(params)
    }

    protected abstract fun execute(params: TParams? = null): Flow<UseCaseResult<TResult>>
}