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

package ru.ar2code.android.redis.core

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.*
import org.junit.Test
import ru.ar2code.android.redis.core.models.IntentMessage
import ru.ar2code.android.redis.core.prepares.*
import ru.ar2code.android.redis.core.services.ActorServiceState
import ru.ar2code.android.redis.core.services.ServiceSubscriber

@ExperimentalCoroutinesApi
class CoroutineActorServiceTests {

    private val testDelayBeforeCheckingResult = 20L

    @Test
    fun `Concurrent subscribe and unsubscribe (2000 subs) gives no any concurrent exceptions`() =
        runBlocking(Dispatchers.Default) {

            println("Start ActionService concurrent test")

            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)
            val subscriptions = mutableListOf<ServiceSubscriber>()

            val d1 = async {
                println("start subscribe task 1")
                repeat(1000) {
                    val subscriber = object : ServiceSubscriber {
                        override fun onReceive(newState: ActorServiceState) {

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
                        override fun onReceive(newState: ActorServiceState) {

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
                        override fun onReceive(newState: ActorServiceState) {

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
    fun `When scope is cancelled services is disposed`() = runBlocking {

        val scope = this + Job()

        val service = ServiceFactory.buildSimpleService(scope, Dispatchers.Default)

        scope.cancel()

        assertThat(service.isDisposed())

        Unit
    }

    @Test(expected = IllegalStateException::class)
    fun `When scope throw exception on attempt to subscribe again`() = runBlocking {

        val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

        this.cancel()

        val subscriber = object : ServiceSubscriber {
            override fun onReceive(newState: ActorServiceState) {

            }
        }
        service.subscribe(subscriber)

        assertThat(service.isDisposed())

        Unit
    }

    @Test(expected = IllegalStateException::class)
    fun `When scope throw exception on attempt to send intent`() = runBlocking {

        val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

        this.cancel()

        service.dispatch(IntentMessage(SimpleIntentType()))

        assertThat(service.isDisposed())

        Unit
    }

    @Test(expected = TestException::class)
    fun `Propagated exception that occurred inside reducer fun block`() = runBlocking {
        val service = ServiceFactory.buildServiceWithReducerException(this, Dispatchers.Default)

        service.dispatch(IntentMessage(SimpleIntentType()))

        delay(testDelayBeforeCheckingResult)

        service.dispose()
    }

    @Test
    fun `SimpleIntentType and Initialized state select SimpleStateReducer`() = runBlocking {
        val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

        var lastStateFromIntent: ActorServiceState? = null

        val subscriber = object : ServiceSubscriber {
            override fun onReceive(newState: ActorServiceState) {
                lastStateFromIntent = newState
            }
        }
        service.subscribe(subscriber)

        service.dispatch(IntentMessage(SimpleIntentType()))

        delay(testDelayBeforeCheckingResult)

        service.dispose()

        assertThat(lastStateFromIntent).isInstanceOf(SimpleState::class.java)

        Unit
    }

    @Test
    fun `FloatIntentType and SimpleState state select FloatStateReducer`() = runBlocking {
        val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

        var lastStateFromIntent: ActorServiceState? = null

        val subscriber = object : ServiceSubscriber {
            override fun onReceive(newState: ActorServiceState) {
                lastStateFromIntent = newState
            }
        }
        service.subscribe(subscriber)

        service.dispatch(IntentMessage(SimpleIntentType()))

        delay(testDelayBeforeCheckingResult)

        service.dispatch(IntentMessage(FloatIntentType()))

        delay(testDelayBeforeCheckingResult)

        service.dispose()

        assertThat(lastStateFromIntent).isInstanceOf(FloatState::class.java)

        Unit
    }

    @Test(expected = IllegalArgumentException::class)
    fun `FloatIntentType and AnotherState state throws reducer not found exception cause FloatStateReducer expect SimpleState`() =
        runBlocking {
            val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

            var lastStateFromIntent: ActorServiceState? = null

            val subscriber = object : ServiceSubscriber {
                override fun onReceive(newState: ActorServiceState) {
                    lastStateFromIntent = newState
                }
            }
            service.subscribe(subscriber)

            service.dispatch(IntentMessage(AnotherIntentType()))

            delay(testDelayBeforeCheckingResult)

            service.dispatch(IntentMessage(FloatIntentType()))

            delay(testDelayBeforeCheckingResult)

            service.dispose()

            assertThat(lastStateFromIntent).isInstanceOf(FloatState::class.java)

            Unit
        }

    @Test(expected = IllegalArgumentException::class)
    fun `Reducer not found throws exception`() = runBlocking {

        val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

        service.dispatch(IntentMessage(FloatIntentType()))
    }

    @Test
    fun `AnotherIntentType and Initialized state select AnotherStateReducer`() = runBlocking {
        val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

        var lastStateFromIntent: ActorServiceState? = null

        val subscriber = object : ServiceSubscriber {
            override fun onReceive(newState: ActorServiceState) {
                lastStateFromIntent = newState
            }
        }
        service.subscribe(subscriber)

        service.dispatch(IntentMessage(AnotherIntentType()))

        delay(testDelayBeforeCheckingResult)

        service.dispose()

        assertThat(lastStateFromIntent).isInstanceOf(AnotherState::class.java)

        Unit
    }

//    @Test
//    fun `Receive result from subscription after sending intent`() = runBlocking {
//        val service = SimpleServiceCoroutine(this, Dispatchers.Default)
//
//        val payload = "1"
//        var emptyResult = false
//        var gotResult = false
//
//        val subscriber = object : ServiceSubscriber {
//            override fun onReceive(newState: ActorServiceState) {
//                if (stateWithResult?.result is ServiceResult.InitResult) {
//                    emptyResult = true
//                    return
//                }
//
//                gotResult = stateWithResult?.result?.payload == payload
//
//                service.dispose()
//            }
//        }
//        service.subscribe(subscriber)
//
//        service.sendIntent(IntentMessage(SimpleServiceCoroutine.SimpleIntentType(payload)))
//
//        delay(testDelayBeforeCheckingResult)
//
//        Assert.assertTrue(gotResult)
//        Assert.assertTrue(emptyResult)
//    }
//
//    @Test
//    fun `Receive init result from subscription after service initiated`() = runBlocking {
//        val service = SimpleServiceCoroutine(this, Dispatchers.Default)
//
//        var emptyResult = false
//
//        val subscriber = object : ServiceSubscriber {
//            override fun onReceive(newState: ActorServiceState) {
//                if (stateWithResult?.result is ServiceResult.InitResult) {
//                    emptyResult = true
//                    service.dispose()
//                }
//            }
//        }
//        service.subscribe(subscriber)
//
//        delay(testDelayBeforeCheckingResult)
//
//        Assert.assertTrue(emptyResult)
//    }
//
//    @Test
//    fun `Receive init result from all subscriptions after service initiated`() = runBlocking {
//        val service = SimpleServiceCoroutine(this, Dispatchers.Default)
//
//        var emptyResult = false
//        var emptyResultTwo = false
//
//        service.subscribe(object : ServiceSubscriber {
//            override fun onReceive(newState: ActorServiceState) {
//                if (stateWithResult?.result is ServiceResult.InitResult) {
//                    emptyResult = true
//                }
//            }
//        })
//
//        service.subscribe(object : ServiceSubscriber {
//            override fun onReceive(newState: ActorServiceState) {
//                if (stateWithResult?.result is ServiceResult.InitResult) {
//                    emptyResultTwo = true
//                }
//            }
//        })
//
//        delay(testDelayBeforeCheckingResult)
//
//        service.dispose()
//
//        Assert.assertTrue(emptyResult)
//        Assert.assertTrue(emptyResultTwo)
//    }
//
//    @Test
//    fun `Receive last result from newly added subscription`() = runBlocking {
//        val service = SimpleServiceCoroutine(this, Dispatchers.Default)
//        val payload = "1"
//
//        var emptyResultOne = false
//        var payloadResultOne = false
//
//        var emptyResultTwo = false
//        var payloadResultTwo = false
//
//        service.subscribe(object : ServiceSubscriber {
//            override fun onReceive(newState: ActorServiceState) {
//                if (stateWithResult?.result is ServiceResult.InitResult) {
//                    emptyResultOne = true
//                } else {
//                    payloadResultOne = stateWithResult?.result?.payload == payload
//                }
//            }
//        })
//
//        service.dispatch(IntentMessage(SimpleServiceCoroutine.SimpleIntentType(payload)))
//
//        delay(testDelayBeforeCheckingResult)
//
//        service.subscribe(object : ServiceSubscriber {
//            override fun onReceive(newState: ActorServiceState) {
//                if (stateWithResult?.result is ServiceResult.InitResult) {
//                    emptyResultTwo = true
//                } else {
//                    payloadResultTwo = stateWithResult?.result?.payload == payload
//                }
//            }
//        })
//
//        delay(testDelayBeforeCheckingResult)
//
//        service.dispose()
//
//        val firstSubscriberReceiveEmptyAndPayloadResult = emptyResultOne && payloadResultOne
//        val secondSubscriberReceiveOnlyPayloadResult = !emptyResultTwo && payloadResultTwo
//
//        Assert.assertTrue(firstSubscriberReceiveEmptyAndPayloadResult)
//        Assert.assertTrue(secondSubscriberReceiveOnlyPayloadResult)
//
//    }

    @Test
    fun `Do not subscribe again if subscriber already exists`() = runBlocking {

        val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

        val subscriber = object : ServiceSubscriber {
            override fun onReceive(newState: ActorServiceState) {

            }
        }

        service.subscribe(subscriber)
        service.subscribe(subscriber)
        service.subscribe(subscriber)

        assertThat(service.getSubscribersCount()).isEqualTo(1)

        service.dispose()
    }

    @Test
    fun `No active subscribers if service is disposed`() = runBlocking {

        val service = ServiceFactory.buildSimpleService(this, Dispatchers.Default)

        val subscriber = object : ServiceSubscriber {
            override fun onReceive(newState: ActorServiceState) {

            }
        }

        service.subscribe(subscriber)

        service.dispose()

        assertThat(service.getSubscribersCount()).isEqualTo(0)
    }

    @Test
    fun `Can provide init service state with own state class type`() = runBlocking {
        var emptyResultOne = false

        val service = ServiceFactory.buildServiceWithCustomInit(this, Dispatchers.Default)

        val subscriber = object : ServiceSubscriber {
            override fun onReceive(newState: ActorServiceState) {
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

//    @Test
//    fun `Service stores arbitrary data in state handler`() = runBlocking {
//
//        val savedId = "123"
//
//        val stateHandler = TestMemorySavedStateStore()
//
//        val service = ServiceWithSavedStateHandlerCoroutine(
//            this,
//            Dispatchers.Default
//        )
//
//        service.dispatch(IntentMessage(SimpleIntentType(savedId)))
//
//        delay(testDelayBeforeCheckingResult)
//
//        val storedData = stateHandler.get<String>(SAVE_KEY)
//
//        assertThat(storedData).isEqualTo(savedId)
//
//        service.dispose()
//    }

//    @Test
//    fun `Service restore state if state handler contains data`() = runBlocking {
//
//        val savedId = "123"
//
//        val stateHandler = TestMemorySavedStateHandler()
//
//        val service = ServiceWithSavedStateHandlerCoroutine(
//            this,
//            Dispatchers.Default
//        )
//
//        service.setServiceSavedStateHandler(stateHandler)
//
//        service.dispatch(IntentMessage(SimpleServiceCoroutine.SimpleIntentType(savedId)))
//
//        delay(testDelayBeforeCheckingResult)
//
//        service.dispose()
//
//        val serviceWithRestoring = ServiceWithSavedStateHandlerCoroutine(
//            this,
//            Dispatchers.Default
//        )
//
//        var isGotInitResult = false
//        var isGotPreviousServiceResult = false
//
//        val subscriber = object : ServiceSubscriber {
//            override fun onReceive(newState: ActorServiceState) {
//                if (stateWithResult?.result is ServiceResult.InitResult) {
//                    isGotInitResult = true
//                }
//                if (stateWithResult?.result is ServiceResult.BasicResult) {
//                    isGotPreviousServiceResult = stateWithResult.result.payload == savedId
//                }
//            }
//        }
//
//        serviceWithRestoring.subscribe(subscriber)
//
//        serviceWithRestoring.setServiceSavedStateHandler(stateHandler)
//
//        delay(testDelayBeforeCheckingResult)
//
//        Assert.assertTrue(isGotInitResult)
//        Assert.assertTrue(isGotPreviousServiceResult)
//
//        serviceWithRestoring.dispose()
//    }
}