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
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import ru.ar2code.redis.core.android.prepares.*


class RedisViewModelUnitTest {

    private val delayBeforeAssertMs = 100L

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Test
    fun `view model was created then view model get initiated state`() = runBlocking {
        val viewModel = TestViewModel()

        delay(delayBeforeAssertMs)

        assertThat(viewModel.state).isInstanceOf(ViewModelInitiatedState::class.java)
    }

    @Test
    fun `view model with initiated state and dispatch intent A then view model change state to State A`() = runBlocking {
        val viewModel = TestViewModel()

        viewModel.dispatch(IntentUiTypeA())

        delay(delayBeforeAssertMs)

        assertThat(viewModel.state).isInstanceOf(ViewModelTypeAState::class.java)
        assertThat(viewModel.stateLive.value).isInstanceOf(ViewModelTypeAState::class.java)
    }

    @Test
    fun `view model with initiated state and dispatch intent B then view model change state to State B`() = runBlocking {
        val viewModel = TestViewModel()

        viewModel.dispatch(IntentUiTypeB())

        delay(delayBeforeAssertMs)

        assertThat(viewModel.state).isInstanceOf(ViewModelTypeBState::class.java)
        assertThat(viewModel.stateLive.value).isInstanceOf(ViewModelTypeBState::class.java)
    }

    @Test
    fun `view model receive state model only then viewStateLive is not null and viewEventLive is null`() = runBlocking {
        val viewModel = TestViewModelWithRedisOnly()

        viewModel.viewStateLive.observeForever {}
        viewModel.viewEventLive.observeForever {}

        viewModel.dispatch(IntentUiViewStateOnly())

        delay(delayBeforeAssertMs)

        assertThat(viewModel.state).isInstanceOf(ViewModelViewOnlyState::class.java)

        val state = viewModel.state as ViewModelViewOnlyState

        assertThat(state.viewState).isNotNull()
        assertThat(state.viewEvent).isNull()
    }

    @Test
    fun `view model receive event model only then viewStateLive is null and viewEventLive is not null`() = runBlocking {
        val viewModel = TestViewModelWithRedisOnly()

        viewModel.viewStateLive.observeForever {}
        viewModel.viewEventLive.observeForever {}

        viewModel.dispatch(IntentUiViewEventOnly())

        delay(delayBeforeAssertMs)

        assertThat(viewModel.state).isInstanceOf(ViewModelEventOnlyState::class.java)

        val state = viewModel.state as ViewModelEventOnlyState

        assertThat(state.viewState).isNull()
        assertThat(state.viewEvent).isNotNull()
    }

    @Test
    fun `view model receive state and event models then viewStateLive is not null and viewEventLive is not null`() = runBlocking {
        val viewModel = TestViewModelWithRedisOnly()

        viewModel.viewStateLive.observeForever {}
        viewModel.viewEventLive.observeForever {}

        viewModel.dispatch(IntentUiViewStateWithEvent())

        delay(delayBeforeAssertMs)

        assertThat(viewModel.state).isInstanceOf(ViewModelViewWithEventState::class.java)

        val state = viewModel.state as ViewModelViewWithEventState

        assertThat(state.viewState).isNotNull()
        assertThat(state.viewEvent).isNotNull()
    }
}