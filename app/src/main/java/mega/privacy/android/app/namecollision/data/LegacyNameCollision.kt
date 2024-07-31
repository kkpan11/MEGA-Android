package mega.privacy.android.app.namecollision.data

import mega.privacy.android.app.ShareInfo
import mega.privacy.android.domain.entity.node.FileNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.uri.UriPath
import nz.mega.sdk.MegaNode
import java.io.File
import java.io.Serializable

/**
 * Name collision
 * @property collisionHandle
 * @property name
 * @property size
 * @property childFolderCount
 * @property childFileCount
 * @property lastModified
 * @property parentHandle
 * @property isFile
 * @constructor Create empty Name collision
 */
@Deprecated("Use domain's NodeNameCollision")
sealed class LegacyNameCollision : Serializable {
    abstract val collisionHandle: Long
    abstract val name: String
    abstract val size: Long?
    abstract val childFolderCount: Int
    abstract val childFileCount: Int
    abstract val lastModified: Long
    abstract val parentHandle: Long?
    abstract val isFile: Boolean

    /**
     * Upload
     *
     * @property collisionHandle
     * @property absolutePath
     * @property name
     * @property size
     * @property childFolderCount
     * @property childFileCount
     * @property lastModified
     * @property parentHandle
     * @property isFile
     * @constructor Create empty Upload
     */
    data class Upload constructor(
        override val collisionHandle: Long,
        val absolutePath: String,
        override val name: String,
        override val size: Long? = null,
        override val childFolderCount: Int = 0,
        override val childFileCount: Int = 0,
        override val lastModified: Long,
        override val parentHandle: Long?,
        override val isFile: Boolean = true,
    ) : LegacyNameCollision() {

        companion object {

            /**
             * Gets a [LegacyNameCollision.Upload] from a [File].
             *
             * @param collisionHandle   The node handle with which there is a name collision.
             * @param file              The file from which the [LegacyNameCollision.Upload] will be get.
             * @param parentHandle      The parent handle of the node in which the file has to be uploaded.
             */
            @JvmStatic
            fun getUploadCollision(
                collisionHandle: Long,
                file: File,
                parentHandle: Long?,
            ): Upload =
                Upload(
                    collisionHandle = collisionHandle,
                    absolutePath = file.absolutePath,
                    name = file.name,
                    size = if (file.isFile) file.length() else null,
                    childFolderCount = file.listFiles()?.count { it.isDirectory } ?: 0,
                    childFileCount = file.listFiles()?.count { it.isFile } ?: 0,
                    lastModified = file.lastModified(),
                    parentHandle = parentHandle
                )

            /**
             * Gets a [LegacyNameCollision.Upload] from a [ShareInfo].
             *
             * @param collisionHandle   The node handle with which there is a name collision.
             * @param shareInfo         The file from which the [LegacyNameCollision.Upload] will be get.
             * @param parentHandle      The parent handle of the node in which the file has to be uploaded.
             */
            @JvmStatic
            fun getUploadCollision(
                collisionHandle: Long,
                shareInfo: ShareInfo,
                parentHandle: Long,
            ): Upload = Upload(
                collisionHandle = collisionHandle,
                absolutePath = shareInfo.fileAbsolutePath,
                name = shareInfo.originalFileName,
                size = shareInfo.size,
                lastModified = shareInfo.lastModified,
                parentHandle = parentHandle
            )

            /**
             * Get upload collision
             *
             * @param collision The [FileNameCollision] from which the [LegacyNameCollision.Upload] will be get.
             * @return
             */
            @JvmStatic
            fun getUploadCollision(
                collision: FileNameCollision,
            ): Upload = Upload(
                collisionHandle = collision.collisionHandle,
                absolutePath = collision.path.value,
                name = collision.name,
                size = if (collision.isFile) collision.size else null,
                childFolderCount = collision.childFolderCount,
                childFileCount = collision.childFileCount,
                lastModified = collision.lastModified,
                parentHandle = collision.parentHandle,
                isFile = collision.isFile
            )
        }
    }

