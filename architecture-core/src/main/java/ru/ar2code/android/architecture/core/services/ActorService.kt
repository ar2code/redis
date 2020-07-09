package ru.ar2code.android.architecture.core.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.ar2code.android.architecture.core.models.IntentMessage
import ru.ar2code.android.architecture.core.models.ServiceResult

@ExperimentalCoroutinesApi
abstract class ActorService<TResult> where TResult : Any {

    private lateinit var scope: CoroutineScope

    protected var serviceState: ActorServiceState = ActorServiceState.Created()

    private var outerResultChannel = BroadcastChannel<ServiceResult<TResult>>(Channel.BUFFERED)

    private var intentMsgChannel = Channel<IntentMessage>(Channel.UNLIMITED)

    protected abstract suspend fun onIntentMsg(msg: IntentMessage)

    @Synchronized
    fun initialize(scope: CoroutineScope) {

        fun initService() {
            this.scope = scope
            subscribeToIntentMessages()
            serviceState = ActorServiceState.Initiated()
        }

        when (serviceState) {
            is ActorServiceState.Created -> {
                initService()
            }
            is ActorServiceState.Disposed -> {
                throw IllegalStateException("ServiceActor is disposed. Cannot initialize again.")
            }
            else -> {
                return
            }
        }

    }

    fun sendIntent(msg: IntentMessage) {
        scope.launch {
            intentMsgChannel.send(msg)
        }
    }

    fun dispose() {
        serviceState = ActorServiceState.Disposed()

        intentMsgChannel.close()
        outerResultChannel.close()
    }

    fun subscribe(): ReceiveChannel<ServiceResult<TResult>> =
        outerResultChannel.openSubscription()

    fun isDisposed() = serviceState is ActorServiceState.Disposed

    private fun subscribeToIntentMessages() {
        scope.launch {
            while (!isDisposed() && isActive) {
                val msg = intentMsgChannel.receive()
                onIntentMsg(msg)
            }
        }
    }

    protected suspend fun provideResult(
        newServiceState: ActorServiceState,
        result: ServiceResult<TResult>
    ) {
        serviceState = newServiceState
        outerResultChannel.send(result)
    }
}