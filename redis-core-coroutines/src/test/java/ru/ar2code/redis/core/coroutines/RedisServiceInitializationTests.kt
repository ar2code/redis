package ru.ar2code.redis.core.coroutines

import com.google.common.collect.BoundType
import com.google.common.collect.Range
import com.google.common.truth.Truth
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import ru.ar2code.redis.core.ServiceSubscriber
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.coroutines.prepares.Constants.awaitStateTimeout
import ru.ar2code.redis.core.coroutines.prepares.IntentTypeA
import ru.ar2code.redis.core.coroutines.prepares.ServiceFactory
import ru.ar2code.redis.core.coroutines.prepares.StateA
import ru.ar2code.redis.core.test.TestLogger
import java.lang.IllegalStateException

class RedisServiceInitializationTests {

    class ServiceWithErrorOnBeforeInitialization(
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher,
        logObjectName: String? = null
    ) : RedisCoroutineStateService(
        scope,
        dispatcher,
        State.Initiated(),
        ServiceFactory.defaultReducers,
        DefaultReducerSelector(),
        DefaultIntentSelector(),
        emptyList(),
        DefaultStateTriggerSelector(),
        null,
        null,
        null,
        TestLogger(),
        logObjectName,
        emitErrorAsState = true
    ) {
        override suspend fun onBeforeInitialization() {
            super.onBeforeInitialization()

            throw IllegalStateException("ServiceWithErrorOnBeforeInitialization onBeforeInitialization error")
        }
    }

    @Test
    fun testAtomicInitialization() = runBlocking {

        val service = ServiceWithErrorOnBeforeInitialization(this, Dispatchers.Default)

        val states = mutableListOf<State>()

        service.subscribe(object : ServiceSubscriber {
            override suspend fun onReceive(newState: State) {
                states.add(newState)
            }
        })

        service.dispatch(IntentTypeA())

        service.awaitStateWithTimeout(awaitStateTimeout, StateA::class)

        Truth.assertThat(states.size).isIn(Range.range(2, BoundType.CLOSED, 3, BoundType.CLOSED))

        if (states.size == 3) {
            //Got all states: init, error, type a
            Truth.assertThat(states[0]).isInstanceOf(State.Initiated::class.java)
            Truth.assertThat(states[1]).isInstanceOf(State.ErrorOccurred::class.java)
            Truth.assertThat(states[2]).isInstanceOf(StateA::class.java)
        } else if (states.size == 2) {
            //Got last states: error, type a. ErrorState wiped an init state, subscriber was very slow.
            Truth.assertThat(states[0]).isInstanceOf(State.ErrorOccurred::class.java)
            Truth.assertThat(states[1]).isInstanceOf(StateA::class.java)
        }

        service.dispose()
    }
}