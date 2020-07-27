package ru.ar2code.android.architecture.core.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import ru.ar2code.android.architecture.core.impl.DefaultLogger

/**
 * Actor Service with default [dispatcher] = [Dispatchers.Default] and [logger] = [DefaultLogger]
 */
abstract class DefaultActorService<TResult>(scope: CoroutineScope) :
    ActorService<TResult>(scope, Dispatchers.Default, DefaultLogger()) where TResult : Any