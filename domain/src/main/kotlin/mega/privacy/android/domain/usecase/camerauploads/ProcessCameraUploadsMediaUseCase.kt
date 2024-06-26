package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import mega.privacy.android.domain.entity.CameraUploadsRecordType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import javax.inject.Inject

/**
 * Use case to retrieve media from the media stores, and save them in the database
 * to be uploaded by Camera Uploads
 *
 * @property getPrimaryFolderPathUseCase
 * @property getSecondaryFolderPathUseCase
 * @property getMediaStoreFileTypesUseCase
 * @property isMediaUploadsEnabledUseCase
 * @property retrieveMediaFromMediaStoreUseCase
 */
class ProcessCameraUploadsMediaUseCase @Inject constructor(
    private val getPrimaryFolderPathUseCase: GetPrimaryFolderPathUseCase,
    private val getSecondaryFolderPathUseCase: GetSecondaryFolderPathUseCase,
    private val getMediaStoreFileTypesUseCase: GetMediaStoreFileTypesUseCase,
    private val isMediaUploadsEnabledUseCase: IsMediaUploadsEnabledUseCase,
    private val retrieveMediaFromMediaStoreUseCase: RetrieveMediaFromMediaStoreUseCase,
    private val saveCameraUploadsRecordUseCase: SaveCameraUploadsRecordUseCase,
) {

    /**
     * Invoke
     * @param tempRoot [String]
     */
    suspend operator fun invoke(
        tempRoot: String,
    ) = coroutineScope {
        val (photoMediaStoreTypes, videoMediaStoreTypes) = getMediaStoreFileTypesUseCase().partition { it.isImageFileType() }
        val primaryFolderPath = getPrimaryFolderPathUseCase()

        val primaryPhotoMedia = async {
            photoMediaStoreTypes.takeUnless { it.isEmpty() }?.let {
                retrieveMediaFromMediaStoreUseCase(
                    parentPath = primaryFolderPath,
                    types = it,
                    folderType = CameraUploadFolderType.Primary,
                    fileType = CameraUploadsRecordType.TYPE_PHOTO,
                    tempRoot = tempRoot,
                )
            } ?: emptyList()
        }

        val primaryVideoMedia = async {
            videoMediaStoreTypes.takeUnless { it.isEmpty() }?.let {
                retrieveMediaFromMediaStoreUseCase(
                    parentPath = primaryFolderPath,
                    types = it,
                    folderType = CameraUploadFolderType.Primary,
                    fileType = CameraUploadsRecordType.TYPE_VIDEO,
                    tempRoot = tempRoot,
                )
            } ?: emptyList()
        }

        val isSecondaryFolderEnabled = isMediaUploadsEnabledUseCase()
        val secondaryFolderPath = getSecondaryFolderPathUseCase()

        val secondaryPhotoMedia =
            if (isSecondaryFolderEnabled) {
                async {
                    photoMediaStoreTypes.takeUnless { it.isEmpty() }?.let {
                        retrieveMediaFromMediaStoreUseCase(
                            parentPath = secondaryFolderPath,
                            types = it,
                            folderType = CameraUploadFolderType.Secondary,
                            fileType = CameraUploadsRecordType.TYPE_PHOTO,
                            tempRoot = tempRoot,
                        )
                    }.orEmpty()
                }
            } else null

        val secondaryVideoMedia =
            if (isSecondaryFolderEnabled) {
                async {
                    videoMediaStoreTypes.takeUnless { it.isEmpty() }?.let {
                        retrieveMediaFromMediaStoreUseCase(
                            parentPath = secondaryFolderPath,
                            types = it,
                            folderType = CameraUploadFolderType.Secondary,
                            fileType = CameraUploadsRecordType.TYPE_VIDEO,
                            tempRoot = tempRoot,
                        )
                    }.orEmpty()
                }
            } else null

        val combinedList = buildList {
            addAll(primaryPhotoMedia.await())
            addAll(primaryVideoMedia.await())
            secondaryPhotoMedia?.let { addAll(it.await()) }
            secondaryVideoMedia?.let { addAll(it.await()) }
        }
        saveCameraUploadsRecordUseCase(combinedList)
    }
}
