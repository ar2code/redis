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
import kotlinx.coroutines.*
import org.junit.Test
import ru.ar2code.redis.core.ServiceStateListener
import ru.ar2code.redis.core.ServiceSubscriber
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.coroutines.prepares.*
import ru.ar2code.redis.core.coroutines.prepares.Constants.testDelayBeforeCheckingResult
import ru.ar2code.redis.core.coroutines.test.awaitWhileNotDisposed
import ru.ar2code.redis.core.coroutines.test.disposeServiceWhenIntentDispatched

class ListenServiceTest {
    @Test
    fun `service A listen another service B then service A receive specified intent when service B state changed`() =
        runBlocking {
            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)
            service.disposeServiceWhenIntentDispatched(
                FinishIntent::class,
                testDelayBeforeCheckingResult
            )

            val listenedService = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

            var stateBCount = 0

            val subscriber = object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {
                    if (newState is StateB) {
                        stateBCount++
                    }
                }
            }

            service.subscribe(subscriber)

            service.listen(
                ServiceStateListener(listenedService, mapOf(null to IntentTypeBBuilder()))
            )

            listenedService.dispatch(IntentTypeA())

            delay(testDelayBeforeCheckingResult)

            service.dispatch(FinishIntent())

            service.awaitWhileNotDisposed()

            listenedService.dispose()

            Truth.assertThat(stateBCount)
                .isEqualTo(2) //First time when listenedService initiated and second time when dispatch intent

        }

    @Test
    fun `service A stopped listen service B then service A will not receive specified intent when service B state changed`() =
        runBlocking {
            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)
            service.disposeServiceWhenIntentDispatched(
                FinishIntent::class,
                testDelayBeforeCheckingResult
            )

            val listenedService = ServiceFactory.buildSimpleService(this, Dispatchers.Default)
            val listenedServiceInfo =
                ServiceStateListener(listenedService, mapOf(null to IntentTypeBBuilder()))

            var stateBCount = 0

            val subscriber = object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {
                    if (newState is StateB) {
                        stateBCount++
                    }
                }
            }

            service.subscribe(subscriber)

            service.listen(listenedServiceInfo)

            delay(testDelayBeforeCheckingResult)

            service.stopListening(listenedServiceInfo)

            listenedService.dispatch(IntentTypeA())

            service.dispatch(FinishIntent())

            service.awaitWhileNotDisposed()

            listenedService.dispose()

            Truth.assertThat(stateBCount)
                .isEqualTo(1) //First time when listenedService initiated. And ignore second time when dispatch intent.
        }

    @Test
    fun `service A does not listen any services then stop listening unknown service does nothing`() =
        runBlocking {

            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)
            service.disposeServiceWhenIntentDispatched(
                FinishIntent::class,
                testDelayBeforeCheckingResult
            )

            val listenedService = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

            val listenedServiceInfo =
                ServiceStateListener(listenedService, mapOf(null to IntentTypeBBuilder()))

            var stateBCount = 0

            val subscriber = object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {
                    if (newState is StateB) {
                        stateBCount++
                    }
                }
            }

            service.subscribe(subscriber)
            service.stopListening(listenedServiceInfo)

            listenedService.dispatch(IntentTypeA())

            delay(testDelayBeforeCheckingResult)

            service.dispatch(FinishIntent())

            service.awaitWhileNotDisposed()

            listenedService.dispose()

            Truth.assertThat(stateBCount).isEqualTo(0)

        }

    @Test
    fun `service A listen another service B and service A is disposed then service A stop listen service B no any exceptions`() =
        runBlocking {

            val serviceAScope = CoroutineScope(Dispatchers.Default + Job())

            val service =
                ServiceFactory.buildSimpleService(serviceAScope, Dispatchers.Default, "Service A")
            service.disposeServiceWhenIntentDispatched(
                FinishIntent::class,
                testDelayBeforeCheckingResult
            )

            val listenedService =
                ServiceFactory.buildSimpleService(this, Dispatchers.Default, "Service B")

            val serviceBDispatchCount = 1000
            var serviceAReceiveCount = 0
            var serviceBReceiveCount = 0

            val subscriber = object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {
                    serviceAReceiveCount++
                }
            }

            val listenedServiceSubscriber = object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {
                    if (newState is FinishState) {
                        listenedService.dispose()
                    } else {
                        serviceBReceiveCount++
                        if (serviceBReceiveCount > serviceBDispatchCount / 2) {
                            service.dispose()
                        }
                    }
                }
            }

            listenedService.subscribe(listenedServiceSubscriber)
            service.subscribe(subscriber)

            service.listen(
                ServiceStateListener(listenedService, mapOf(null to IntentTypeBBuilder()))
            )

            async {
                repeat(serviceBDispatchCount) {
                    listenedService.dispatch(CircleIntent())
                }

                delay(testDelayBeforeCheckingResult)
                listenedService.dispatch(FinishIntent())
            }

            service.awaitWhileNotDisposed()

            println("service A changed state times : $serviceAReceiveCount")

            Truth.assertThat(serviceAReceiveCount)
                .isGreaterThan((serviceBDispatchCount * 0.1).toInt()) //Service A received at least 10% from serviceBDispatchCount

        }
}