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
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.*
import org.junit.Test
import ru.ar2code.redis.core.ServiceSubscriber
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.coroutines.prepares.*
import ru.ar2code.redis.core.coroutines.prepares.Constants.awaitDisposedStateTimeout
import ru.ar2code.redis.core.coroutines.prepares.Constants.awaitStateTimeout
import ru.ar2code.redis.core.coroutines.prepares.Constants.testDelayBeforeCheckingResult
import ru.ar2code.redis.core.coroutines.test.awaitWhileNotDisposedWithTimeout
import ru.ar2code.redis.core.coroutines.test.disposeServiceAfterNumbersOfDispatchedIntents
import ru.ar2code.redis.core.coroutines.test.disposeServiceWhenIntentDispatched


class RedisCoroutineStateServiceTests {

    @Test
    fun `dispatch 4000 intents concurrently works normally without any exceptions`() =
        runBlocking(Dispatchers.Default) {

            println("Start concurrent dispatch test")

            val repeatCount = 1000

            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

            service.disposeServiceWhenIntentDispatched(
                FinishIntent::class,
                testDelayBeforeCheckingResult
            )

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

            service.dispatch(FinishIntent())
        }

    @Test
    fun `service receives all state changing events after dispatching 4000 intents concurrently`() =
        runBlocking(Dispatchers.Default) {
            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

            val totalIntents = 4000
            val repeatCount = 1000
            var total = 0

            val subscriber = object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {
                    total++
                }
            }
            service.subscribe(subscriber)

            service.disposeServiceAfterNumbersOfDispatchedIntents(totalIntents)

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

            service.awaitWhileNotDisposedWithTimeout(awaitDisposedStateTimeout)

            assertThat(total).isAtLeast(totalIntents) //4000 dispatch + 1 initiated + 1 finish state + disposed

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
    fun `found some reducer that contains an error and reducer throws an exception then service propagate the same exception if emitErrorAsState is false`() =
        runBlocking {
            val service = ServiceFactory.buildServiceWithReducerException(
                this,
                Dispatchers.Default,
                emitErrorAsState = false
            )

            service.dispatch(IntentTypeA())

            delay(testDelayBeforeCheckingResult)

            service.dispose()
        }

    @Test
    fun `found some reducer that contains an error and reducer throws an exception then service emit error state if emitErrorAsState is true`() =
        runBlocking {
            val service = ServiceFactory.buildServiceWithReducerException(
                this,
                Dispatchers.Default,
                emitErrorAsState = true
            )

            var errorState: State.ErrorOccurred? = null

            service.subscribe(object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {
                    if (errorState == null && newState is State.ErrorOccurred) {
                        errorState = newState
                    }
                }
            })

            service.dispatch(IntentTypeA())

            service.awaitStateWithTimeout(awaitStateTimeout, State.ErrorOccurred::class)

            service.dispose()