    /**
     * Copy
     *
     * @property collisionHandle
     * @property nodeHandle
     * @property name
     * @property size
     * @property childFolderCount
     * @property childFileCount
     * @property lastModified
     * @property parentHandle
     * @property isFile
     * @property serializedNode
     * @constructor Create empty Copy
     */
    data class Copy constructor(
        override val collisionHandle: Long,
        val nodeHandle: Long,
        override val name: String,
        override val size: Long? = null,
        override val childFolderCount: Int = 0,
        override val childFileCount: Int = 0,
        override val lastModified: Long,
        override val parentHandle: Long,
        override val isFile: Boolean,
        val serializedNode: String? = null,
    ) : LegacyNameCollision() {

        companion object {

            /**
             * Gets a [LegacyNameCollision.Copy] from a [MegaNode].
             *
             * @param collisionHandle   The node handle with which there is a name collision.
             * @param node              The node from which the [LegacyNameCollision.Copy] will be get.
             * @param parentHandle      The parent handle of the node in which the file has to be copied.
             */
            @JvmStatic
            fun fromNodeNameCollision(
                collisionHandle: Long,
                node: MegaNode,
                parentHandle: Long,
                childFolderCount: Int,
                childFileCount: Int,
            ): Copy =
                Copy(
                    collisionHandle = collisionHandle,
                    nodeHandle = node.handle,
                    name = node.name,
                    size = if (node.isFile) node.size else null,
                    childFolderCount = childFolderCount,
                    childFileCount = childFileCount,
                    lastModified = if (node.isFile) node.modificationTime else node.creationTime,
                    parentHandle = parentHandle,
                    isFile = node.isFile,
                    serializedNode = node.serialize(),
                )

            /**
             * Temporary mapper to create get [LegacyNameCollision] from domain module's [NodeNameCollision]
             * Should be removed when [GetNameCollisionResultUseCase] is refactored
             */
            @JvmStatic
            fun fromNodeNameCollision(
                nameCollision: NodeNameCollision,
            ): Copy = Copy(
                collisionHandle = nameCollision.collisionHandle,
                nodeHandle = nameCollision.nodeHandle,
                name = nameCollision.name,
                size = nameCollision.size,
                childFolderCount = nameCollision.childFolderCount,
                childFileCount = nameCollision.childFileCount,
                lastModified = nameCollision.lastModified,
                parentHandle = nameCollision.parentHandle,
                isFile = nameCollision.isFile,
                serializedNode = nameCollision.serializedData
            )
        }
    }

    /**
     * Import
     *
     * @property collisionHandle
     * @property nodeHandle
     * @property chatId
     * @property messageId
     * @property name
     * @property size
     * @property childFolderCount
     * @property childFileCount
     * @property lastModified
     * @property parentHandle
     * @property isFile
     * @constructor Create empty Import
     */
    data class Import constructor(
        override val collisionHandle: Long,
        val nodeHandle: Long,
        val chatId: Long,
        val messageId: Long,
        override val name: String,
        override val size: Long? = null,
        override val childFolderCount: Int = 0,
        override val childFileCount: Int = 0,
        override val lastModified: Long,
        override val parentHandle: Long,
        override val isFile: Boolean = true,
    ) : LegacyNameCollision() {

        companion object {

            /**
             * Gets a [LegacyNameCollision.Import] from a [MegaNode].
             *
             * @param collisionHandle   The node handle with which there is a name collision.
             * @property nodeHandle     The node handle of the node to import.
             * @property chatId         The chat identifier where is the node to import.
             * @property messageId      The message identifier where is the node to import.
             * @param node              The node from which the [LegacyNameCollision.Import] will be get.
             * @param parentHandle      The parent handle of the node in which the file has to be copied.
             */
            @JvmStatic
            fun getImportCollision(
                collisionHandle: Long,
                chatId: Long,
                messageId: Long,
                node: MegaNode,
                parentHandle: Long,
            ): Import =
                Import(
                    collisionHandle = collisionHandle,
                    nodeHandle = node.handle,
                    chatId = chatId,
                    messageId = messageId,
                    name = node.name,
                    size = node.size,
                    lastModified = node.modificationTime,
                    parentHandle = parentHandle
                )

            /**
             * Temporary mapper to create a [LegacyNameCollision] from domain module's [NodeNameCollision]
             * Should be removed when [GetNameCollisionResultUseCase] is refactored
             */
            @JvmStatic
            fun fromNodeNameCollision(
                nameCollision: NodeNameCollision.Chat,
            ): Import = Import(
                collisionHandle = nameCollision.collisionHandle,
                nodeHandle = nameCollision.nodeHandle,
                chatId = nameCollision.chatId,
                messageId = nameCollision.messageId,
                name = nameCollision.name,
                size = nameCollision.size,
                childFolderCount = nameCollision.childFolderCount,
                childFileCount = nameCollision.childFileCount,
                lastModified = nameCollision.lastModified,
                parentHandle = nameCollision.parentHandle,
                isFile = nameCollision.isFile
            )
        }
    }

