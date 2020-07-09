package ru.ar2code.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import ru.ar2code.android.architecture.core.models.IntentMessage
import ru.ar2code.demo.impl.DemoService
import ru.ar2code.demo.impl.DemoUseCase

@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity() {

    val tag = "ROZHKOV"

    private val service = DemoService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        GlobalScope.launch {
            var i = 0
            while (true) {

                try {
                    Log.d(tag, "main activity await service result")

                    val result = service.subscribe()
                        .receive()

                    Log.d(tag, "main activity service result $i: ${result.payload}")
                    i++

                    delay(200)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

//        GlobalScope.launch {
//            repeat(500) {
//                service.sendIntent(IntentMessage("ANY"))
//                //delay(1000)
//            }
//            delay(10000)
//           // service.dispose()
//        }

    }
}