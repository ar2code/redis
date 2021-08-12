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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.android.prepares.IntentUiTypeB
import ru.ar2code.redis.core.android.prepares.TestRedisErrorViewModelWithException
import ru.ar2code.redis.core.android.prepares.ViewModelInitiatedState
import ru.ar2code.redis.core.coroutines.awaitStateWithTimeout
import ru.ar2code.redis.core.coroutines.cast


class RedisErrorViewModelUnitTest {

    private val awaitTimeout = 5000L

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Test
    fun `view model gets error occurred state if exception throws inside reducer`() = runBlocking {
        val viewModel = TestRedisErrorViewModelWithException()

        viewModel.viewModelService.awaitStateWithTimeout(
            awaitTimeout,
            ViewModelInitiatedState::class
        )

        viewModel.dispatch(IntentUiTypeB())

        viewModel.viewModelService.awaitStateWithTimeout(
            awaitTimeout,
            State.ErrorOccurred::class
        )

        assertThat(viewModel.state).isInstanceOf(State.ErrorOccurred::class.java)
    }

    @Test
    fun `dispatch ReloadAfterErrorIntent if error occurred inside reducer`() = runBlocking {
        val viewModel = TestRedisErrorViewModelWithException()

        viewModel.viewModelService.awaitStateWithTimeout(
            awaitTimeout,
            ViewModelInitiatedState::class
        )

        viewModel.dispatch(IntentUiTypeB())

        viewModel.viewModelService.awaitStateWithTimeout(
            awaitTimeout,
            TestRedisErrorViewModelWithException.OnViewModelErrorIntentReceivedState::class
        )

        assertThat(viewModel.state).isInstanceOf(TestRedisErrorViewModelWithException.OnViewModelErrorIntentReceivedState::class.java)
    }

    @Test
    fun `test view model contains error information`() = runBlocking {
        val viewModel = TestRedisErrorViewModelWithException()

        viewModel.viewModelService.awaitStateWithTimeout(
            awaitTimeout,
            ViewModelInitiatedState::class
        )

        viewModel.dispatch(IntentUiTypeB())

        viewModel.viewModelService.awaitStateWithTimeout(
            awaitTimeout,
            State.ErrorOccurred::class
        )

        val error = viewModel.state.cast<State.ErrorOccurred>()

        assertThat(error.throwable).isInstanceOf(TestRedisErrorViewModelWithException.TestRedisViewModelThrowable::class.java)
        assertThat(error.intent).isInstanceOf(IntentUiTypeB::class.java)
        assertThat(error.currentState).isInstanceOf(ViewModelInitiatedState::class.java)
    }
}