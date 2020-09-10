/*
 * Copyright (c) 2020. Created by Alexey Rozhkov.
 */

package ru.ar2code.android.architecture.core.services

interface ServiceSubscriber<TResult> where TResult : Any {
    fun onReceive(stateWithResult: ServiceStateWithResult<TResult>?)
}