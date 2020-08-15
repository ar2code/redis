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
            Toast.makeText(this, it.name, Toast.LENGTH_LONG).show()
            textView.setText(it.name)
            Log.d(tag, "main activity viewStateLive observer $it")
        })

        viewModel.viewEventLive.observe(this, Observer {
            Toast.makeText(this, "event", Toast.LENGTH_LONG).show()
            Log.d(tag, "main activity viewEventLive observer $it")
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