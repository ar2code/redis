/*
 * Copyright (c) 2020. Created by Alexey Rozhkov.
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