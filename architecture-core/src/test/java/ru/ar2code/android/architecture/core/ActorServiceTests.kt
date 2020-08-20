package ru.ar2code.android.architecture.core

import kotlinx.coroutines.*
import org.junit.Assert
import org.junit.Test
import ru.ar2code.android.architecture.core.models.IntentMessage
import ru.ar2code.android.architecture.core.models.ServiceResult
import ru.ar2code.android.architecture.core.prepares.*
import ru.ar2code.android.architecture.core.services.ServiceSubscriber

@ExperimentalCoroutinesApi
class ActorServiceTests {

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

    @Test(expected = ServiceWithExceptionInsideIntentHandling.TestException::class)
    fun `Propagated exception inside intent handling block`() = runBlocking {
        val service = ServiceWithExceptionInsideIntentHandling(this, Dispatchers.Default)

        service.sendIntent(IntentMessage(SimpleService.SimpleIntentType()))
    }

    @Test(expected = ServiceWithExceptionInsideIntentHandling.TestException::class)
    fun `Propagated exception inside intent can change state block`() = runBlocking {
        val service = ServiceWithExceptionInsideCanChangeState(this, Dispatchers.Default)

        service.sendIntent(IntentMessage(SimpleService.SimpleIntentType()))
    }

    @Test(expected = ServiceWithExceptionInsideIntentHandling.TestException::class)
    fun `Propagated exception inside intent can init state block`() = runBlocking {
        ServiceWithExceptionInsideInitResult(this, Dispatchers.Default)
        Unit
    }

    @Test
    fun `Receive result from subscription after sending intent`() = runBlocking {
        val service = SimpleService(this, Dispatchers.Default)

        val payload = "1"
        var emptyResult = false
        var gotResult = false

        val subscriber = object : ServiceSubscriber<String> {
            override fun onReceive(result: ServiceResult<String>?) {
                if (result is ServiceResult.InitResult) {
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
    fun `Receive init result from subscription after service initiated`() = runBlocking {
        val service = SimpleService(this, Dispatchers.Default)

        var emptyResult = false

        val subscriber = object : ServiceSubscriber<String> {
            override fun onReceive(result: ServiceResult<String>?) {
                if (result is ServiceResult.InitResult) {
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
    fun `Receive init result from all subscriptions after service initiated`() = runBlocking {
        val service = SimpleService(this, Dispatchers.Default)

        var emptyResult = false
        var emptyResultTwo = false

        service.subscribe(object : ServiceSubscriber<String> {
            override fun onReceive(result: ServiceResult<String>?) {
                if (result is ServiceResult.InitResult) {
                    emptyResult = true
                }
            }
        })

        service.subscribe(object : ServiceSubscriber<String> {
            override fun onReceive(result: ServiceResult<String>?) {
                if (result is ServiceResult.InitResult) {
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
                if (result is ServiceResult.InitResult) {
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
                if (result is ServiceResult.InitResult) {
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

        val service = ServiceWithCustomInitResult(this, Dispatchers.Default)

        val subscriber = object : ServiceSubscriber<String> {
            override fun onReceive(result: ServiceResult<String>?) {
                if (result is ServiceWithCustomInitResult.CustomInitResult) {
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
        var gotNotInitiatedResult = false

        val service =
            ServiceNotAllowAnyResult(this, Dispatchers.Default)

        val subscriber = object : ServiceSubscriber<String> {
            override fun onReceive(result: ServiceResult<String>?) {
                if (result !is ServiceResult.InitResult) {
                    gotNotInitiatedResult = true
                }
            }
        }

        service.subscribe(subscriber)

        service.sendIntent(IntentMessage(SimpleService.SimpleIntentType()))

        delay(testDelayBeforeCheckingResult)

        Assert.assertFalse(gotNotInitiatedResult)

        service.dispose()
    }

    @Test
    fun `Change service state with providing result`() = runBlocking {

        val state = SimpleService.SimpleState()

        val service =
            SimpleService(
                this,
                Dispatchers.Default)

        val subscriber = object : ServiceSubscriber<String> {
            override fun onReceive(result: ServiceResult<String>?) {

            }
        }

        service.subscribe(subscriber)

        service.sendIntent(IntentMessage(SimpleService.SimpleIntentType()))

        delay(testDelayBeforeCheckingResult)

        Assert.assertTrue(service.serviceState is SimpleService.SimpleState)

        service.dispose()
    }
}