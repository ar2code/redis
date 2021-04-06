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
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.coroutines.prepares.*
import ru.ar2code.redis.core.coroutines.prepares.Constants.awaitStateTimeout

class AwaitExtTests {

    @Test
    fun `await state method returns expected state`() = runBlocking {

        val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

        async {
            delay(50)
            service.dispatch(IntentTypeDelayFlow())
            delay(10)
            service.dispatch(IntentTypeFlow())
            delay(10)
            service.dispatch(FinishIntent())
        }

        val awaitedState = service.awaitStateWithTimeout(awaitStateTimeout, FlowStateD::class)

        service.dispose()

        Truth.assertThat(awaitedState).isInstanceOf(FlowStateD::class.java)

    }

    @Test(expected = AwaitStateTimeoutException::class)
    fun `await state method throw timeout exception after specified timeout`() = runBlocking {

        val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

        async {
            delay(50)
            service.dispatch(FinishIntent())
        }

        val awaitedState = service.awaitStateWithTimeout(awaitStateTimeout, FlowStateD::class)

        service.dispose()

        Truth.assertThat(awaitedState).isInstanceOf(FlowStateD::class.java)
    }

    @Test(expected = AwaitStateTimeoutException::class)
    fun `await state method throw timeout exception if service disposed`() = runBlocking {

        val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

        async {
            delay(50)

            service.dispatch(FinishIntent())

            service.dispose()
        }

        val awaitedState = service.awaitStateWithTimeout(awaitStateTimeout, FlowStateD::class)

        Truth.assertThat(awaitedState).isInstanceOf(FlowStateD::class.java)
    }

    @Test
    fun `await state method returns disposed state if it was expected`() = runBlocking {

        val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

        async {
            delay(50)

            service.dispatch(FinishIntent())

            service.dispose()
        }

        val awaitedState = service.awaitStateWithTimeout(awaitStateTimeout, State.Disposed::class)

        Truth.assertThat(awaitedState).isInstanceOf(State.Disposed::class.java)
    }
}