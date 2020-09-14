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

package ru.ar2code.demo.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.ar2code.android.architecture.core.models.UseCaseResult
import ru.ar2code.defaults.DefaultSynchronizedUseCase

class SimpleUseCase  : DefaultSynchronizedUseCase<String, String>() {
    override fun execute(params: String?): Flow<UseCaseResult<String>> {
        return flow {
            emit(UseCaseResult(params))
            kotlinx.coroutines.delay(100)
            emit(UseCaseResult(params))
        }
    }
}