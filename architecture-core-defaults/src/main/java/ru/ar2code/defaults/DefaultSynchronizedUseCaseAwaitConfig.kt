/*
 * Copyright (c) 2020. Created by Alexey Rozhkov.
 */

package ru.ar2code.defaults

import ru.ar2code.android.architecture.core.interfaces.SynchronizedUseCaseAwaitConfig

class DefaultSynchronizedUseCaseAwaitConfig :
    SynchronizedUseCaseAwaitConfig {
    override val awaitStepDelayMs: Long
        get() = 10
    override val awaitTimeoutMs: Long
        get() = 60_000
    override val shouldEmitAwaitState: Boolean
        get() = true
}