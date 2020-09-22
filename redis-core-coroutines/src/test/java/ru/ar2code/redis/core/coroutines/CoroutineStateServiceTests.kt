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
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.IntentMessage
import ru.ar2code.redis.core.ListenedService
import ru.ar2code.redis.core.ServiceSubscriber
import ru.ar2code.redis.core.coroutines.prepares.*

@ExperimentalCoroutinesApi
class CoroutineStateServiceTests {

    private val testDelayBeforeCheckingResult = 50L
    private val testDelayBeforeDispatchSecondIntent = 10L

    @Test
    fun service_ConcurrentSubscribeUnsubscribe_NoAnyConcurrentExceptions() =
        runBlocking(Dispatchers.Default) {

            println("Start ActionService concurrent test")

            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)
            val subscriptions = mutableListOf<ServiceSubscriber>()

            val d1 = async {
                println("start subscribe task 1")
                repeat(1000) {
                    val subscriber = object : ServiceSubscriber {
                        override fun onReceive(newState: State) {

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
                repeat(1000) {
                    val subscriber = object : ServiceSubscriber {
                        override fun onReceive(newState: State) {

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
                repeat(1000) {
                    val subscriber = object : ServiceSubscriber {
                        override fun onReceive(newState: State) {

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
    fun service_ScopeCancelled_ServiceDisposed() = runBlocking {

        val scope = this + Job()

        val service = ServiceFactory.buildSimpleService(scope, Dispatchers.Default)

        scope.cancel()

        assertThat(service.isDisposed())

        Unit
    }

    @Test(expected = IllegalStateException::class)
    fun serviceScopeCancelled_SubscribeToService_throwException() = runBlocking {

        val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

        this.cancel()

        val subscriber = object : ServiceSubscriber {
            override fun onReceive(newState: State) {

            }
        }
        service.subscribe(subscriber)

        assertThat(service.isDisposed())

        Unit
    }

    @Test(expected = IllegalStateException::class)
    fun serviceScopeCancelled_dispatch_throwException() = runBlocking {

        val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

        this.cancel()

        service.dispatch(IntentTypeA())

        assertThat(service.isDisposed())

        Unit
    }

    @Test(expected = TestException::class)
    fun service_ExceptionInsideReducer_throwException() = runBlocking {
        val service = ServiceFactory.buildServiceWithReducerException(this, Dispatchers.Default)

        service.dispatch(IntentTypeA())

        delay(testDelayBeforeCheckingResult)

        service.dispose()
    }

    @Test
    fun serviceInitializedState_DispatchSimpleIntentType_SelectSimpleStateReducer() =
        runBlocking {
            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

            var lastStateFromIntent: State? = null

            val subscriber = object : ServiceSubscriber {
                override fun onReceive(newState: State) {
                    lastStateFromIntent = newState
                }
            }
            service.subscribe(subscriber)

            service.dispatch(IntentTypeA())

            delay(testDelayBeforeCheckingResult)

            service.dispose()

            assertThat(lastStateFromIntent).isInstanceOf(StateA::class.java)

            Unit
        }

    @Test
    fun serviceInitializedState_DispatchFloatIntentType_SelectFloatStateReducer() =
        runBlocking {
            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

            var lastStateFromIntent: State? = null

            val subscriber = object : ServiceSubscriber {
                override fun onReceive(newState: State) {
                    lastStateFromIntent = newState
                }
            }
            service.subscribe(subscriber)

            service.dispatch(IntentTypeA())

            delay(testDelayBeforeCheckingResult)

            service.dispatch(IntentTypeC())

            delay(testDelayBeforeCheckingResult)

            service.dispose()

            assertThat(lastStateFromIntent).isInstanceOf(StateC::class.java)

            Unit
        }

    @Test(expected = IllegalArgumentException::class)
    fun serviceAnotherState_DispatchFloatIntentType_ThrowReducerNotFoundException() =
        runBlocking {
            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

            var lastStateFromIntent: State? = null

            val subscriber = object : ServiceSubscriber {
                override fun onReceive(newState: State) {
                    lastStateFromIntent = newState
                }
            }
            service.subscribe(subscriber)

            service.dispatch(IntentTypeB())

            delay(testDelayBeforeCheckingResult)

            service.dispatch(IntentTypeC())

            delay(testDelayBeforeCheckingResult)

            service.dispose()

            assertThat(lastStateFromIntent).isInstanceOf(StateC::class.java)

            Unit
        }

    @Test(expected = IllegalArgumentException::class)
    fun service_DispatchNoReducerIntent_ThrowReducerNotFoundException() =
        runBlocking {

            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

            service.dispatch(IntentTypeC())
        }

    @Test
    fun serviceInitializedState_DispatchAnotherIntentType_AnotherStateReducer() =
        runBlocking {
            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

            var lastStateFromIntent: State? = null

            val subscriber = object : ServiceSubscriber {
                override fun onReceive(newState: State) {
                    lastStateFromIntent = newState
                }
            }
            service.subscribe(subscriber)

            service.dispatch(IntentTypeB())

            delay(testDelayBeforeCheckingResult)

            service.dispose()

            assertThat(lastStateFromIntent).isInstanceOf(StateB::class.java)

            Unit
        }

    @Test
    fun service_DispatchIntentForReducerWithFlow_ChangeStateSeveralTimes() =
        runBlocking {
            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

            val expected = "${FlowStateD.NAME}${FlowStateF.NAME}"
            var result = ""

            val subscriber = object : ServiceSubscriber {
                override fun onReceive(newState: State) {
                    if (newState is FlowState) {
                        result += newState.name
                    }
                }
            }
            service.subscribe(subscriber)

            service.dispatch(IntentTypeFlow())

            delay(testDelayBeforeCheckingResult)

            assertThat(result).isEqualTo(expected)

            service.dispose()
        }


    @Test
    fun service_NewSubscribersAdded_ReceiveInitStateToAll() = runBlocking {
        val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

        var emptyResult = false
        var emptyResultTwo = false

        service.subscribe(object : ServiceSubscriber {
            override fun onReceive(newState: State) {
                if (newState is State.Initiated) {
                    emptyResult = true
                }
            }
        })

        service.subscribe(object : ServiceSubscriber {
            override fun onReceive(newState: State) {
                if (newState is State.Initiated) {
                    emptyResultTwo = true
                }
            }
        })

        delay(testDelayBeforeCheckingResult)

        assertThat(emptyResult && emptyResultTwo)

        service.dispose()
    }

    @Test
    fun service_NewSubscriberAdded_ReceiveLastState() = runBlocking {
        val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

        var emptyResultOne = false
        var payloadResultOne = false

        var emptyResultTwo = false
        var payloadResultTwo = false

        service.subscribe(object : ServiceSubscriber {
            override fun onReceive(newState: State) {
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
            override fun onReceive(newState: State) {
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

        assertThat(firstSubscriberReceiveEmptyAndPayloadResult)
        assertThat(secondSubscriberReceiveOnlyPayloadResult)

        service.dispose()
    }

    @Test
    fun service_AddSameSubscriberTwoTimes_SubscriberDidNotAddedIfExists() =
        runBlocking {

            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

            val subscriber = object : ServiceSubscriber {
                override fun onReceive(newState: State) {

                }
            }

            service.subscribe(subscriber)
            service.subscribe(subscriber)
            service.subscribe(subscriber)

            assertThat(service.getSubscribersCount()).isEqualTo(1)

            service.dispose()
        }

    @Test
    fun service_Dispose_NoActiveSubscribers() = runBlocking {

        val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

        val subscriber = object : ServiceSubscriber {
            override fun onReceive(newState: State) {

            }
        }

        service.subscribe(subscriber)

        service.dispose()

        assertThat(service.getSubscribersCount()).isEqualTo(0)
    }

    @Test
    fun service_SpecifyInitialState_SubscriberReceiveCustomInitState() = runBlocking {
        var emptyResultOne = false

        val service = ServiceFactory.buildServiceWithCustomInit(this, Dispatchers.Default)

        val subscriber = object : ServiceSubscriber {
            override fun onReceive(newState: State) {
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
    fun service_DispatchTwoIntents_CallReducersConsistently() = runBlocking {

        //First intent invoke InitiatedStateTypeDelayFlowReducer G->H and after InitiatedStateTypeFlowReducer D->F
        val expectedFlow =
            "${FlowStateG.NAME}${FlowStateH.NAME}${FlowStateD.NAME}${FlowStateF.NAME}"

        var resultData = ""

        val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

        val subscriber = object : ServiceSubscriber {
            override fun onReceive(newState: State) {
                if (newState is FlowState) {
                    resultData += newState.name
                }
            }
        }

        service.subscribe(subscriber)

        service.dispatch(IntentTypeDelayFlow())

        delay(testDelayBeforeDispatchSecondIntent)

        service.dispatch(IntentTypeFlow())

        delay(testDelayBeforeCheckingResult)

        assertThat(resultData).isEqualTo(expectedFlow)

        service.dispose()
    }

    @Test
    fun service_AddListenedService_GotSpecifiedIntentWhenListenedServiceStateChanged() = runBlocking {
        val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)
        val listenedService = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

        var stateBCount = 0

        val subscriber = object : ServiceSubscriber {
            override fun onReceive(newState: State) {
                if (newState is StateB) {
                    stateBCount++
                }
            }
        }

        service.subscribe(subscriber)

        service.listen(ListenedService(listenedService) { _ -> IntentTypeB() })

        listenedService.dispatch(IntentTypeA())

        delay(testDelayBeforeCheckingResult)

        assertThat(stateBCount).isEqualTo(2) //First time when listenedService initiated and second time when dispatch intent

        service.dispose()
        listenedService.dispose()
    }

    @Test
    fun service_StopListeningService_DoNotDispatchIntentWhenListenedServiceStateChanged() =
        runBlocking {
            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)
            val listenedService = ServiceFactory.buildSimpleService(this, Dispatchers.Default)
            val listenedServiceInfo = ListenedService(listenedService) { _ -> IntentTypeB() }

            var stateBCount = 0

            val subscriber = object : ServiceSubscriber {
                override fun onReceive(newState: State) {
                    if (newState is StateB) {
                        stateBCount++
                    }
                }
            }

            service.subscribe(subscriber)

            service.listen(listenedServiceInfo)

            service.stopListening(listenedServiceInfo)

            listenedService.dispatch(IntentTypeA())

            delay(testDelayBeforeCheckingResult)

            assertThat(stateBCount).isEqualTo(1) //First time when listenedService initiated. And ignore second time when dispatch intent.

            service.dispose()
            listenedService.dispose()
        }

    @Test
    fun service_RemoveUnknownServiceForListening_DoNothing() = runBlocking {
        val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)
        val listenedService = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

        val listenedServiceInfo = ListenedService(listenedService) { _ -> IntentTypeB() }

        var stateBCount = 0

        val subscriber = object : ServiceSubscriber {
            override fun onReceive(newState: State) {
                if (newState is StateB) {
                    stateBCount++
                }
            }
        }

        service.subscribe(subscriber)
        service.stopListening(listenedServiceInfo)

        listenedService.dispatch(IntentTypeA())

        delay(testDelayBeforeCheckingResult)

        assertThat(stateBCount).isEqualTo(0)

        service.dispose()

        listenedService.dispose()
    }
}