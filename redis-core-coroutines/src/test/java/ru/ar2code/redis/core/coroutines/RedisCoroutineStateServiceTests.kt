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

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.*
import org.junit.Test
import ru.ar2code.redis.core.ServiceStateListener
import ru.ar2code.redis.core.ServiceSubscriber
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.coroutines.prepares.*


class RedisCoroutineStateServiceTests {

    private val testDelayBeforeCheckingResult = 10L

    @Test
    fun `dispatch 4000 intents concurrently works normally without any exceptions`() =
        runBlocking(Dispatchers.Default) {

            println("Start concurrent dispatch test")

            val repeatCount = 1000

            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

            val subscriber = object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {
                    if (newState is FinishState) {
                        service.dispose()
                    }
                }
            }
            service.subscribe(subscriber)

            val d1 = async {
                println("start dispatch task 1")
                repeat(repeatCount) {
                    service.dispatch(IntentTypeConcurrentTest(it))
                }
                println("end dispatch task 1")
            }

            d1.await()

            val d2 = async {
                println("start dispatch task 2")
                repeat(repeatCount) {
                    service.dispatch(IntentTypeConcurrentTest(it))
                }
                println("end dispatch task 2")

            }
            val d3 = async {
                println("start dispatch task 3")
                repeat(repeatCount) {
                    service.dispatch(IntentTypeConcurrentTest(it))
                }
                println("end dispatch task 3")
            }
            val d4 = async {
                println("start dispatch task 4")
                repeat(repeatCount) {
                    service.dispatch(IntentTypeConcurrentTest(it))
                }
                println("end dispatch task 4")
            }

            awaitAll(d2, d3, d4)

            delay(testDelayBeforeCheckingResult)

