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

import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.android.Changeable
import ru.ar2code.redis.core.android.RedisErrorViewState
import ru.ar2code.redis.core.android.RedisViewState
import ru.ar2code.redis.core.coroutines.castOrNull

class TestViewModelState(override val error: Changeable<State.ErrorOccurred>) :
    RedisErrorViewState {

    override fun isErrorShouldBeRendered(currentUiState: RedisErrorViewState?): Boolean {
        return super.isErrorShouldBeRendered(currentUiState)
    }

    override fun updateErrorVersion(serviceError: State.ErrorOccurred): RedisErrorViewState {
        return TestViewModelState(Changeable(error.data, error.generateUpperVersion()))
    }

    override fun clearError(): RedisErrorViewState {
        return TestViewModelState(Changeable(null, error.generateUpperVersion()))
    }

    override fun equals(other: Any?): Boolean {
        return error.data == other.castOrNull<TestViewModelState>()?.error?.data
    }

    override fun hashCode(): Int {
        return error.hashCode()
    }
}