    /**
     * Movement
     *
     * @property collisionHandle
     * @property nodeHandle
     * @property name
     * @property size
     * @property childFolderCount
     * @property childFileCount
     * @property lastModified
     * @property parentHandle
     * @property isFile
     * @constructor Create empty Movement
     */
    data class Movement constructor(
        override val collisionHandle: Long,
        val nodeHandle: Long,
        override val name: String,
        override val size: Long? = null,
        override val childFolderCount: Int = 0,
        override val childFileCount: Int = 0,
        override val lastModified: Long,
        override val parentHandle: Long,
        override val isFile: Boolean,
    ) : LegacyNameCollision() {

        companion object {

            /**
             * Gets a [LegacyNameCollision.Movement] from a [MegaNode].
             *
             * @param collisionHandle   The node handle with which there is a name collision.
             * @param node              The node from which the [LegacyNameCollision.Movement] will be get.
             * @param parentHandle      The parent handle of the node in which the file has to be moved.
             */
            @JvmStatic
            fun fromNodeNameCollision(
                collisionHandle: Long,
                node: MegaNode,
                parentHandle: Long,
                childFolderCount: Int,
                childFileCount: Int,
            ): Movement = Movement(
                collisionHandle = collisionHandle,
                nodeHandle = node.handle,
                name = node.name,
                size = if (node.isFile) node.size else null,
                childFolderCount = childFolderCount,
                childFileCount = childFileCount,
                lastModified = if (node.isFile) node.modificationTime else node.creationTime,
                parentHandle = parentHandle,
                isFile = node.isFile
            )

            /**
             * Temporary mapper to create a [LegacyNameCollision] from domain module's [NodeNameCollision]
             * Should be removed when [GetNameCollisionResultUseCase] is refactored
             */
            @JvmStatic
            fun fromNodeNameCollision(
                nameCollision: NodeNameCollision,
            ): Movement = Movement(
                collisionHandle = nameCollision.collisionHandle,
                nodeHandle = nameCollision.nodeHandle,
                name = nameCollision.name,
                size = nameCollision.size,
                childFolderCount = nameCollision.childFolderCount,
                childFileCount = nameCollision.childFileCount,
                lastModified = nameCollision.lastModified,
                parentHandle = nameCollision.parentHandle,
                isFile = nameCollision.isFile
            )
        }
    }
}

/**
 * Temporary mapper to create a [LegacyNameCollision.Import] from domain module's [NodeNameCollision]
 * Should be removed when [GetNameCollisionResultUseCase] is refactored
 */
fun NodeNameCollision.Chat.toLegacyImport() = LegacyNameCollision.Import.fromNodeNameCollision(this)

/**
 * Temporary mapper to create a [LegacyNameCollision.Movement] from domain module's [NodeNameCollision]
 * Should be removed when [GetNameCollisionResultUseCase] is refactored
 */
fun NodeNameCollision.toLegacyMove() = LegacyNameCollision.Movement.fromNodeNameCollision(this)

/**
 * Temporary mapper to create a [LegacyNameCollision.Copy] from domain module's [NodeNameCollision]
 * Should be removed when [GetNameCollisionResultUseCase] is refactored
 */
fun NodeNameCollision.toLegacyCopy() = LegacyNameCollision.Copy.fromNodeNameCollision(this)


/**
 * Temporary mapper to create a domain module's [mega.privacy.android.domain.entity.node.NameCollision] from app module's [LegacyNameCollision]
 * Should be removed when all features are refactored
 */
fun LegacyNameCollision.toDomainEntity(): mega.privacy.android.domain.entity.node.NameCollision =
    when (this) {
        is LegacyNameCollision.Copy -> NodeNameCollision.Default(
            collisionHandle = collisionHandle,
            nodeHandle = nodeHandle,
            name = name,
            size = size ?: -1L,
            childFolderCount = childFileCount,
            childFileCount = childFileCount,
            lastModified = lastModified,
            parentHandle = parentHandle,
            isFile = isFile,
            serializedData = null,
            renameName = null,
            type = NodeNameCollisionType.COPY
        )

        is LegacyNameCollision.Movement -> NodeNameCollision.Default(
            collisionHandle = collisionHandle,
            nodeHandle = nodeHandle,
            name = name,
            size = size ?: -1L,
            childFolderCount = childFileCount,
            childFileCount = childFileCount,
            lastModified = lastModified,
            parentHandle = parentHandle,
            isFile = isFile,
            serializedData = null,
            renameName = null,
            type = NodeNameCollisionType.MOVE
        )

        is LegacyNameCollision.Import -> NodeNameCollision.Chat(
            collisionHandle = collisionHandle,
            nodeHandle = nodeHandle,
            name = name,
            size = size ?: -1L,
            childFolderCount = childFileCount,
            childFileCount = childFileCount,
            lastModified = lastModified,
            parentHandle = parentHandle,
            isFile = isFile,
            serializedData = null,
            renameName = null,
            chatId = chatId,
            messageId = messageId
        )

        is LegacyNameCollision.Upload -> FileNameCollision(
            collisionHandle = collisionHandle,
            name = name,
            size = size ?: 0L,
            childFolderCount = childFileCount,
            childFileCount = childFileCount,
            lastModified = lastModified,
            parentHandle = parentHandle ?: -1L,
            isFile = isFile,
            path = UriPath(absolutePath)
        )
    }