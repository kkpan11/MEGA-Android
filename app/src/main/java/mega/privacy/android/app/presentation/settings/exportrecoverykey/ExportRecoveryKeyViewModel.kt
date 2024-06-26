package mega.privacy.android.app.presentation.settings.exportrecoverykey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.settings.exportrecoverykey.model.RecoveryKeyUIState
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetExportMasterKeyUseCase
import mega.privacy.android.domain.usecase.SetMasterKeyExportedUseCase
import mega.privacy.android.domain.usecase.account.GetPrintRecoveryKeyFileUseCase
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * View model for [ExportRecoveryKeyActivity]
 * @see [ExportRecoveryKeyActivity]
 */
@HiltViewModel
class ExportRecoveryKeyViewModel @Inject constructor(
    private val getExportMasterKeyUseCase: GetExportMasterKeyUseCase,
    private val setMasterKeyExportedUseCase: SetMasterKeyExportedUseCase,
    private val getPrintRecoveryKeyFileUseCase: GetPrintRecoveryKeyFileUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RecoveryKeyUIState())

    /**
     * Flow of [ExportRecoveryKeyActivity] UI State
     * @see ExportRecoveryKeyActivity
     * @see RecoveryKeyUIState
     */
    val uiState = _uiState.asStateFlow()

    /**
     * Exports the Recovery Key
     */
    suspend fun getRecoveryKey(): String? {
        return getExportMasterKeyUseCase().also { key ->
            if (key.isNullOrBlank().not()) {
                setMasterKeyExportedUseCase()
            }
        }
    }

    /**
     * Set Action Button Group to Vertical Orientation
     */
    fun setActionGroupVertical() = viewModelScope.launch {
        _uiState.update { it.copy(isActionGroupVertical = true) }
    }

    /**
     * Action to trigger showing SnackBar in Compose View
     */
    fun showSnackBar(message: String) = viewModelScope.launch {
        _uiState.update { it.copy(message = message) }
    }

    /**
     * Action to mark that SnackBar has been shown
     */
    fun setSnackBarShown() = viewModelScope.launch {
        _uiState.update { it.copy(message = null) }
    }


    /**
     * Print recovery key
     */
    fun printRecoveryKey() = viewModelScope.launch {
        val file = runCatching { getPrintRecoveryKeyFileUseCase() }
            .onFailure { Timber.e(it) }
            .getOrNull()
        _uiState.update { it.copy(printRecoveryKey = triggered(file)) }
    }

    /**
     * Delete the temp recovery key on print complete
     */
    fun onPrintRecoveryKeyCompleted(file: File) {
        viewModelScope.launch(ioDispatcher) {
            file.delete()
        }
    }

    /**
     * Reset and notify printRk is consumed
     */
    fun resetPrintRecoveryKey() = _uiState.update { it.copy(printRecoveryKey = consumed()) }

}