package ru.ar2code.android.architecture.core

import junit.framework.Assert.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

import ru.ar2code.android.architecture.core.prepares.SimpleDelayedSyncUseCase
import ru.ar2code.android.architecture.core.prepares.SimpleExceptionUseCase
import ru.ar2code.android.architecture.core.models.UseCaseResult
import ru.ar2code.android.architecture.core.prepares.SimpleCustomAwaitConfigUseCase
import ru.ar2code.android.architecture.core.usecases.UseCaseCancelledException

@ExperimentalCoroutinesApi
class SynchronizedUseCaseTests {

    private fun <T> appendResult(
        results: StringBuilder,
        useCaseResult: UseCaseResult<T>,
        awaitPrefix: String
    ) where T : Any {
        when (useCaseResult) {
            is UseCaseResult.AwaitAnotherRunFinish -> results.append("${useCaseResult.payload}$awaitPrefix")
            else -> results.append(useCaseResult.payload)
        }
    }

    @Test
    fun `Flows that was run from same synchronized use case execute sequentially`() =
        runBlockingTest {

            val flow1param = "flow1"
            val flow2param = "flow2"
            val flow3param = "flow3"

            val useCase =
                SimpleDelayedSyncUseCase()
            val flow1 = useCase.run(flow1param)
            val flow2 = useCase.run(flow2param)
            val flow3 = useCase.run(flow3param)

            val expectedResult = "$flow1param$flow2param$flow3param"
            val results = StringBuilder()

            launch {
                flow1
                    .onCompletion {
                        results.append(flow1param)
                    }
                    .collect { }
            }
            launch {
                flow2
                    .onCompletion {
                        results.append(flow2param)
                    }
                    .collect { }
            }
            launch {
                flow3
                    .onCompletion {
                        results.append(flow3param)
                        assertEquals(expectedResult, results.toString())
                    }
                    .collect {}
            }
        }

    @Test
    fun `Flow that was run from same synchronized use case awaits previous flow finishing`() =
        runBlockingTest {

            val flow1param = "flow1"
            val flow2param = "flow2"
            val awaitPrefix = "_await"

            val useCase =
                SimpleDelayedSyncUseCase()
            val flow1 = useCase.run(flow1param)
            val flow2 = useCase.run(flow2param)

            //expected: f1-1emit/f2-await/f1-2emit/f2-1emit/f2-2emit
            val expectedResult =
                "$flow1param$flow2param$awaitPrefix$flow1param$flow2param$flow2param"
            val results = StringBuilder()

            launch {
                flow1.collect {
                    appendResult(results, it, awaitPrefix)
                }
            }
            launch {
                flow2
                    .onCompletion {
                        assertEquals(expectedResult, results.toString())
                    }
                    .collect {
                        appendResult(results, it, awaitPrefix)
                    }
            }

        }

    @Test
    fun `Flow propagates exception`() = runBlockingTest {
        val exceptionMsg = "test exception"
        val useCase =
            SimpleExceptionUseCase()
        val flow = useCase.run(exceptionMsg)
        var isExceptionOccurred = false

        flow
            .catch { th ->
                isExceptionOccurred = true
                assertEquals(exceptionMsg, th.message)
            }
            .collect { }

        assertTrue(isExceptionOccurred)
    }

    @Test
    fun `Flow throws cancellation exception after use case cancelled`() = runBlockingTest {
        val useCase = SimpleDelayedSyncUseCase()
        val flow = useCase.run("")

        var isExceptionOccurred = false

        flow
            .catch { th ->
                isExceptionOccurred = true
                assertTrue(th is UseCaseCancelledException)
            }
            .collect {
                useCase.cancel()
            }

        assertTrue(isExceptionOccurred)
    }

    @Test
    fun `Flow does not emit AwaitResult if option is disabled in config`() = runBlockingTest {
        val flow1param = "flow1"
        val flow2param = "flow2"
        val awaitPrefix = "_await"

        val useCase =
            SimpleCustomAwaitConfigUseCase()
        val flow1 = useCase.run(flow1param)
        val flow2 = useCase.run(flow2param)

        //expected: f1-1emit/f1-2emit/f2-1emit/f2-2emit
        val expectedResult =
            "$flow1param$flow1param$flow2param$flow2param"
        val results = StringBuilder()

        launch {
            flow1
                .collect {
                    appendResult(results, it, awaitPrefix)
                }
        }
        launch {
            flow2
                .onCompletion {
                    val actual = results.toString()
                    assertEquals(expectedResult, actual)
                    assertFalse(actual.contains(awaitPrefix))
                }
                .collect {
                    appendResult(results, it, awaitPrefix)
                }
        }
    }

    @Test
    fun `Flow is cancelled with exception after timeout`() = runBlockingTest {
        val flow1param = "flow1"
        val flow2param = "flow2"

        val timeoutMs = 500L

        val useCase =
            SimpleCustomAwaitConfigUseCase(timeoutMs)
        val flow1 = useCase.run(flow1param)
        val flow2 = useCase.run(flow2param)

        var flow2CollectedSomething = false

        launch {
            flow1
                .collect {}
        }
        launch {
            flow2
                .onCompletion { th ->
                    assertFalse(flow2CollectedSomething)
                    assertNotNull(th)
                    assertTrue(th is TimeoutCancellationException)
                }
                .collect {
                    flow2CollectedSomething = true
                }
        }
    }
}