            assertThat(errorState).isNotNull()
            assertThat(errorState?.throwable).isInstanceOf(TestException::class.java)
        }

    @Test(expected = TestException::class)
    fun `exception occurred inside trigger action block then service propagate the same exception if emitErrorAsState is false`() =
        runBlocking {
            val service = ServiceFactory.buildSimpleServiceWithActionErrorTrigger(
                this,
                Dispatchers.Default,
                emitErrorAsState = false
            )

            service.dispatch(IntentTypeA())

            delay(testDelayBeforeCheckingResult)

            service.dispose()
        }

    @Test
    fun `exception occurred inside trigger action block then service emit error state if emitErrorAsState is true`() =
        runBlocking {
            val service = ServiceFactory.buildSimpleServiceWithActionErrorTrigger(
                this,
                Dispatchers.Default,
                emitErrorAsState = true
            )

            var errorState: State.ErrorOccurred? = null

            service.subscribe(object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {
                    if (errorState == null && newState is State.ErrorOccurred) {
                        errorState = newState
                    }
                }
            })

            service.dispatch(IntentTypeA())

            service.awaitStateWithTimeout(awaitStateTimeout, State.ErrorOccurred::class)

            service.dispose()

            assertThat(errorState).isNotNull()
            assertThat(errorState?.throwable).isInstanceOf(TestException::class.java)
        }

    @Test(expected = TestException::class)
    fun `exception occurred inside trigger intent block then service propagate the same exception if emitErrorAsState is false`() =
        runBlocking {
            val service = ServiceFactory.buildSimpleServiceWithIntentErrorTrigger(
                this,
                Dispatchers.Default,
                emitErrorAsState = false
            )

            service.dispatch(IntentTypeA())

            delay(testDelayBeforeCheckingResult)

            service.dispose()
        }

    @Test
    fun `exception occurred inside trigger intent block then service emit error state if emitErrorAsState is true`() =
        runBlocking {
            val service = ServiceFactory.buildSimpleServiceWithIntentErrorTrigger(
                this,
                Dispatchers.Default,
                emitErrorAsState = true
            )

            var errorState: State.ErrorOccurred? = null

            service.subscribe(object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {
                    if (errorState == null && newState is State.ErrorOccurred) {
                        errorState = newState
                    }
                }
            })

            service.dispatch(IntentTypeA())

            service.awaitStateWithTimeout(awaitStateTimeout, State.ErrorOccurred::class)

            service.dispose()

            assertThat(errorState).isNotNull()
            assertThat(errorState?.throwable).isInstanceOf(TestException::class.java)
        }

    @Test(expected = TestException::class)
    fun `exception occurred inside on created block then service propagate the same exception if emitErrorAsState is false`() =
        runBlocking {
            val service = ServiceFactory.buildServiceWithErrorInsideCreateBlock(
                this,
                Dispatchers.Default,
                emitErrorAsState = false
            )

            delay(testDelayBeforeCheckingResult)

            service.dispose()
        }

    @Test
    fun `exception occurred inside on created block then service emit error state if emitErrorAsState is true`() =
        runBlocking {
            val service = ServiceFactory.buildServiceWithErrorInsideCreateBlock(
                this,
                Dispatchers.Default,
                emitErrorAsState = true
            )

            var errorState: State.ErrorOccurred? = null

            service.subscribe(object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {
                    if (errorState == null && newState is State.ErrorOccurred) {
                        errorState = newState
                    }
                }
            })

            delay(testDelayBeforeCheckingResult)

            service.dispose()

            assertThat(errorState).isNotNull()
            assertThat(errorState?.throwable).isInstanceOf(TestException::class.java)
        }

    @Test
    fun `service is in initiated state and dispatch IntentTypeA then found SimpleStateReducer`() =
        runBlocking {

            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

            service.disposeServiceWhenIntentDispatched(
                FinishIntent::class,
                testDelayBeforeCheckingResult
            )

            var lastStateFromIntent: State? = null

            val subscriber = object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {
                    if (newState !is State.Disposed) {
                        lastStateFromIntent = newState
                    }
                }
            }
            service.subscribe(subscriber)

            service.dispatch(IntentTypeA())

            service.dispatch(FinishIntent())

            service.awaitWhileNotDisposedWithTimeout(awaitDisposedStateTimeout)

            assertThat(lastStateFromIntent).isInstanceOf(StateA::class.java)
        }

    @Test
    fun `service is in initiated state and dispatch FloatIntentType then found FloatStateReducer`() =
        runBlocking {
            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)
            service.disposeServiceWhenIntentDispatched(
                FinishIntent::class,
                testDelayBeforeCheckingResult
            )

            var lastStateFromIntent: State? = null

            val subscriber = object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {
                    if (newState !is State.Disposed) {
                        lastStateFromIntent = newState
                    }
                }
            }
            service.subscribe(subscriber)

            delay(testDelayBeforeCheckingResult)

            service.dispatch(IntentTypeA())

            delay(testDelayBeforeCheckingResult)

            service.dispatch(IntentTypeC())

            delay(testDelayBeforeCheckingResult)

            service.dispatch(FinishIntent())

            service.awaitWhileNotDisposedWithTimeout(awaitDisposedStateTimeout)

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

            delay(testDelayBeforeCheckingResult)

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

            service.disposeServiceWhenIntentDispatched(
                FinishIntent::class,
                testDelayBeforeCheckingResult
            )

            val expected = "${FlowStateD.NAME}${FlowStateF.NAME}"
            var result = ""

            val subscriber = object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {
                    if (newState is FlowState) {
                        result += newState.name
                    }
                }
            }
            service.subscribe(subscriber)

            delay(testDelayBeforeCheckingResult)

            service.dispatch(IntentTypeFlow())

            delay(testDelayBeforeCheckingResult)

            service.dispatch(FinishIntent())

            service.awaitWhileNotDisposedWithTimeout(awaitDisposedStateTimeout)

            assertThat(result).isEqualTo(expected)
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

        delay(testDelayBeforeCheckingResult)

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

        assertThat(service.getSubscribersCount()).isEqualTo(1)

        service.dispose()

        assertThat(service.getSubscribersCount()).isEqualTo(0)
    }

    @Test
    fun `service with CustomInit state then all subscribers receive CustomInit state`() =
        runBlocking {
            var emptyResultOne = false
            var createdResult = false

            val service = ServiceFactory.buildServiceWithCustomInit(this, Dispatchers.Default)

            val subscriber = object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {
                    if (newState is State.Created) {
                        createdResult = true
                    }
                    if (newState is CustomInitState) {
                        emptyResultOne = true
                    }
                }
            }

            service.subscribe(subscriber)

            delay(testDelayBeforeCheckingResult)

            assertThat(emptyResultOne).isTrue()
            assertThat(createdResult).isFalse()

            service.dispose()
        }

    @Test
    fun `dispatch two intents call appropriate reducers consistently`() = runBlocking {

        //First intent invoke InitiatedStateTypeDelayFlowReducer G->H and after InitiatedStateTypeFlowReducer D->F
        val expectedFlow =
            "${FlowStateG.NAME}${FlowStateH.NAME}${FlowStateD.NAME}${FlowStateF.NAME}"

        var resultData = ""

        val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)
        service.disposeServiceWhenIntentDispatched(
            FinishIntent::class,
            testDelayBeforeCheckingResult
        )

        val subscriber = object : ServiceSubscriber {
            override suspend fun onReceive(newState: State) {
                if (newState is FlowState) {
                    resultData += newState.name
                }
            }
        }

        service.subscribe(subscriber)

        delay(testDelayBeforeCheckingResult)

        service.dispatch(IntentTypeDelayFlow())

        delay(testDelayBeforeCheckingResult)

        service.dispatch(IntentTypeFlow())

        delay(testDelayBeforeCheckingResult)

        service.dispatch(FinishIntent())

        service.awaitWhileNotDisposedWithTimeout(awaitDisposedStateTimeout)

        assertThat(resultData).isEqualTo(expectedFlow)
    }

    @Test
    fun `test service trigger`() = runBlocking {
        val expectResult = "IAC"  //Initiated -> State A (Intent) -> trigger -> State C
        var result = ""
        val service = ServiceFactory.buildSimpleServiceWithTriggers(this, Dispatchers.Default)
        service.disposeServiceWhenIntentDispatched(
            FinishIntent::class,
            testDelayBeforeCheckingResult
        )

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

        delay(testDelayBeforeCheckingResult)

        service.dispatch(IntentTypeA())

        delay(testDelayBeforeCheckingResult)

        service.dispatch(FinishIntent())

        service.awaitWhileNotDisposedWithTimeout(awaitDisposedStateTimeout)

        assertThat(result).isEqualTo(expectResult)
    }

    @Test
    fun `service has quick and slow subscribers and dispatch intents then quick receives all states and do not wait slow`() =
        runBlocking {
            val service = ServiceFactory.buildSimpleServiceWithTriggers(this, Dispatchers.Default)

            var receiveSlow = false
            var receiveSlowAfterDelay = false
            var receiveQuick = false

            val subscriberLong = object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {
                    receiveSlow = true

                    delay(1000)

                    receiveSlowAfterDelay = true
                }
            }
            val subscriberQuick = object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {
                    if (newState is FinishState) {
                        receiveQuick = true
                    }
                }
            }

            service.subscribe(subscriberLong)
            service.subscribe(subscriberQuick)

            service.dispatch(FinishIntent())

            delay(testDelayBeforeCheckingResult)

            service.dispose()

            assertThat(receiveSlow).isTrue()
            assertThat(receiveQuick).isTrue()
            assertThat(receiveSlowAfterDelay).isFalse()
        }

    @Test
    fun `service has quick and slow subscribers and dispatch intents then all subscribers receive all states`() =
        runBlocking {
            val expectResult = "IAC"  //Initiated -> State A (Intent) -> trigger -> State C
            var resultQuick = ""
            var resultSlow = ""
            var slowResultOnMomentWhenQuickFinished = ""

            val service = ServiceFactory.buildSimpleServiceWithTriggers(this, Dispatchers.Default)
            service.disposeServiceWhenIntentDispatched(
                FinishIntent::class,
                testDelayBeforeCheckingResult
            )

            val subscriberQuick = object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {
                    when (newState) {
                        is State.Initiated -> {
                            resultQuick += "I"
                        }
                        is StateA -> {
                            resultQuick += "A"
                        }
                        is StateC -> {
                            resultQuick += "C"
                        }
                    }

                    if (resultQuick == expectResult && slowResultOnMomentWhenQuickFinished.isNullOrEmpty()) {
                        slowResultOnMomentWhenQuickFinished = resultSlow
                    }
                }
            }

            val subscriberSlow = object : ServiceSubscriber {
                override suspend fun onReceive(newState: State) {
                    when (newState) {
                        is State.Initiated -> {
                            resultSlow += "I"
                        }
                        is StateA -> {
                            resultSlow += "A"
                        }
                        is StateC -> {
                            resultSlow += "C"
                        }
                    }
                    delay(testDelayBeforeCheckingResult)
                }
            }

            service.subscribe(subscriberQuick)

            service.subscribe(subscriberSlow)

            delay(testDelayBeforeCheckingResult)

            service.dispatch(IntentTypeA())

            delay(testDelayBeforeCheckingResult)

            service.dispatch(FinishIntent())

            service.awaitWhileNotDisposedWithTimeout(awaitDisposedStateTimeout)

            assertThat(resultSlow).isEqualTo(expectResult)
            assertThat(resultQuick).isEqualTo(expectResult)

            assertThat(slowResultOnMomentWhenQuickFinished).isNotEqualTo(expectResult)
            assertThat(slowResultOnMomentWhenQuickFinished).isNotEmpty()
            assertThat(slowResultOnMomentWhenQuickFinished.length).isLessThan(expectResult.length)
        }


}