package ru.ar2code.redis.core.coroutines

import com.google.common.truth.Truth
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import org.junit.Test
import ru.ar2code.redis.core.IntentMessage
import ru.ar2code.redis.core.ServiceSubscriber
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.coroutines.prepares.Constants
import ru.ar2code.redis.core.coroutines.test.awaitWhileNotDisposedWithTimeout
import ru.ar2code.redis.core.test.TestLogger

@ExperimentalCoroutinesApi
class ConcurrentReducerTest {

    class ConcurrentService(scope: CoroutineScope) : RedisCoroutineStateService(
        scope,
        Dispatchers.IO,
        State.Initiated(),
        listOf(InitiatedStateOnStartIntentReducer()),
        DefaultReducerSelector(),
        DefaultIntentSelector(),
        null,
        DefaultStateTriggerSelector(),
        null,
        null,
        null,
        TestLogger(),
        null,
        false
    ) {
        class StartIntent : IntentMessage()

        open class PartialResultState(val number: Int) : State() {
            override fun clone(): State {
                return PartialResultState(number)
            }
        }

        class FinishState(number: Int) : PartialResultState(number) {
            override fun clone(): State {
                return FinishState(number)
            }
        }

        class InitiatedStateOnStartIntentReducer :
            StateReducer<State, StartIntent>(TestLogger()) {
            override val isAnyState: Boolean
                get() = true
            override val isAnyIntent: Boolean
                get() = false

            override fun reduce(currentState: State, intent: StartIntent): Flow<State> {

                println("start InitiatedStateOnStartIntentReducer")

                return channelFlow {
                    val l1 = partialAsync(3, 1500)
                    val l2 = partialAsync(1, 500)
                    val l3 = partialAsync(2, 1000)

                    awaitAll(l1, l2, l3)

                    send(FinishState(4))
                }
            }

            private fun ProducerScope<State>.partialAsync(
                number: Int,
                delayTime: Long

            ): Deferred<Unit> {
                println("enter partial result fun: $number")
                val l1 = async {
                    partialResult(number, delayTime, this@partialAsync)
                }

                return l1
            }

            override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
                return intent is StartIntent
            }

            private suspend fun partialResult(
                number: Int,
                delayTime: Long,
                flowCollector: ProducerScope<State>
            ) {

                delay(delayTime)
                println("send partial result: $number")
                flowCollector.send(PartialResultState(number))

            }
        }
    }

    data class TestResult(val number: Int, val timeAt: Long)

    @Test
    fun testConcurrentPartialResults() = runBlocking {
        val service = ConcurrentService(this)

        val results = mutableListOf<TestResult>()

        service.subscribe(object : ServiceSubscriber {
            override suspend fun onReceive(newState: State) {
                if (newState is ConcurrentService.PartialResultState) {
                    println("receive partial result: ${newState.number}")
                    results.add(TestResult(newState.number, System.currentTimeMillis()))
                }

                if (newState is ConcurrentService.FinishState) {
                    service.dispose()
                }
            }
        })

        service.dispatch(ConcurrentService.StartIntent())

        service.awaitWhileNotDisposedWithTimeout(Constants.awaitDisposedStateTimeout)

        println("result=$results")

        Truth.assertThat(results[0].number).isEqualTo(1)
        Truth.assertThat(results[0].timeAt).isLessThan(results[1].timeAt)

        Truth.assertThat(results[1].number).isEqualTo(2)
        Truth.assertThat(results[1].timeAt).isLessThan(results[2].timeAt)

        Truth.assertThat(results[2].number).isEqualTo(3)
        Truth.assertThat(results[2].timeAt).isLessThan(results[3].timeAt)

        Truth.assertThat(results[3].number).isEqualTo(4)
    }
}