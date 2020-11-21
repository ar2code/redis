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

package ru.ar2code.android.service_save_state_demo

import androidx.lifecycle.*
import ru.ar2code.redis.core.ServiceSubscriber
import ru.ar2code.redis.core.State
import ru.ar2code.redis.core.SavedStateStore

class MainViewModel(private val state: SavedStateHandle) : ViewModel() {

    private val serviceStateHandler = object : SavedStateStore {
        override fun <T> get(key: String): T? {
            return state.get(key)
        }

        override fun <T> set(key: String, value: T?) {
            state.set(key, value)
        }

        override fun keys(): List<String> {
            return state.keys().toList()
        }
    }

    private val service = MainServiceRedisCoroutineCoroutine(viewModelScope, serviceStateHandler)

    private val viewState = MutableLiveData<Long>()
    val viewStateLive = viewState as LiveData<Long>

    init {
        service.subscribe(object : ServiceSubscriber {
            override suspend fun onReceive(newState: State) {
                val keep = newState as? MainServiceRedisCoroutineCoroutine.KeepState
                keep?.let {
                    viewState.postValue(it.timestamp)
                }
            }
        })
    }

    fun onButtonClick() {
        service.dispatch(MainServiceRedisCoroutineCoroutine.MainIntent(System.currentTimeMillis()))
    }

}