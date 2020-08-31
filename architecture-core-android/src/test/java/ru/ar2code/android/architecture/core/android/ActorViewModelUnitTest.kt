package ru.ar2code.android.architecture.core.android

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.Rule
import ru.ar2code.android.architecture.core.android.prepares.*
import ru.ar2code.android.architecture.core.models.IntentMessage
import ru.ar2code.android.architecture.core.services.ActorServiceState

class ActorViewModelUnitTest {

    private val delayBeforeAssertMs = 20L

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Test
    fun `ViewModel's state is created or initiated immediately after creation`() = runBlocking {
        val viewModel = TestViewModel()

        assertTrue(viewModel.state is ActorServiceState.Created || viewModel.state is ActorServiceState.Initiated)
    }

    @Test
    fun `ViewModel's state becomes initiated after creation with small delay`() = runBlocking {
        val viewModel = TestViewModel()

        delay(delayBeforeAssertMs)

        assertTrue(viewModel.state is ActorServiceState.Initiated)
    }

    @Test
    fun `Can change state set correct state after receiving results`() = runBlocking {
        val viewModel = TestViewModel()

        assertTrue(viewModel.state is ActorServiceState.Created || viewModel.state is ActorServiceState.Initiated)

        viewModel.sendIntent(IntentMessage(TestIntentMessageType()))

        delay(delayBeforeAssertMs)

        assertTrue(viewModel.state is TestViewModelInternalOkState)
    }

    @Test
    fun `Intent send new view state only and viewStateLive contains correct state`() = runBlocking {
        val viewModel = TestViewModelWithStateOnly()

        viewModel.viewStateLive.observeForever {}
        viewModel.viewEventLive.observeForever {}

        viewModel.sendIntent(IntentMessage(TestIntentMessageType()))

        delay(delayBeforeAssertMs)

        assertTrue(viewModel.viewStateLive.value is TestViewModelState)
        assertNull(viewModel.viewEventLive.value)
    }

    @Test
    fun `Intent send new view event only and viewEventLive contains correct event`() = runBlocking {
        val viewModel = TestViewModelWithEventOnly()

        viewModel.viewStateLive.observeForever {}
        viewModel.viewEventLive.observeForever {}

        viewModel.sendIntent(IntentMessage(TestIntentMessageType()))

        delay(delayBeforeAssertMs)

        assertTrue(viewModel.viewEventLive.value?.data is TestViewModelEvent)
        assertNull(viewModel.viewStateLive.value)
    }

    @Test
    fun `Intent send new view state and event and viewStateLive and viewEventLive contains correct data`() = runBlocking {
        val viewModel = TestViewModelWithStateAndEvent()

        viewModel.viewStateLive.observeForever {}
        viewModel.viewEventLive.observeForever {}

        viewModel.sendIntent(IntentMessage(TestIntentMessageType()))

        delay(delayBeforeAssertMs)

        assertTrue(viewModel.viewEventLive.value?.data is TestViewModelEvent)
        assertTrue(viewModel.viewStateLive.value is TestViewModelState)
    }
}