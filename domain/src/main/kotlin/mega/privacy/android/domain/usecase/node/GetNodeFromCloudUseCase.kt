package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.AddNodeType
import javax.inject.Inject

/**
 * Check if there is a node with the same fingerprint in cloud drive in order to avoid uploading duplicate files
 *
 * NOTE: only looking for the node by original fingerprint is not enough,
 * because some old nodes do not have the attribute OriginalFingerprint,
 * in that case also look for the node by attribute Fingerprint
 */
class GetNodeFromCloudUseCase @Inject constructor(
    private val getNodeByOriginalFingerprintUseCase: GetNodeByOriginalFingerprintUseCase,
    private val getNodeByFingerprintAndParentNodeUseCase: GetNodeByFingerprintAndParentNodeUseCase,
    private val addNodeType: AddNodeType,
) {
    /**
     * @param originalFingerprint Fingerprint of the local file
     * @param generatedFingerprint Generated fingerprint of the local file
     *
     * @return A node with the same fingerprint, or null when not found
     * */
    suspend operator fun invoke(
        originalFingerprint: String,
        generatedFingerprint: String? = null,
        parentNodeId: NodeId,
    ): TypedFileNode? {
        // Try to find the node by original fingerprint in the account
        getNodeByOriginalFingerprintUseCase(originalFingerprint, null)?.let {
            return addNodeType(it) as? TypedFileNode
        }

        // Try to find the node by original fingerprint in the account
        getNodeByFingerprintAndParentNodeUseCase(originalFingerprint, parentNodeId)?.let {
            return addNodeType(it) as? TypedFileNode
        }

        generatedFingerprint?.let { fingerprint ->
            // Try to find the node by generated fingerprint in the account
            getNodeByFingerprintAndParentNodeUseCase(fingerprint, parentNodeId)?.let {
                return addNodeType(it) as? TypedFileNode
            }
        }

        // Node does not exist
        return null
    }
}