            service.dispatch(FinishIntent())
        }

    @Test
    fun `service receives all state changing events after dispatching 4000 intents concurrently`() =
        runBlocking(Dispatchers.Default) {
            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

            val repeatCount = 1000
            var total = 0

            val subscriber = object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {
                    total++

                    if (total > 4000) {
                        delay(testDelayBeforeCheckingResult)
                        service.dispatch(FinishIntent())
                    }

                    if (newState is FinishState) {
                        service.dispose()
                    }
                }
            }
            service.subscribe(subscriber)

            val d1 = async {
                println("start dispatch task 1")
                repeat(repeatCount) {
                    service.dispatch(IntentTypeConcurrentTest(it))
                }
                println("end dispatch task 1")
            }

            val d2 = async {
                println("start dispatch task 2")
                repeat(repeatCount) {
                    service.dispatch(IntentTypeConcurrentTest(it))
                }
                println("end dispatch task 2")

            }
            val d3 = async {
                println("start dispatch task 3")
                repeat(repeatCount) {
                    service.dispatch(IntentTypeConcurrentTest(it))
                }
                println("end dispatch task 3")
            }
            val d4 = async {
                println("start dispatch task 4")
                repeat(repeatCount) {
                    service.dispatch(IntentTypeConcurrentTest(it))
                }
                println("end dispatch task 4")
            }

            awaitAll(d1, d2, d3, d4)

            while (!service.isDisposed()) {
                //await disposing
            }

            assertThat(total).isEqualTo(4002) //4000 dispatch + 1 initiated + 1 finish state

        }

    @Test
    fun `concurrent 2000 subscribe unsubscribe operations works normally without any exceptions`() =
        runBlocking(Dispatchers.Default) {

            println("Start concurrent subscribe test")

            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)
            val subscriptions = mutableListOf<ServiceSubscriber>()

            val d1 = async {
                println("start subscribe task 1")
                repeat(500) {
                    val subscriber = object : ServiceSubscriber {
                        override suspend fun onReceive(newState: State) {

                        }
                    }
                    service.subscribe(subscriber)
                    subscriptions.add(subscriber)
                }
                println("end subscribe task 1")

            }

            d1.await()

            val d2 = async {
                println("start subscribe task 2")
                repeat(500) {
                    val subscriber = object : ServiceSubscriber {
                        override suspend fun onReceive(newState: State) {

                        }
                    }
                    service.subscribe(subscriber)
                }
                println("end subscribe task 1")

            }
            val d3 = async {
                println("start unsubscribe task 3, total subscribed = ${subscriptions.size}")
                repeat(subscriptions.size) {
                    subscriptions.toList().forEach {
                        service.unsubscribe(it)
                    }
                }
                println("end unsubscribe task 3")
            }
            val d4 = async {
                println("start subscribe task 4")
                repeat(500) {
                    val subscriber = object : ServiceSubscriber {
                        override suspend fun onReceive(newState: State) {

                        }
                    }
                    service.subscribe(subscriber)
                }
                println("end subscribe task 4")
            }

            awaitAll(d2, d3, d4)

            service.dispose()
        }

    @Test
    fun `isDisposed returns true after cancelling service scope`() = runBlocking {

        val scope = this + Job()

        val service = ServiceFactory.buildSimpleService(scope, Dispatchers.Default)

        scope.cancel()

        assertThat(service.isDisposed())

        Unit
    }

    @Test(expected = IllegalStateException::class)
    fun `service scope was cancelled and try to subscribe throws IllegalStateException`() =
        runBlocking {

            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

            this.cancel()

            val subscriber = object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {

                }
            }
            service.subscribe(subscriber)

            assertThat(service.isDisposed())

            Unit
        }

    @Test(expected = IllegalStateException::class)
    fun `service scope was cancelled and try to dispatch an intent throws IllegalStateException`() =
        runBlocking {

            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

            this.cancel()

            service.dispatch(IntentTypeA())

            assertThat(service.isDisposed())

            Unit
        }

    @Test(expected = TestException::class)
    fun `found reducer that contains error and reducer throws exception then service propagate same exception`() =
        runBlocking {
            val service = ServiceFactory.buildServiceWithReducerException(this, Dispatchers.Default)

            service.dispatch(IntentTypeA())

            delay(testDelayBeforeCheckingResult)

            service.dispose()
        }

    @Test
    fun `service is in initiated state and dispatch IntentTypeA then found SimpleStateReducer`() =
        runBlocking {

            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

            var lastStateFromIntent: State? = null

            val subscriber = object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {
                    if (newState is FinishState) {
                        service.dispose()
                    } else {
                        lastStateFromIntent = newState
                    }
                }
            }
            service.subscribe(subscriber)

            service.dispatch(IntentTypeA())

            delay(testDelayBeforeCheckingResult)

            service.dispatch(FinishIntent())

            while (!service.isDisposed()) {
                //await disposing
            }

            assertThat(lastStateFromIntent).isInstanceOf(StateA::class.java)
        }

    @Test
    fun `service is in initiated state and dispatch FloatIntentType then found FloatStateReducer`() =
        runBlocking {
            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

            var lastStateFromIntent: State? = null

            val subscriber = object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {
                    if (newState is FinishState) {
                        service.dispose()
                    } else {
                        lastStateFromIntent = newState
                    }
                }
            }
            service.subscribe(subscriber)

            service.dispatch(IntentTypeA())

            delay(testDelayBeforeCheckingResult)

            service.dispatch(IntentTypeC())

            delay(testDelayBeforeCheckingResult)

            service.dispatch(FinishIntent())

            while (!service.isDisposed()) {
                //await disposing
            }

            assertThat(lastStateFromIntent).isInstanceOf(StateC::class.java)
        }

    @Test(expected = ReducerNotFoundException::class)
    fun `service is in AnotherState and dispatch FloatIntentType then ThrowReducerNotFoundException`() =
        runBlocking {
            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

            var lastStateFromIntent: State? = null

            val subscriber = object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {
                    lastStateFromIntent = newState
                }
            }
            service.subscribe(subscriber)

            service.dispatch(IntentTypeB())

            delay(testDelayBeforeCheckingResult)

            service.dispatch(IntentTypeC())

            delay(testDelayBeforeCheckingResult)

            service.dispose()
        }

    @Test(expected = ReducerNotFoundException::class)
    fun `service is in Initiated state and dispatch IntentTypeC then ThrowReducerNotFoundException`() =
        runBlocking {

            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

            service.dispatch(IntentTypeC())
        }

    @Test
    fun `reducer contains a flow with 2 states then service changes state 2 times after single intent`() =
        runBlocking {
            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

            val expected = "${FlowStateD.NAME}${FlowStateF.NAME}"
            var result = ""

            val subscriber = object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {
                    if (newState is FlowState) {
                        result += newState.name
                    } else if (newState is FinishState) {
                        service.dispose()
                    }
                }
            }
            service.subscribe(subscriber)

            service.dispatch(IntentTypeFlow())

            delay(testDelayBeforeCheckingResult)

            service.dispatch(FinishIntent())

            while (!service.isDisposed()) {
                //await disposing
            }

            assertThat(result).isEqualTo(expected)

            service.dispose()
        }


    @Test
    fun `service is in initiated state and two subscribers added then subscribers receive initial state`() =
        runBlocking {
            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

            var initialState = false
            var initialStateTwo = false

            service.subscribe(object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {
                    if (newState is State.Initiated) {
                        initialState = true
                    }
                }
            })

            service.subscribe(object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {
                    if (newState is State.Initiated) {
                        initialStateTwo = true
                    }
                }
            })

            delay(testDelayBeforeCheckingResult)

            assertThat(initialState && initialStateTwo)

            service.dispose()
        }

    @Test
    fun `new subscriber receive current service state`() = runBlocking {
        val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

        var emptyResultOne = false
        var payloadResultOne = false

        var emptyResultTwo = false
        var payloadResultTwo = false

        service.subscribe(object : ServiceSubscriber {
            override suspend fun onReceive(newState: State) {
                if (newState is State.Initiated) {
                    emptyResultOne = true
                } else if (newState is StateB) {
                    payloadResultOne = true
                }
            }
        })

        service.dispatch(IntentTypeB())

        delay(testDelayBeforeCheckingResult)

        service.subscribe(object : ServiceSubscriber {
            override suspend fun onReceive(newState: State) {
                if (newState is State.Initiated) {
                    emptyResultTwo = true
                } else if (newState is StateB) {
                    payloadResultTwo = true
                }
            }
        })

        delay(testDelayBeforeCheckingResult)

        val firstSubscriberReceiveEmptyAndPayloadResult = emptyResultOne && payloadResultOne
        val secondSubscriberReceiveOnlyPayloadResult = !emptyResultTwo && payloadResultTwo

        assertThat(firstSubscriberReceiveEmptyAndPayloadResult).isTrue()
        assertThat(secondSubscriberReceiveOnlyPayloadResult).isTrue()

        service.dispose()
    }

    @Test
    fun `service has a subscriber and same subscriber try to subscribe again then does not subscribe again`() =
        runBlocking {

            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

            val subscriber = object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {

                }
            }

            service.subscribe(subscriber)
            service.subscribe(subscriber)
            service.subscribe(subscriber)

            assertThat(service.getSubscribersCount()).isEqualTo(1)

            service.dispose()
        }

    @Test
    fun `service is disposed then all subscribers are removed`() = runBlocking {

        val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

        val subscriber = object : ServiceSubscriber {
            override suspend fun onReceive(newState: State) {

            }
        }

        service.subscribe(subscriber)

        service.dispose()

        assertThat(service.getSubscribersCount()).isEqualTo(0)
    }

    @Test
    fun `service with CustomInit state then all subscribers receive CustomInit state`() =
        runBlocking {
            var emptyResultOne = false


            val service = ServiceFactory.buildServiceWithCustomInit(this, Dispatchers.Default)

            val subscriber = object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {
                    if (newState is CustomInitState) {
                        emptyResultOne = true
                    }
                }
            }

            service.subscribe(subscriber)

            delay(testDelayBeforeCheckingResult)

            assertThat(emptyResultOne)

            service.dispose()
        }

    @Test
    fun `dispatch two intents call appropriate reducers consistently`() = runBlocking {

        //First intent invoke InitiatedStateTypeDelayFlowReducer G->H and after InitiatedStateTypeFlowReducer D->F
        val expectedFlow =
            "${FlowStateG.NAME}${FlowStateH.NAME}${FlowStateD.NAME}${FlowStateF.NAME}"

        var resultData = ""

        val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

        val subscriber = object : ServiceSubscriber {
            override suspend fun onReceive(newState: State) {
                if (newState is FlowState) {
                    resultData += newState.name
                } else if (newState is FinishState) {
                    service.dispose()
                }
            }
        }

        service.subscribe(subscriber)

        service.dispatch(IntentTypeDelayFlow())

        delay(testDelayBeforeCheckingResult)

        service.dispatch(IntentTypeFlow())

        delay(testDelayBeforeCheckingResult)

        service.dispatch(FinishIntent())

        while (!service.isDisposed()) {
            //await
        }

        assertThat(resultData).isEqualTo(expectedFlow)

    }

    @Test
    fun `service A listen another service B then service A receive specified intent when service B state changed`() =
        runBlocking {
            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)
            val listenedService = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

            var stateBCount = 0

            val subscriber = object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {
                    if (newState is StateB) {
                        stateBCount++
                    } else if (newState is FinishState) {
                        service.dispose()
                        listenedService.dispose()
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

            while (!service.isDisposed()) {
                //await
            }

            assertThat(stateBCount).isEqualTo(2) //First time when listenedService initiated and second time when dispatch intent

        }

    @Test
    fun `service A stopped listen service B then service A will not receive specified intent when service B state changed`() =
        runBlocking {
            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)
            val listenedService = ServiceFactory.buildSimpleService(this, Dispatchers.Default)
            val listenedServiceInfo =
                ServiceStateListener(listenedService, mapOf(null to IntentTypeBBuilder()))

            var stateBCount = 0

            val subscriber = object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {
                    if (newState is StateB) {
                        stateBCount++
                    } else if (newState is FinishState) {
                        service.dispose()
                        listenedService.dispose()
                    }
                }
            }

            service.subscribe(subscriber)

            service.listen(listenedServiceInfo)

            delay(testDelayBeforeCheckingResult)

            service.stopListening(listenedServiceInfo)

            listenedService.dispatch(IntentTypeA())

            service.dispatch(FinishIntent())

            while (!service.isDisposed()) {
                //await
            }

            assertThat(stateBCount).isEqualTo(1) //First time when listenedService initiated. And ignore second time when dispatch intent.
        }

    @Test
    fun `service A does not listen any services then stop listening unknown service does nothing`() =
        runBlocking {

            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)
            val listenedService = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

            val listenedServiceInfo =
                ServiceStateListener(listenedService, mapOf(null to IntentTypeBBuilder()))

            var stateBCount = 0

            val subscriber = object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {
                    if (newState is StateB) {
                        stateBCount++
                    } else if (newState is FinishState) {
                        service.dispose()
                        listenedService.dispose()
                    }
                }
            }

            service.subscribe(subscriber)
            service.stopListening(listenedServiceInfo)

            listenedService.dispatch(IntentTypeA())

            delay(testDelayBeforeCheckingResult)

            service.dispatch(FinishIntent())

            while (!service.isDisposed()) {
                //await
            }

            assertThat(stateBCount).isEqualTo(0)

        }

    @Test
    fun `test service trigger`() = runBlocking {
        var result = ""
        val service = ServiceFactory.buildSimpleServiceWithTriggers(this, Dispatchers.Default)
        val subscriber = object : ServiceSubscriber {
            override suspend fun onReceive(newState: State) {
                when (newState) {
                    is State.Initiated -> {
                        result += "I"
                    }
                    is StateA -> {
                        result += "A"
                    }
                    is StateC -> {
                        result += "C"
                    }
                    is FinishState -> {
                        service.dispose()
                    }
                }
            }
        }

        service.subscribe(subscriber)

        service.dispatch(IntentTypeA())

        delay(testDelayBeforeCheckingResult)

        service.dispatch(FinishIntent())

        while (!service.isDisposed()) {
            //await
        }

        assertThat(result).isEqualTo("IAC") //Initiated -> State A (Intent) -> trigger -> State C

    }
}