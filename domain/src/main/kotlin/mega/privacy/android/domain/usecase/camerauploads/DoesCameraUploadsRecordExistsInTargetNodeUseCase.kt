package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.node.FindNodeWithFingerprintInParentNodeUseCase
import javax.inject.Inject

/**
 * Check if the record has already been updated in the target node or exists in a different folder,
 * except rubbish bin and set property `existsInTargetNode` and `existingNodeId` to the [CameraUploadsRecord]
 */
class DoesCameraUploadsRecordExistsInTargetNodeUseCase @Inject constructor(
    private val findNodeWithFingerprintInParentNodeUseCase: FindNodeWithFingerprintInParentNodeUseCase,
) {

    /**
     * Check if the record has already been updated in the target node or exists in a different folder,
     * except rubbish bin and set property `existsInTargetNode` and `existingNodeId` to the [CameraUploadsRecord]
     *
     * @param recordList
     * @param primaryUploadNodeId
     * @param secondaryUploadNodeId
     * @return a list of [CameraUploadsRecord]
     */
    suspend operator fun invoke(
        recordList: List<CameraUploadsRecord>,
        primaryUploadNodeId: NodeId,
        secondaryUploadNodeId: NodeId,
    ): List<CameraUploadsRecord> =
        recordList.mapNotNull { record ->
            runCatching {
                findNodeWithFingerprintInParentNodeUseCase(
                    record.originalFingerprint,
                    record.generatedFingerprint,
                    when (record.folderType) {
                        CameraUploadFolderType.Primary -> primaryUploadNodeId
                        CameraUploadFolderType.Secondary -> secondaryUploadNodeId
                    },
                )
            }.getOrNull()?.let { (existsInTargetNode, existingNodeId) ->
                record.copy(
                    existsInTargetNode = existsInTargetNode,
                    existingNodeId = existingNodeId,
                )
            }
        }
}
