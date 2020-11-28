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

package ru.ar2code.redis.clean.arch.coroutins

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import ru.ar2code.redis.clean.arch.coroutins.prepares.SimpleDelayedSyncUseCase
import ru.ar2code.redis.clean.arch.coroutins.prepares.SimpleExceptionUseCase
import ru.ar2code.redis.clean.arch.coroutins.prepares.SimpleCustomAwaitConfigUseCase
import ru.ar2code.redis.clean.arch.coroutines.UseCaseCancelledException


class SynchronizedUseCaseTests {

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
                        assertThat(results.toString()).isEqualTo(expectedResult)
                    }
                    .collect {}
            }
        }

    @Test
    fun `Flow that was run from same synchronized use case awaits previous flow finishing`() =
        runBlockingTest {

            val flow1param = "flow1"
            val flow2param = "flow2"

            val useCase =
                SimpleDelayedSyncUseCase()
            val flow1 = useCase.run(flow1param)
            val flow2 = useCase.run(flow2param)

            //expected: f1-1emit/f1-2emit/f2-1emit/f2-2emit
            val expectedResult =
                "$flow1param$flow1param$flow2param$flow2param"
            val results = StringBuilder()

            launch {
                flow1.collect {
                    results.append(it)
                }
            }
            launch {
                flow2
                    .onCompletion {
                        assertThat(results.toString()).isEqualTo(expectedResult)
                    }
                    .collect {
                        results.append(it)
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
                assertThat(th.message).isEqualTo(exceptionMsg)
            }
            .collect { }

        assertThat(isExceptionOccurred)
    }

    @Test
    fun `Flow throws cancellation exception after use case cancelled`() = runBlockingTest {
        val useCase = SimpleDelayedSyncUseCase()
        val flow = useCase.run("")

        var isExceptionOccurred = false

        flow
            .catch { th ->
                isExceptionOccurred = true
                assertThat(th).isInstanceOf(UseCaseCancelledException::class.java)
            }
            .collect {
                useCase.cancel()
            }

        assertThat(isExceptionOccurred)
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
                    assertThat(flow2CollectedSomething).isFalse()
                    assertThat(th).isNotNull()
                    assertThat(th).isInstanceOf(TimeoutCancellationException::class.java)
                }
                .collect {
                    flow2CollectedSomething = true
                }
        }
    }
}