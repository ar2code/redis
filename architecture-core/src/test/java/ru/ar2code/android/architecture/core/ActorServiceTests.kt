package ru.ar2code.android.architecture.core

import kotlinx.coroutines.*
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import ru.ar2code.android.architecture.core.models.IntentMessage
import ru.ar2code.android.architecture.core.models.ServiceResult
import ru.ar2code.android.architecture.core.prepares.SimpleService
import ru.ar2code.android.architecture.core.services.ServiceSubscriber

@ExperimentalCoroutinesApi
class ActorServiceTests {

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @ExperimentalCoroutinesApi
    class MainCoroutineRule(
        private val testDispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()
    ) : TestWatcher() {

        override fun starting(description: Description?) {
            super.starting(description)
            Dispatchers.setMain(testDispatcher)
        }

        override fun finished(description: Description?) {
            super.finished(description)
            Dispatchers.resetMain()
            testDispatcher.cleanupTestCoroutines()
        }
    }

    private val testDelayBeforeCheckingResult = 10L

    @Test
    fun `Concurrent subscribe and unsubscribe (2000 subs) gives no any concurrent exceptions`() =
        runBlocking(Dispatchers.Default) {

            println("Start ActionService concurrent test")

            val service = SimpleService(this, Dispatchers.Default)
            val subscriptions = mutableListOf<ServiceSubscriber<String>>()

            val d1 = async {
                println("start subscribe task 1")
                repeat(1000) {
                    val subscriber = object : ServiceSubscriber<String> {
                        override fun onReceive(result: ServiceResult<String>?) {

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
                    val subscriber = object : ServiceSubscriber<String> {
                        override fun onReceive(result: ServiceResult<String>?) {

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
                    val subscriber = object : ServiceSubscriber<String> {
                        override fun onReceive(result: ServiceResult<String>?) {

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

        val service = SimpleService(scope, Dispatchers.Default)

        scope.cancel()

        Assert.assertTrue(service.isDisposed())
    }

    @Test(expected = IllegalStateException::class)
    fun `When scope throw exception on attempt to subscribe again`() = runBlocking {

        val service = SimpleService(this, Dispatchers.Default)

        this.cancel()

        val subscriber = object : ServiceSubscriber<String> {
            override fun onReceive(result: ServiceResult<String>?) {

            }
        }
        service.subscribe(subscriber)

        Assert.assertTrue(service.isDisposed())
    }

    @Test(expected = IllegalStateException::class)
    fun `When scope throw exception on attempt to send intent`() = runBlocking {

        val service = SimpleService(this, Dispatchers.Default)

        this.cancel()

        service.sendIntent(IntentMessage(SimpleService.SimpleIntentType()))

        Assert.assertTrue(service.isDisposed())
    }

    @Test
    fun `Receive result from subscription after sending intent`() = runBlocking {
        val service = SimpleService(this, Dispatchers.Default)

        val payload = "1"
        var emptyResult = false
        var gotResult = false

        val subscriber = object : ServiceSubscriber<String> {
            override fun onReceive(result: ServiceResult<String>?) {
                if (result is ServiceResult.EmptyResult) {
                    emptyResult = true
                    return
                }

                gotResult = result?.payload == payload

                service.dispose()
            }
        }
        service.subscribe(subscriber)

        service.sendIntent(IntentMessage(SimpleService.SimpleIntentType(payload)))

        delay(testDelayBeforeCheckingResult)

        Assert.assertTrue(gotResult)
        Assert.assertTrue(emptyResult)
    }

    @Test
    fun `Receive empty result from subscription after service initiated`() = runBlocking {
        val service = SimpleService(this, Dispatchers.Default)

        var emptyResult = false

        val subscriber = object : ServiceSubscriber<String> {
            override fun onReceive(result: ServiceResult<String>?) {
                if (result is ServiceResult.EmptyResult) {
                    emptyResult = true
                    service.dispose()
                }
            }
        }
        service.subscribe(subscriber)

        delay(testDelayBeforeCheckingResult)

        Assert.assertTrue(emptyResult)
    }

    @Test
    fun `Receive empty result from all subscriptions after service initiated`() = runBlocking {
        val service = SimpleService(this, Dispatchers.Default)

        var emptyResult = false
        var emptyResultTwo = false

        service.subscribe(object : ServiceSubscriber<String> {
            override fun onReceive(result: ServiceResult<String>?) {
                if (result is ServiceResult.EmptyResult) {
                    emptyResult = true
                }
            }
        })

        service.subscribe(object : ServiceSubscriber<String> {
            override fun onReceive(result: ServiceResult<String>?) {
                if (result is ServiceResult.EmptyResult) {
                    emptyResultTwo = true
                }
            }
        })

        delay(testDelayBeforeCheckingResult)

        service.dispose()

        Assert.assertTrue(emptyResult)
        Assert.assertTrue(emptyResultTwo)
    }

    @Test
    fun `Receive last result from newly added subscription`() = runBlocking {
        val service = SimpleService(this, Dispatchers.Default)
        val payload = "1"

        var emptyResultOne = false
        var payloadResultOne = false

        var emptyResultTwo = false
        var payloadResultTwo = false

        service.subscribe(object : ServiceSubscriber<String> {
            override fun onReceive(result: ServiceResult<String>?) {
                if (result is ServiceResult.EmptyResult) {
                    emptyResultOne = true
                } else {
                    payloadResultOne = result?.payload == payload
                }
            }
        })

        service.sendIntent(IntentMessage(SimpleService.SimpleIntentType(payload)))

        delay(testDelayBeforeCheckingResult)

        service.subscribe(object : ServiceSubscriber<String> {
            override fun onReceive(result: ServiceResult<String>?) {
                if (result is ServiceResult.EmptyResult) {
                    emptyResultTwo = true
                } else {
                    payloadResultTwo = result?.payload == payload
                }
            }
        })

        delay(testDelayBeforeCheckingResult)

        service.dispose()

        val firstSubscriberReceiveEmptyAndPayloadResult = emptyResultOne && payloadResultOne
        val secondSubscriberReceiveOnlyPayloadResult = !emptyResultTwo && payloadResultTwo

        Assert.assertTrue(firstSubscriberReceiveEmptyAndPayloadResult)
        Assert.assertTrue(secondSubscriberReceiveOnlyPayloadResult)

    }

    @Test
    fun `Do not subscribe again if subscriber already exists`() = runBlocking {

        val service = SimpleService(this, Dispatchers.Default)

        val subscriber = object : ServiceSubscriber<String> {
            override fun onReceive(result: ServiceResult<String>?) {

            }
        }

        service.subscribe(subscriber)
        service.subscribe(subscriber)
        service.subscribe(subscriber)

        Assert.assertEquals(1, service.getSubscribersCount())

        service.dispose()
    }

    @Test
    fun `No active subscribers if service is disposed`() = runBlocking {

        val service = SimpleService(this, Dispatchers.Default)

        val subscriber = object : ServiceSubscriber<String> {
            override fun onReceive(result: ServiceResult<String>?) {

            }
        }

        service.subscribe(subscriber)

        service.dispose()

        Assert.assertEquals(0, service.getSubscribersCount())
    }

    @Test
    fun `Can override init service result with own class type`() = runBlocking {
        var emptyResultOne = false

        val service = SimpleService(this, Dispatchers.Default)

        val subscriber = object : ServiceSubscriber<String> {
            override fun onReceive(result: ServiceResult<String>?) {
                if (result is SimpleService.SimpleEmptyResult) {
                    emptyResultOne = true
                }
            }
        }

        service.subscribe(subscriber)

        delay(testDelayBeforeCheckingResult)

        Assert.assertTrue(emptyResultOne)

        service.dispose()
    }

    @Test
    fun `Service will not send result if can change state returns false`() = runBlocking {
        var emptyResultOne = false

        val service =
            SimpleService(this, Dispatchers.Default, canChangeStateCallback = { _, _ -> false })

        val subscriber = object : ServiceSubscriber<String> {
            override fun onReceive(result: ServiceResult<String>?) {
                if (result is SimpleService.SimpleEmptyResult) {
                    emptyResultOne = true
                }
            }
        }

        service.subscribe(subscriber)

        delay(testDelayBeforeCheckingResult)

        Assert.assertFalse(emptyResultOne)

        service.dispose()
    }

    @Test
    fun `Change service state with providing result`() = runBlocking {

        val state = SimpleService.SimpleState()

        val service =
            SimpleService(
                this,
                Dispatchers.Default,
                newStateCallback = {
                    state
                })

        val subscriber = object : ServiceSubscriber<String> {
            override fun onReceive(result: ServiceResult<String>?) {

            }
        }

        service.subscribe(subscriber)

        service.sendIntent(IntentMessage(SimpleService.SimpleIntentType()))

        delay(testDelayBeforeCheckingResult)

        Assert.assertEquals(state, service.serviceState)

        service.dispose()
    }
}