package ru.ar2code.android.architecture.core.impl

import ru.ar2code.android.architecture.core.interfaces.SynchronizedUseCaseAwaitConfig

internal class DefaultSynchronizedUseCaseAwaitConfig :
    SynchronizedUseCaseAwaitConfig {
    override val awaitStepDelayMs: Long
        get() = 10
    override val awaitTimeoutMs: Long
        get() = 60_000
    override val shouldEmitAwaitState: Boolean
        get() = true
}