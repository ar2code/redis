package ru.ar2code.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
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

@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity() {

    val tag = "ROZHKOV"

    private val job = Job()
    private val service = DemoService(GlobalScope + job, Dispatchers.Default)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var i = 0
        service.subscribe(object : ServiceSubscriber<String> {
            override fun onReceive(result: ServiceResult<String>?) {
                Log.d(tag, "main activity service result $i: ${result?.payload}")
                i++

                GlobalScope.launch(Dispatchers.Main) {
                    text.text = i.toString()
                }
            }
        })

        GlobalScope.launch {

            repeat(100) {
                service.sendIntent(IntentMessage(ActionOneIntentMsg("ANY 1")))
            }

        }

        GlobalScope.launch {

            repeat(50) {
                service.sendIntent(IntentMessage(ActionOneIntentMsg("ANY 2")))
            }
            delay(4000)
            job.cancel()
            //service.dispose()

        }
     GlobalScope.launch {

            repeat(100) {
                service.sendIntent(IntentMessage(ActionOneIntentMsg("ANY 2")))
            }

        }

    }
}