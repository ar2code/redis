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

package ru.ar2code.redis.core.prepares

import ru.ar2code.redis.core.services.SavedStateStore

class TestMemorySavedStateStore : SavedStateStore {

    private val dictionary = HashMap<String, Any>()

    override fun <T> get(key: String): T? {
        return dictionary.get(key) as? T
    }

    override fun <T> set(key: String, value: T?) {
        if (value != null) {
            dictionary.put(key, value)
        } else {
            dictionary.remove(key)
        }
    }

    override fun keys(): List<String> {
        return dictionary.keys.toList()
    }
}