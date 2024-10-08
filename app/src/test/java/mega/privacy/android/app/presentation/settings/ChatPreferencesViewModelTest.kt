package mega.privacy.android.app.presentation.settings

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.activities.settingsActivities.ChatPreferencesViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.MonitorChatSignalPresenceUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class ChatPreferencesViewModelTest {

    private lateinit var underTest: ChatPreferencesViewModel

    //Mocks
    private val monitorUpdatePushNotificationSettingsUseCase =
        mock<MonitorUpdatePushNotificationSettingsUseCase> {
            onBlocking { invoke() }.thenReturn(flowOf(true))
        }

    private val monitorChatSignalPresenceUseCase =
        mock<MonitorChatSignalPresenceUseCase> {
            onBlocking { invoke() }.thenReturn(flowOf(Unit))
        }

    @BeforeEach
    fun setUp() {
        underTest = ChatPreferencesViewModel(
            monitorUpdatePushNotificationSettingsUseCase = monitorUpdatePushNotificationSettingsUseCase,
            monitorChatSignalPresenceUseCase = monitorChatSignalPresenceUseCase,
        )
    }

    @Test
    fun `test that when push notification settings is updated state is also updated`() =
        runTest {
            testScheduler.advanceUntilIdle()
            verify(monitorUpdatePushNotificationSettingsUseCase).invoke()
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.isPushNotificationSettingsUpdatedEvent).isTrue()
            }
        }

    @Test
    fun `test that when onConsumePushNotificationSettingsUpdateEvent is called then state is also updated`() =
        runTest {
            testScheduler.advanceUntilIdle()
            verify(monitorUpdatePushNotificationSettingsUseCase).invoke()
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.isPushNotificationSettingsUpdatedEvent).isTrue()
                underTest.onConsumePushNotificationSettingsUpdateEvent()
                val updatedState = awaitItem()
                Truth.assertThat(updatedState.isPushNotificationSettingsUpdatedEvent).isFalse()
            }
        }

    @Test
    fun `test that when chat signal presence is updated state is also updated`() =
        runTest {
            testScheduler.advanceUntilIdle()
            verify(monitorChatSignalPresenceUseCase).invoke()
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.signalPresenceUpdate).isTrue()
            }
        }

    @Test
    fun `test that when onSignalPresenceUpdateConsumed is called then state is also updated`() =
        runTest {
            testScheduler.advanceUntilIdle()
            verify(monitorChatSignalPresenceUseCase).invoke()
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.signalPresenceUpdate).isTrue()
                underTest.onSignalPresenceUpdateConsumed()
                val updatedState = awaitItem()
                Truth.assertThat(updatedState.signalPresenceUpdate).isFalse()
            }
        }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}