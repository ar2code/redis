package ru.ar2code.redis.core.coroutines

import com.google.common.truth.Truth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import org.junit.Test
import ru.ar2code.redis.core.IntentMessage
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.coroutines.prepares.IntentTypeA
import ru.ar2code.redis.core.coroutines.prepares.IntentTypeB
import ru.ar2code.redis.core.coroutines.prepares.StateA
import ru.ar2code.redis.core.coroutines.prepares.StateB
import ru.ar2code.redis.core.test.TestLogger

class StateReducerTest {

    class StateAOnIntentTypeBReducer : StateReducer<StateA, IntentTypeB>(TestLogger()) {

        override val isAnyIntent: Boolean
            get() = false

        override val isAnyState: Boolean
            get() = false

        override fun reduce(currentState: StateA, intent: IntentTypeB): Flow<State> {
            return flow {
                emit(StateB())
            }
        }

        override fun isReducerApplicable(currentState: State, intent: IntentMessage): Boolean {
            return currentState is StateA && intent is IntentTypeB
        }
    }

    @Test
    fun testReduce() = runBlocking {
        val reducer = StateAOnIntentTypeBReducer()

        val result = reducer.reduceState(StateA(), IntentTypeB())?.single()

        Truth.assertThat(result).isInstanceOf(StateB::class.java)
    }

    @Test
    fun givenStateBIntentTypeB_whenIsReducerApplicable_ReturnFalse() = runBlocking {
        val reducer = StateAOnIntentTypeBReducer()

        val result = reducer.isReducerApplicable(StateB(), IntentTypeB())

        Truth.assertThat(result).isFalse()
    }

    @Test
    fun givenStateAIntentTypeB_whenIsReducerApplicable_ReturnTrue() = runBlocking {
        val reducer = StateAOnIntentTypeBReducer()

        val result = reducer.isReducerApplicable(StateA(), IntentTypeB())

        Truth.assertThat(result).isTrue()
    }

    @Test
    fun givenStateAIntentTypeA_whenIsReducerApplicable_ReturnFalse() = runBlocking {
        val reducer = StateAOnIntentTypeBReducer()

        val result = reducer.isReducerApplicable(StateA(), IntentTypeA())

        Truth.assertThat(result).isFalse()
    }

    @Test(expected = ClassCastException::class)
    fun givenStateBIntentTypeB_whenReduce_ThenCastExceptionOccurred() = runBlocking {
        val reducer = StateAOnIntentTypeBReducer()

        reducer.reduceState(StateB(), IntentTypeB())?.single()

        Unit
    }
}