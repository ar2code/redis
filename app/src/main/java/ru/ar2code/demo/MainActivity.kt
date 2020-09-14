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

package ru.ar2code.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.receiveOrNull
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import ru.ar2code.android.architecture.core.models.IntentMessage
import ru.ar2code.android.architecture.core.models.ServiceResult
import ru.ar2code.android.architecture.core.services.ServiceSubscriber
import ru.ar2code.demo.impl.ActionOneIntentMsg
import ru.ar2code.demo.impl.DemoService
import ru.ar2code.demo.impl.DemoUseCase
import ru.ar2code.demo.impl.DemoViewModel

@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity() {

    val tag = "Ar2CodeAndroidArchCore"

//    private val job = Job()
//    private val service = DemoService(GlobalScope + job)
//
//    private val job2 = Job()
//    private lateinit var service2: DemoService

    private val viewModel : DemoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel.viewStateLive.observe(this, Observer {
            if(it.isChangedSincePrevious || textView.text.isNullOrEmpty()) {
                Toast.makeText(this, it.state, Toast.LENGTH_LONG).show()
                textView.setText(it.state)
            }
            Log.d(tag, "main activity viewStateLive observer ${it.state}")
        })

        viewModel.viewEventLive.observe(this, Observer {
            Toast.makeText(this, "event", Toast.LENGTH_LONG).show()
            Log.d(tag, "main activity viewEventLive observer ${it.data?.viewEventType?.payload}")
        })



//        var i = 0
//        val subs = object : ServiceSubscriber<String> {
//            override fun onReceive(result: ServiceResult<String>?) {
//                Log.d(tag, "main activity service result $i: ${result?.payload}")
//                i++
//
//                GlobalScope.launch(Dispatchers.Main) {
//                    text.text = i.toString()
//                }
//            }
//        }
//        service.subscribe(subs)
//
//        GlobalScope.launch {
//
//            service.sendIntent(IntentMessage(ActionOneIntentMsg("ANY 1")))
//        }
//

    }
}