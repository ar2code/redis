/*
 * Copyright (c) 2020. Created by Alexey Rozhkov.
 */

package ru.ar2code.android.architecture.core.models

/**
 * Use case result with generic payload with valuable data.
 */
open class UseCaseResult<Type>(val payload: Type? = null) where Type : Any {

    /**
     * Special [UseCaseResult] that indicates current flow is awaiting previous execution finishing.
     */
    class AwaitAnotherRunFinish<Type>(payload: Type? = null) :
        UseCaseResult<Type>(payload) where Type : Any

}