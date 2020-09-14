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

package ru.ar2code.android.redis.core.android

/**
 * todo comment here about this class
 */
abstract class ChangeableState<T>(
    val state: T?,

    /**
     * True means that [state] was changed since previous time. You need to update UI with a new [state]
     * False means nothing was changed and you should ignore this state assume that UI is up-to-date with state
     */
    val isChangedSincePrevious: Boolean
) : BaseViewState() where T : Any