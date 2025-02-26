package mega.privacy.android.domain.usecase.transfers.pending

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.InsertPendingTransferRequest
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferNodeIdentifier
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.NotEnoughStorageException
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.file.DoesUriPathHaveSufficientSpaceForNodesUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.DestinationAndAppDataForDownloadResult
import mega.privacy.android.domain.usecase.transfers.downloads.GetFileDestinationAndAppDataForDownloadUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InsertPendingDownloadsForNodesUseCaseTest {
    private lateinit var underTest: InsertPendingDownloadsForNodesUseCase

    private val transferRepository = mock<TransferRepository>()
    private val getPendingTransferNodeIdentifierUseCase =
        mock<GetPendingTransferNodeIdentifierUseCase>()
    private val doesUriPathHaveSufficientSpaceForNodesUseCase =
        mock<DoesUriPathHaveSufficientSpaceForNodesUseCase>()
    private val getFileDestinationAndAppDataForDownloadUseCase =
        mock<GetFileDestinationAndAppDataForDownloadUseCase>()
    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setUp() {
        underTest = InsertPendingDownloadsForNodesUseCase(
            transferRepository,
            getPendingTransferNodeIdentifierUseCase,
            doesUriPathHaveSufficientSpaceForNodesUseCase,
            getFileDestinationAndAppDataForDownloadUseCase,
            fileSystemRepository,
        )
    }

    @BeforeEach
    fun cleanUp() = runTest {
        reset(
            transferRepository,
            getPendingTransferNodeIdentifierUseCase,
            doesUriPathHaveSufficientSpaceForNodesUseCase,
            getFileDestinationAndAppDataForDownloadUseCase,
            fileSystemRepository,
        )
        val nodeIdentifier = PendingTransferNodeIdentifier.CloudDriveNode(NodeId(647L))
        whenever(getPendingTransferNodeIdentifierUseCase(anyOrNull())) doReturn nodeIdentifier
        whenever(getFileDestinationAndAppDataForDownloadUseCase(UriPath(PATH_STRING))) doReturn
                DestinationAndAppDataForDownloadResult(UriPath(PATH_STRING), null)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that an InsertPendingTransferRequest is sent to transferRepository's insertPendingTransfers with the correct parameters for each node`(
        isHighPriority: Boolean,
    ) = runTest {
        val nodes = (0..5).map {
            mock<DefaultTypedFileNode>()
        }
        val uriPath = UriPath(PATH_STRING)
        val expected = nodes.mapIndexed { index, node ->
            val nodeIdentifier =
                PendingTransferNodeIdentifier.CloudDriveNode(NodeId(index.toLong()))
            whenever(getPendingTransferNodeIdentifierUseCase(node)) doReturn nodeIdentifier
            whenever(
                doesUriPathHaveSufficientSpaceForNodesUseCase(uriPath, nodes)
            ) doReturn true
            InsertPendingTransferRequest(
                transferType = TransferType.DOWNLOAD,
                nodeIdentifier = nodeIdentifier,
                path = uriPath.value,
                appData = null,
                isHighPriority = isHighPriority,
            )
        }

        underTest(nodes, uriPath, isHighPriority)

        verify(transferRepository).insertPendingTransfers(expected)
    }

    @Test
    fun `test that a NotEnoughStorageException is thrown when doesPathHaveSufficientSpaceForNodesUseCase returns false`() =
        runTest {
            val node = mock<DefaultTypedFileNode>()
            val destination = UriPath(PATH_STRING)
            whenever(doesUriPathHaveSufficientSpaceForNodesUseCase(destination, listOf(node))) doReturn
                    false

            assertThrows<NotEnoughStorageException> {
                underTest(listOf(node), destination, false)
            }
        }

    @Test
    fun `test that an exception thrown by getDestinationForSdkAndAppDataForDownloadUseCase is propagated`() =
        runTest {
            val node = mock<DefaultTypedFileNode>()
            val destination = PATH_STRING
            val expected = RuntimeException()
            whenever(getFileDestinationAndAppDataForDownloadUseCase(UriPath(destination))) doThrow expected

            val actual = runCatching {
                underTest(listOf(node), UriPath(destination), false)
            }.exceptionOrNull()

            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test that folder is created`() = runTest {
        val uriPath = UriPath(PATH_STRING)
        val nodes = listOf(mock<DefaultTypedFileNode>())
        whenever(
            doesUriPathHaveSufficientSpaceForNodesUseCase(uriPath, nodes)
        ) doReturn true

        underTest(nodes, uriPath, false)

        verify(fileSystemRepository).createDirectory(PATH_STRING)
    }

    companion object {
        private const val PATH_STRING = "uriPath/"
    }
}