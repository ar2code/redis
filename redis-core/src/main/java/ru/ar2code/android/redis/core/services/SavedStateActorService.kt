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

package ru.ar2code.android.redis.core.services

abstract class SavedStateActorService(
    protected var savedStateHandler: ServiceSavedStateHandler?
) : ActorService {

    /**
     * Set ServiceSavedStateHandler implementation for saving service state due to process kill or even app closing.
     */
    fun setServiceSavedStateHandler(serviceSavedStateHandler: ServiceSavedStateHandler?) {
        savedStateHandler = serviceSavedStateHandler
        restoreState()
    }

    /**
     * You can check [savedStateHandler] and restore previous saved state.
     * Best practice is to save some Id (or any small identical piece of data) that you got inside [onIntentMsg] than restore that Id here and send same intent.
     */
    protected open fun restoreState() {

    }
}