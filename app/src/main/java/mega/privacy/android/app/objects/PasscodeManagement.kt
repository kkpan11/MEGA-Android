package mega.privacy.android.app.objects

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.passcode.MonitorPasscodeLockStateUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Class which manages passcode lock behaviours.
 *
 * @property lastPause          Latest timestamp when an activity was paused.
 * @property showPasscodeScreen True if should show passcode lock, false otherwise.
 * @property needsOpenAgain     True if passcode lock should be opened again, false otherwise.
 */
@Singleton
class PasscodeManagement @Inject constructor(
    monitorPasscodeLockStateUseCase: MonitorPasscodeLockStateUseCase,
    @ApplicationScope applicationScope: CoroutineScope,
) {
    /**
     * State flow to monitor passcode lock state.
     */
    val shouldLock: StateFlow<Boolean> = monitorPasscodeLockStateUseCase()
        .stateIn(applicationScope, SharingStarted.Eagerly, false)

    var lastPause: Long = 0
    var showPasscodeScreen: Boolean = true
    var needsOpenAgain: Boolean = false

    fun resetDefaults() {
        lastPause = 0
        showPasscodeScreen = true
        needsOpenAgain = false
    }
}