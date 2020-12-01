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

import com.google.common.truth.Truth
import org.junit.Test
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.coroutines.prepares.*


class DefaultStateTriggerSelectorTests {

    @Test
    fun `find trigger - Initiated to StateA returns InitiatedToAStateTrigger`() {

        val triggerSelector = DefaultStateTriggerSelector()

        val trigger =
            triggerSelector.findTrigger(ServiceFactory.defaultTriggers, State.Initiated(), StateA())

        Truth.assertThat(trigger).isInstanceOf(InitiatedToAStateTrigger::class.java)
    }

    @Test
    fun `find trigger - Initiated to StateB returns InitiatedToBStateTrigger`() {

        val triggerSelector = DefaultStateTriggerSelector()

        val trigger =
            triggerSelector.findTrigger(ServiceFactory.defaultTriggers, State.Initiated(), StateB())

        Truth.assertThat(trigger).isInstanceOf(InitiatedToBStateTrigger::class.java)
    }

    @Test
    fun `trigger not found returns null`() {

        val triggerSelector = DefaultStateTriggerSelector()

        val trigger =
            triggerSelector.findTrigger(ServiceFactory.defaultTriggers, StateA(), StateB())

        Truth.assertThat(trigger).isNull()
    }

    @Test
    fun `find any trigger - StateA to StateC returns AnyToCStateTrigger`() {

        val triggerSelector = DefaultStateTriggerSelector()

        val trigger =
            triggerSelector.findTrigger(ServiceFactory.defaultTriggersWithAny, StateA(), StateC())

        Truth.assertThat(trigger).isInstanceOf(AnyToCStateTrigger::class.java)
    }

    @Test
    fun `find any trigger - StateA to FlowStateD returns InitiatedToAnyStateTrigger`() {

        val triggerSelector = DefaultStateTriggerSelector()

        val trigger = triggerSelector.findTrigger(
            ServiceFactory.defaultTriggersWithAny,
            State.Initiated(),
            FlowStateD(FlowStateD.NAME)
        )

        Truth.assertThat(trigger).isInstanceOf(InitiatedToAnyStateTrigger::class.java)
    }
}