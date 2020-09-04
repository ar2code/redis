package ru.ar2code.demo.impl

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import org.koin.core.context.startKoin
import org.koin.dsl.module

val module = module {
    single { (scope: CoroutineScope) -> DemoService(scope) as AbstractDemoService }
}

class AndroidApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            modules(module)
        }

    }

}