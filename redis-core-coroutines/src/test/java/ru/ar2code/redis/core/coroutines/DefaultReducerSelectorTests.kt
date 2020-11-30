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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.coroutines.prepares.*


class DefaultReducerSelectorTests {

    @Test
    fun `find correct reducer for concrete reducer and intent - Initiated state IntentTypeA returns InitiatedStateTypeAReducer`() {

        val reducerSelector = DefaultReducerSelector()

        val reducer = reducerSelector.findReducer(
            ServiceFactory.defaultReducers,
            State.Initiated(),
            IntentTypeA()
        )

        assertThat(reducer).isInstanceOf(InitiatedStateTypeAReducer::class.java)
    }

    @Test
    fun `find correct reducer for concrete reducer and intent - Initiated state IntentTypeB returns InitiatedStateTypeBReducer`() {

        val reducerSelector = DefaultReducerSelector()

        val reducer = reducerSelector.findReducer(
            ServiceFactory.defaultReducers,
            State.Initiated(),
            IntentTypeB()
        )

        assertThat(reducer).isInstanceOf(InitiatedStateTypeBReducer::class.java)
    }

    @Test
    fun `find correct reducer for concrete reducer and intent - StateB IntentTypeFlow returns StateBTypeFlowReducer`() {

        val reducerSelector = DefaultReducerSelector()

        val reducer =
            reducerSelector.findReducer(ServiceFactory.defaultReducers, StateB(), IntentTypeFlow())

        assertThat(reducer).isInstanceOf(StateBTypeFlowReducer::class.java)
    }

    @Test
    fun `find reducer for any state and concrete intent - StateC IntentTypeC returns AnyStateTypeCReducer`() {

        val reducerSelector = DefaultReducerSelector()

        val reducers = ServiceFactory.defaultReducers.toMutableList()
        reducers.add(AnyStateTypeCReducer())

        val reducer = reducerSelector.findReducer(reducers, StateC(), IntentTypeC())

        assertThat(reducer).isInstanceOf(AnyStateTypeCReducer::class.java)
    }

    @Test(expected = ReducerNotFoundException::class)
    fun `reducer was not found - StateC IntentTypeB throws ReducerNotFoundException`() {

        val reducerSelector = DefaultReducerSelector()

        val reducers = ServiceFactory.defaultReducers.toMutableList()
        reducers.add(AnyStateTypeCReducer())

        reducerSelector.findReducer(reducers, StateC(), IntentTypeB())
    }

    @Test
    fun `find reducer for any state any intent - StateC IntentTypeB returns AnyStateAnyTypeReducer`() {

        val reducerSelector = DefaultReducerSelector()

        val reducers = ServiceFactory.defaultReducers.toMutableList()
        reducers.add(AnyStateTypeCReducer())
        reducers.add(AnyStateAnyTypeReducer())

        val reducer = reducerSelector.findReducer(reducers, StateC(), IntentTypeB())

        assertThat(reducer).isInstanceOf(AnyStateAnyTypeReducer::class.java)
    }

    @Test
    fun `find reducer for concrete state any intent - StateC IntentTypeB returns StateCAnyTypeReducer`() {

        val reducerSelector = DefaultReducerSelector()

        val reducers = ServiceFactory.defaultReducers.toMutableList()
        reducers.add(StateCAnyTypeReducer())

        val reducer = reducerSelector.findReducer(reducers, StateC(), IntentTypeB())

        assertThat(reducer).isInstanceOf(StateCAnyTypeReducer::class.java)
    }
}