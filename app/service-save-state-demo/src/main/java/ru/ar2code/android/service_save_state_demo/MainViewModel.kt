/*
 * Copyright (c) 2020. Created by Alexey Rozhkov.
 */

package ru.ar2code.android.service_save_state_demo

import androidx.lifecycle.*
import ru.ar2code.android.architecture.core.models.IntentMessage
import ru.ar2code.android.architecture.core.services.ServiceSavedStateHandler
import ru.ar2code.android.architecture.core.services.ServiceStateWithResult
import ru.ar2code.android.architecture.core.services.ServiceSubscriber

class MainViewModel(private val state: SavedStateHandle) : ViewModel() {

    private val serviceStateHandler = object : ServiceSavedStateHandler {
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

    private val service = MainService(viewModelScope, serviceStateHandler)

    private val viewState = MutableLiveData<String>()
    val viewStateLive = viewState as LiveData<String>

    init {
        service.subscribe(object : ServiceSubscriber<String> {
            override fun onReceive(stateWithResult: ServiceStateWithResult<String>?) {
                viewState.postValue(stateWithResult?.result?.payload)
            }
        })
    }

    fun onButtonClick() {
        service.sendIntent(IntentMessage(MainService.MainServiceIntentType()))
    }

}