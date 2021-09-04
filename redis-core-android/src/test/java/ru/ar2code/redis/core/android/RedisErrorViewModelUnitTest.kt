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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import ru.ar2code.redis.core.ServiceStateListener
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.android.prepares.*
import ru.ar2code.redis.core.coroutines.awaitStateWithTimeout
import ru.ar2code.redis.core.coroutines.cast


class RedisErrorViewModelUnitTest {

    private val awaitTimeout = 5000L

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Test
    fun `view model gets error occurred state if exception throws inside reducer`() = runBlocking {
        val viewModel = TestRedisErrorViewModelWithException(
            listOf(
                InitiatedStateTypeAReducer(),
                TestRedisErrorViewModelWithException.InitiatedStateTypeBExceptionReducer(),
                TestRedisErrorViewModelWithException.ErrorOccurredOnViewModelErrorNullReducer()
            )
        )

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
    fun `dispatch OnViewModelErrorIntent if error occurred inside reducer`() = runBlocking {
        val viewModel = TestRedisErrorViewModelWithException(
            listOf(
                InitiatedStateTypeAReducer(),
                TestRedisErrorViewModelWithException.InitiatedStateTypeBExceptionReducer(),
                TestRedisErrorViewModelWithException.ErrorOccurredOnViewModelErrorEmitViewModelErrorStateReducer()
            )
        )

        viewModel.viewModelService.awaitStateWithTimeout(
            awaitTimeout,
            ViewModelInitiatedState::class
        )

        viewModel.dispatch(IntentUiTypeB())

        viewModel.viewModelService.awaitStateWithTimeout(
            awaitTimeout,
            TestRedisErrorViewModelWithException.ViewModelErrorState::class
        )

        assertThat(viewModel.state).isInstanceOf(TestRedisErrorViewModelWithException.ViewModelErrorState::class.java)
    }

    @Test
    fun `test view model contains error information when unhandled error occurred inside view model`() =
        runBlocking {
            val viewModel = TestRedisErrorViewModelWithException(
                listOf(
                    InitiatedStateTypeAReducer(),
                    TestRedisErrorViewModelWithException.InitiatedStateTypeBExceptionReducer(),
                    TestRedisErrorViewModelWithException.ErrorOccurredOnViewModelErrorEmitViewModelErrorStateReducer()
                )
            )

            viewModel.viewModelService.awaitStateWithTimeout(
                awaitTimeout,
                ViewModelInitiatedState::class
            )

            viewModel.dispatch(IntentUiTypeB())

            viewModel.viewModelService.awaitStateWithTimeout(
                awaitTimeout,
                TestRedisErrorViewModelWithException.ViewModelErrorState::class
            )

            val error =
                viewModel.state.cast<TestRedisErrorViewModelWithException.ViewModelErrorState>().error

            assertThat(error.throwable).isInstanceOf(TestRedisErrorViewModelWithException.TestRedisViewModelThrowable::class.java)
            assertThat(error.intent).isInstanceOf(IntentUiTypeB::class.java)
            assertThat(error.stateBeforeError).isInstanceOf(ViewModelInitiatedState::class.java)
        }

    @Test
    fun `test view model gets error state if listening service error occurred`() = runBlocking {
        val listeningService = TestServiceWithException(this, Dispatchers.Default)

        val viewModel = TestRedisErrorViewModelWithException(
            listOf(
                InitiatedStateTypeAReducer(),
                TestRedisErrorViewModelWithException.InitiatedStateTypeBExceptionReducer(),
                TestRedisErrorViewModelWithException.ErrorOccurredOnViewModelErrorNullReducer(),
                TestRedisErrorViewModelWithException.ViewModelTypeAStateOnListeningServiceErrorIntentEmitServiceStateReducer()
            )
        )

        val listener = ServiceStateListener(
            listeningService, mapOf(
                State.Initiated::class to IntentUiTypeA.createBuilder()
            )
        )
        viewModel.listenWithErrorHandling(listener)

        listeningService.dispatch(TestServiceWithException.SomeIntent())

        viewModel.viewModelService.awaitStateWithTimeout(
            awaitTimeout,
            TestRedisErrorViewModelWithException.ServiceErrorState::class
        )

        assertThat(viewModel.state).isInstanceOf(TestRedisErrorViewModelWithException.ServiceErrorState::class.java)

        viewModel.stopListening(listener)
        listeningService.dispose()
    }

    @Test
    fun `test view model contains correct error information if listening service error occurred`() =
        runBlocking {
            val listeningService = TestServiceWithException(this, Dispatchers.Default)

            val viewModel = TestRedisErrorViewModelWithException(
                listOf(
                    InitiatedStateTypeAReducer(),
                    TestRedisErrorViewModelWithException.InitiatedStateTypeBExceptionReducer(),
                    TestRedisErrorViewModelWithException.ErrorOccurredOnViewModelErrorNullReducer(),
                    TestRedisErrorViewModelWithException.ViewModelTypeAStateOnListeningServiceErrorIntentEmitServiceStateReducer()
                )
            )

            val listener = ServiceStateListener(
                listeningService, mapOf(
                    State.Initiated::class to IntentUiTypeA.createBuilder()
                )
            )
            viewModel.listenWithErrorHandling(listener)

            listeningService.dispatch(TestServiceWithException.SomeIntent())

            viewModel.viewModelService.awaitStateWithTimeout(
                awaitTimeout,
                TestRedisErrorViewModelWithException.ServiceErrorState::class
            )

            val error =
                viewModel.state.cast<TestRedisErrorViewModelWithException.ServiceErrorState>().error

            assertThat(error.throwable).isInstanceOf(TestServiceWithException.SomeError::class.java)
            assertThat(error.intent).isInstanceOf(TestServiceWithException.SomeIntent::class.java)
            assertThat(error.stateBeforeError).isInstanceOf(State.Initiated::class.java)

            viewModel.stopListening(listener)
            listeningService.dispose()
        }

    @Test
    fun givenErrorState_WhenTryAgainAfterError_ThenGetsReloadingAfterErrorState() = runBlocking {
        val viewModel = TestRedisErrorViewModelWithException(
            listOf(
                InitiatedStateTypeAReducer(),
                TestRedisErrorViewModelWithException.InitiatedStateTypeBExceptionReducer(),
                TestRedisErrorViewModelWithException.ErrorOccurredOnViewModelErrorEmitErrorStateReducer()
            )
        )

        viewModel.viewModelService.awaitStateWithTimeout(
            awaitTimeout,
            ViewModelInitiatedState::class
        )

        viewModel.dispatch(IntentUiTypeB())

        viewModel.viewModelService.awaitStateWithTimeout(
            awaitTimeout,
            RedisErrorViewModel.ErrorState::class
        )

        val currentUiState =
            viewModel.state.cast<RedisErrorViewModel.ErrorState<TestViewModelState, TestViewModelEvent>>().viewState

        viewModel.tryAgainAfterError()

        viewModel.viewModelService.awaitStateWithTimeout(
            awaitTimeout,
            RedisErrorViewModel.ReloadingAfterErrorState::class
        )

        val reloadingState =
            viewModel.state.cast<RedisErrorViewModel.ReloadingAfterErrorState<TestViewModelState, TestViewModelEvent>>()

        assertThat(reloadingState.viewState).isEqualTo(currentUiState)
    }
}