package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RenameNodeUseCaseTest {

    private lateinit var underTest: RenameNodeUseCase
    private val cameraUploadsRepository: CameraUploadsRepository = mock()

    @BeforeAll
    fun setUp() {
        underTest = RenameNodeUseCase(cameraUploadsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadsRepository)
    }

    @Test
    fun `test that when nodeRepository's renameNode returns an empty list then result returns correctly`() =
        runTest {
            val nodeHandle = 1L
            val newName = "newName"

            underTest(nodeHandle, newName)

            verify(cameraUploadsRepository).renameNode(nodeHandle, newName)
        }
}
