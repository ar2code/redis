/*
 * Copyright (c) 2020. Created by Alexey Rozhkov.
 */

package ru.ar2code.defaults

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import ru.ar2code.android.architecture.core.services.ActorService
import ru.ar2code.android.architecture.core.services.ServiceSavedStateHandler

/**
 * Actor Service with default [dispatcher] = [Dispatchers.Default] and [logger] = [DefaultLogger]
 */
abstract class DefaultActorService<TResult>(scope: CoroutineScope, savedStateHandler: ServiceSavedStateHandler? = null) :
    ActorService<TResult>(scope, Dispatchers.Default, savedStateHandler, DefaultLogger()) where TResult : Any