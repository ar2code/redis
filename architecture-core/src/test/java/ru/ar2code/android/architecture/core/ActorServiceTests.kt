package ru.ar2code.android.architecture.core

import kotlinx.coroutines.*
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import ru.ar2code.android.architecture.core.models.ServiceResult
import ru.ar2code.android.architecture.core.prepares.SimpleService
import ru.ar2code.android.architecture.core.services.ServiceSubscriber

class ActorServiceTests {

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @ExperimentalCoroutinesApi
    class MainCoroutineRule(
        val testDispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()
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

    @Test
    fun `Concurrent subscribe and unsubscribe (2000 subs) give no any concurrent exceptions`() = runBlocking(Dispatchers.Default) {

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

}