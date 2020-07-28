package ru.ar2code.defaults

import ru.ar2code.android.architecture.core.usecases.SynchronizedUseCase

abstract class DefaultSynchronizedUseCase<TParams, TResult> :
    SynchronizedUseCase<TParams, TResult>(DefaultSynchronizedUseCaseAwaitConfig()) where TResult : Any