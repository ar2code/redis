/*
 * Copyright (c) 2020. Created by Alexey Rozhkov.
 */

package ru.ar2code.android.architecture.core.android

abstract class ViewEventType<out T>(val payload: T? = null) where T : Any