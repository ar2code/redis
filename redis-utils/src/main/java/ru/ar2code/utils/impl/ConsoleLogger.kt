/*
 * Copyright (c) 2020.  The Redis Open Source Project
 * Author: Alexey Rozhkov https://github.com/ar2code
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.ar2code.utils.impl

import ru.ar2code.utils.LoggableObject
import ru.ar2code.utils.Logger
import java.util.logging.Level

/**
 * Simple logger for Redis framework based on [java.util.logging.Logger]
 */
open class ConsoleLogger(tag: String) : Logger(tag) {

    open val javaLogger = java.util.logging.Logger.getLogger(tag).apply {
        level = Level.ALL
    } ?: null

    override fun info(msg: String, level: String, where: LoggableObject?) {
        javaLogger?.log(Level.INFO, buildMessage(msg, level, where))
    }

    override fun error(msg: String, t: Throwable, level: String, where: LoggableObject?) {
        javaLogger?.log(Level.SEVERE, buildMessage(msg, level, where), t)
    }

    override fun warning(msg: String, level: String, where: LoggableObject?) {
        javaLogger?.log(Level.WARNING, buildMessage(msg, level, where))
    }

    private fun buildMessage(msg: String, level: String, where: LoggableObject?): String {
        return "[$level]/$where: $msg"
    }
}