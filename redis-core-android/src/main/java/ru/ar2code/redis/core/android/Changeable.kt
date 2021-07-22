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

package ru.ar2code.redis.core.android

import androidx.annotation.VisibleForTesting

/**
 * Data class that contains some data with flag that indicates is data was changed or not.
 */
data class Changeable<T>(
    val data: T?,

    /**
     * You can keep data version inside UI and check version to decide do you need update UI or not.
     * If version is the same, you do not need to update UI.
     */
    val version: Int = INIT_VERSION
) where T : Any {

    companion object {
        @VisibleForTesting
        const val INIT_VERSION = 1
    }

    fun generateUpperVersion(): Int {
        return version + 1
    }

    //todo test
    fun shouldBeRendered(previous: Changeable<T>?): Boolean {
        return data != null && version != previous?.version
    }
}

//todo test ext

fun <T> Changeable<T>?.getUpperVersion(): Int where T : Any {
    return this?.generateUpperVersion() ?: Changeable.INIT_VERSION
}

fun <T> Changeable<T>?.getCurrentOrInitVersion(): Int where T : Any {
    return this?.version ?: Changeable.INIT_VERSION
}

fun <T> Changeable<T>?.orEmpty(data: T? = null): Changeable<T> where T : Any {
    return this ?: Changeable(data)
}