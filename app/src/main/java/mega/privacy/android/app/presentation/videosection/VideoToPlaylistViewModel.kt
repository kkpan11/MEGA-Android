package mega.privacy.android.app.presentation.videosection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.videosection.mapper.VideoPlaylistSetUiEntityMapper
import mega.privacy.android.app.presentation.videosection.model.VideoToPlaylistUiState
import mega.privacy.android.domain.usecase.photos.GetNextDefaultAlbumNameUseCase
import mega.privacy.android.domain.usecase.videosection.CreateVideoPlaylistUseCase
import mega.privacy.android.domain.usecase.videosection.GetVideoPlaylistSetsUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Video playlist view model
 */
@HiltViewModel
class VideoToPlaylistViewModel @Inject constructor(
    private val getVideoPlaylistSetsUseCase: GetVideoPlaylistSetsUseCase,
    private val createVideoPlaylistUseCase: CreateVideoPlaylistUseCase,
    private val getNextDefaultAlbumNameUseCase: GetNextDefaultAlbumNameUseCase,
    private val videoPlaylistSetUiEntityMapper: VideoPlaylistSetUiEntityMapper,
) : ViewModel() {
    private val _uiState = MutableStateFlow(VideoToPlaylistUiState())
    internal val uiState = _uiState.asStateFlow()

    private var createVideoPlaylistJob: Job? = null

    init {
        loadVideoPlaylistSets()
    }

    private fun loadVideoPlaylistSets() {
        viewModelScope.launch {
            runCatching {
                getVideoPlaylistSetsUseCase()
            }.onSuccess { sets ->
                val newSets = sets.map {
                    videoPlaylistSetUiEntityMapper(it)
                }
                _uiState.update {
                    it.copy(
                        items = newSets,
                        isLoading = false
                    )
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Create new video playlist
     *
     * @param title video playlist title
     */
    internal fun createNewPlaylist(title: String) {
        if (createVideoPlaylistJob?.isActive == true) return
        title.ifEmpty {
            _uiState.value.createVideoPlaylistPlaceholderTitle
        }.trim()
            .takeIf { it.isNotEmpty() && checkVideoPlaylistTitleValidity(it) }
            ?.let { playlistTitle ->
                createVideoPlaylistJob = viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = true) }
                    setShouldCreateVideoPlaylist(false)
                    runCatching {
                        createVideoPlaylistUseCase(playlistTitle)
                    }.onSuccess { videoPlaylist ->
                        _uiState.update {
                            it.copy(
                                isVideoPlaylistCreatedSuccessfully = true
                            )
                        }
                        loadVideoPlaylistSets()
                        Timber.d("Created video playlist: ${videoPlaylist.title}")
                    }.onFailure { exception ->
                        Timber.e(exception)
                        _uiState.update {
                            it.copy(
                                isVideoPlaylistCreatedSuccessfully = false,
                                isLoading = false
                            )
                        }
                    }
                }
            }
    }

    private fun checkVideoPlaylistTitleValidity(
        title: String,
    ): Boolean {
        var errorMessage: Int? = null
        var isTitleValid = true

        if (title.isBlank()) {
            isTitleValid = false
            errorMessage = R.string.invalid_string
        } else if (title in getAllVideoPlaylistTitles()) {
            isTitleValid = false
            errorMessage = ERROR_MESSAGE_REPEATED_TITLE
        } else if ("[\\\\*/:<>?\"|]".toRegex().containsMatchIn(title)) {
            isTitleValid = false
            errorMessage = R.string.invalid_characters_defined
        }

        _uiState.update {
            it.copy(
                isInputTitleValid = isTitleValid,
                createDialogErrorMessage = errorMessage
            )
        }

        return isTitleValid
    }

    private fun getAllVideoPlaylistTitles() = _uiState.value.items.map { it.title }

    internal fun setShouldCreateVideoPlaylist(value: Boolean) = _uiState.update {
        it.copy(shouldCreateVideoPlaylist = value)
    }

    internal fun setNewPlaylistTitleValidity(valid: Boolean) = _uiState.update {
        it.copy(isInputTitleValid = valid)
    }

    internal fun setPlaceholderTitle(placeholderTitle: String) {
        val playlistTitles = getAllVideoPlaylistTitles()
        _uiState.update {
            it.copy(
                createVideoPlaylistPlaceholderTitle = getNextDefaultAlbumNameUseCase(
                    defaultName = placeholderTitle,
                    currentNames = playlistTitles
                )
            )
        }
    }

    companion object {
        internal const val ERROR_MESSAGE_REPEATED_TITLE = 0
    }
}