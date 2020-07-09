package ru.ar2code.android.architecture.core.interfaces

/**
 * Sync parameters for SynchronizedUseCase
 *
 * @property awaitStepDelayMs - delay in milliseconds on every awaiting step
 * @property awaitTimeoutMs - throw exception after that time in milliseconds
 * @property shouldEmitAwaitState - emit special value that indicates flow is awaiting finishing of previous execution
 */
interface SynchronizedUseCaseAwaitConfig {

    val awaitStepDelayMs : Long

    val awaitTimeoutMs : Long

    val shouldEmitAwaitState : Boolean
}