package ru.ar2code.defaults

import kotlinx.coroutines.ExperimentalCoroutinesApi
import ru.ar2code.android.architecture.core.usecases.SynchronizedUseCase

@ExperimentalCoroutinesApi
abstract class DefaultSynchronizedUseCase<TParams, TResult> :
    SynchronizedUseCase<TParams, TResult>(DefaultSynchronizedUseCaseAwaitConfig(), DefaultLogger()) where TResult : Any