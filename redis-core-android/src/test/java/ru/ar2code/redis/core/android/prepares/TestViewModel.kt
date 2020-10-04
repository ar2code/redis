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

package ru.ar2code.redis.core.android.prepares

import kotlinx.coroutines.ExperimentalCoroutinesApi
import ru.ar2code.redis.core.android.RedisViewModel
import ru.ar2code.redis.core.android.ViewModelStateWithEvent
import ru.ar2code.redis.core.android.ViewStateReducer
import ru.ar2code.utils.Logger

@ExperimentalCoroutinesApi
class TestViewModel() : RedisViewModel<TestViewModelState, TestViewModelEvent>(
    null
) {

    override val logger: Logger
        get() = SimpleTestLogger()

    override val initialState: ViewModelStateWithEvent<TestViewModelState, TestViewModelEvent>
        get() = ViewModelInitiatedState()

    override val reducers: List<ViewStateReducer<TestViewModelState, TestViewModelEvent>>
        get() = defaultReducers
}