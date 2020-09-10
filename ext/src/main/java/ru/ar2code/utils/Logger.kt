/*
 * Copyright (c) 2020. Created by Alexey Rozhkov.
 */

package ru.ar2code.utils

abstract class Logger(private val tag: String?) {
    abstract fun info(msg: String)
    abstract fun error(msg: String, t: Throwable)
    abstract fun warning(msg: String)
}