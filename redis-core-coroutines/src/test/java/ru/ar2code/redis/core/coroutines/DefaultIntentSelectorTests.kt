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
import ru.ar2code.redis.core.coroutines.prepares.*

class DefaultIntentSelectorTests {

    @Test
    fun `test find correct intent for state - StateA to IntentA`() {

        val intentSelector = DefaultIntentSelector()

        val intent = intentSelector.findIntent(ServiceFactory.defaultStateIntentsWithAny, StateA())

        Truth.assertThat(intent).isInstanceOf(IntentTypeA::class.java)
    }

    @Test
    fun `test find correct intent for state - StateB to IntentB`() {

        val intentSelector = DefaultIntentSelector()

        val intent = intentSelector.findIntent(ServiceFactory.defaultStateIntentsWithAny, StateB())

        Truth.assertThat(intent).isInstanceOf(IntentTypeB::class.java)
    }

    @Test
    fun `test find correct intent for state - StateC to IntentC`() {

        val intentSelector = DefaultIntentSelector()

        val intent = intentSelector.findIntent(ServiceFactory.defaultStateIntentsWithAny, StateC())

        Truth.assertThat(intent).isInstanceOf(IntentTypeC::class.java)
    }

    @Test(expected = IntentNotFoundException::class)
    fun `test throw exception if intent for specified state not found`() {

        val intentSelector = DefaultIntentSelector()

        val intent = intentSelector.findIntent(ServiceFactory.defaultStateIntents, CustomInitState())
    }

    @Test
    fun `test found default intent if there is no specified intent for state`() {

        val intentSelector = DefaultIntentSelector()

        val intent = intentSelector.findIntent(ServiceFactory.defaultStateIntentsWithAny, CustomInitState())

        Truth.assertThat(intent).isInstanceOf(IntentTypeFlow::class.java)
    }
}