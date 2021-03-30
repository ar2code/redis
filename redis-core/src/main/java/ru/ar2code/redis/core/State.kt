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

package ru.ar2code.redis.core

import ru.ar2code.utils.LoggableObject

/**
 * Plain object that describes service's state.
 */
abstract class State : LoggableObject {

    open val stateName : String = this::class.simpleName!!

    /**
     * To protect from state data changing from outside, service emits a clone from current state.
     */
    abstract fun clone(): State

    class Created : State() {
        override fun clone(): State {
            return Created()
        }
    }

    class Initiated : State() {
        override fun clone(): State {
            return Initiated()
        }
    }

    class Disposed : State() {
        override fun clone(): State {
            return Disposed()
        }
    }

}