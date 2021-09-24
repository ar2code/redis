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

package ru.ar2code.utils

/**
 * Logger for Redis framework.
 */
abstract class Logger(private val tag: String?) {

    companion object {
        const val DEFAULT_LEVEL = "Redis-default"
    }

    abstract fun info(
        msg: String,
        level: String = DEFAULT_LEVEL,
        where: LoggableObject? = null,
        labels: List<String>? = null
    )

    abstract fun error(
        msg: String,
        t: Throwable,
        level: String = DEFAULT_LEVEL,
        where: LoggableObject? = null,
        labels: List<String>? = null
    )

    abstract fun warning(
        msg: String,
        level: String = DEFAULT_LEVEL,
        where: LoggableObject? = null,
        labels: List<String>? = null
    )
}