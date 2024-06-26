package mega.privacy.android.domain.usecase.favourites

import mega.privacy.android.domain.entity.offline.BackupsOfflineNodeInformation
import mega.privacy.android.domain.entity.offline.IncomingShareOfflineNodeInformation
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.repository.FileSystemRepository
import java.io.File
import javax.inject.Inject

/**
 * Get offline file
 */
class GetOfflineFileUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * Invoke
     *
     * @param offlineInformation
     * @return the offline file
     */
    suspend operator fun invoke(offlineInformation: OfflineNodeInformation): File {
        return when (offlineInformation) {
            is BackupsOfflineNodeInformation -> getFile(
                fileSystemRepository.getOfflineBackupsPath(),
                offlineInformation.path,
                offlineInformation.name
            )

            is IncomingShareOfflineNodeInformation -> getFile(
                fileSystemRepository.getOfflinePath(),
                offlineInformation.incomingHandle,
                offlineInformation.path,
                offlineInformation.name
            )

            else -> getFile(
                fileSystemRepository.getOfflinePath(),
                offlineInformation.path,
                offlineInformation.name
            )
        }
    }

    private fun getFile(vararg paths: String) =
        File(paths.filterNot { it == File.separator }
            .joinToString(separator = File.separator))

}