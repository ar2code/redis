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

package ru.ar2code.redis.core.coroutines

import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import ru.ar2code.redis.core.ServiceSubscriber
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.coroutines.prepares.*
import ru.ar2code.redis.core.test.TestMemorySavedStateStore


class SavedRedisStateServiceTests {

    @Test
    fun `Service stores arbitrary data in state storage`() = runBlocking {

        val savedId = 123

        val stateHandler = TestMemorySavedStateStore()

        val service = ServiceFactory.buildSimpleServiceWithSavedStateStore(
            this,
            Dispatchers.Default,
            stateHandler,
            TestSavedStateHandler()
        )

        service.dispatch(IntentTypeB(savedId))

        service.awaitStateWithTimeout(Constants.awaitStateTimeout, StateB::class)

        val storedData = stateHandler.get<Int>(TestSavedStateHandler.STATE_DATA_KEY)

        Truth.assertThat(storedData).isEqualTo(savedId)

        service.dispose()
    }

    @Test
    //todo check test
    fun `Service restores state if state handler contains data`() = runBlocking {

        val savedId = 123

        val stateHandler = TestMemorySavedStateStore()

        val service = ServiceFactory.buildSimpleServiceWithSavedStateStore(
            this,
            Dispatchers.Default,
            stateHandler,
            TestSavedStateHandler()
        )

        service.dispatch(IntentTypeB(savedId))

        service.awaitStateWithTimeout(Constants.awaitStateTimeout, StateB::class)

        val serviceWithRestoring = ServiceFactory.buildSimpleServiceWithSavedStateStore(
            this,
            Dispatchers.Default,
            stateHandler,
            TestSavedStateHandler()
        )

        var isGotInitResult = false
        var isGotPreviousServiceResult = false

        val subscriber = object : ServiceSubscriber {
            override suspend fun onReceive(newState: State) {
                if (newState is State.Initiated) {
                    isGotInitResult = true
                }
                if (newState is StateB) {
                    isGotPreviousServiceResult = newState.data == savedId
                }
            }
        }

        serviceWithRestoring.subscribe(subscriber)

        serviceWithRestoring.awaitStateWithTimeout(Constants.awaitStateTimeout, FlowStateD::class)

        Truth.assertThat(isGotInitResult).isFalse()
        Truth.assertThat(isGotPreviousServiceResult).isTrue()

        service.dispose()
        serviceWithRestoring.dispose()
    }

    @Test
    fun `Service dispatches intent after restoring if intent specified`() = runBlocking {

        val savedId = 123

        val stateHandler = TestMemorySavedStateStore()

        val service = ServiceFactory.buildSimpleServiceWithSavedStateStore(
            this,
            Dispatchers.Default,
            stateHandler,
            TestSavedStateHandler()
        )

        service.dispatch(IntentTypeB(savedId))

        service.awaitStateWithTimeout(Constants.awaitStateTimeout, StateB::class)

        val serviceWithRestoring = ServiceFactory.buildSimpleServiceWithSavedStateStore(
            this,
            Dispatchers.Default,
            stateHandler,
            TestSavedStateHandler()
        )

        var isGotFlowStateAfterRestoring = false

        val subscriber = object : ServiceSubscriber {
            override suspend fun onReceive(newState: State) {
                if (newState is FlowStateD) {
                    isGotFlowStateAfterRestoring = true
                }
            }
        }

        serviceWithRestoring.subscribe(subscriber)

        serviceWithRestoring.awaitStateWithTimeout(Constants.awaitStateTimeout, FlowStateD::class)

        Truth.assertThat(isGotFlowStateAfterRestoring).isTrue()

        service.dispose()
        serviceWithRestoring.dispose()
    }

    @Test
    fun `Service deletes all stored data after disposing`() = runBlocking {

        val savedId = 123

        val stateHandler = TestMemorySavedStateStore()

        stateHandler.set("keep", "it")

        val service = ServiceFactory.buildSimpleServiceWithSavedStateStore(
            this,
            Dispatchers.Default,
            stateHandler,
            TestSavedStateHandler()
        )

        service.dispatch(IntentTypeB(savedId))

        service.awaitStateWithTimeout(Constants.awaitStateTimeout, StateB::class)

        Truth.assertThat(stateHandler.keys().size).isEqualTo(3) //State key and data key + keep it

        service.dispose()

        Truth.assertThat(stateHandler.keys().size)
            .isEqualTo(1) //Stored keys were deleted. Keep it only exists.
        Truth.assertThat(stateHandler.get<String>("keep")).isEqualTo("it") //keep it exists
    }

    @Test
    fun `Test service isServiceRestoredState flag after restoring`() = runBlocking {

        val savedId = 123

        val stateHandler = TestMemorySavedStateStore()

        val service = ServiceFactory.buildSimpleServiceWithSavedStateStore(
            this,
            Dispatchers.Default,
            stateHandler,
            TestSavedStateHandler()
        )

        service.dispatch(IntentTypeB(savedId))

        service.awaitStateWithTimeout(Constants.awaitStateTimeout, StateB::class)

        val serviceWithRestoring = ServiceFactory.buildSimpleServiceWithSavedStateStore(
            this,
            Dispatchers.Default,
            stateHandler,
            TestSavedStateHandler()
        )

        Truth.assertThat(service.isServiceRestoredState()).isFalse()
        Truth.assertThat(serviceWithRestoring.isServiceRestoredState()).isTrue()

        service.dispose()
        serviceWithRestoring.dispose()
    }
}