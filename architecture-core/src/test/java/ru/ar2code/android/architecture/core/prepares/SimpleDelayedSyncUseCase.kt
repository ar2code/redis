/*
 * Copyright (c) 2020.  The Redim Open Source Project
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

package ru.ar2code.android.architecture.core.prepares

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.ar2code.android.architecture.core.models.UseCaseResult
import ru.ar2code.android.architecture.core.usecases.SynchronizedUseCase

class SimpleDelayedSyncUseCase : SynchronizedUseCase<String, String>(
    DefaultSynchronizedUseCaseAwaitConfig(),
    SimpleTestLogger()
) {

    private var flowParam: String? = null

    override fun execute(params: String?): Flow<UseCaseResult<String>> {
        flowParam = params

        return flow {
            emit(UseCaseResult(params))
            delay(1000)
            emit(UseCaseResult(params))
        }
    }

    override fun getPayloadForAwaitResult(): String? {
        return flowParam
    }
}