package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

/**
 * Test class for [SetLocationTagsEnabledUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetLocationTagsEnabledUseCaseTest {

    private lateinit var underTest: SetLocationTagsEnabledUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetLocationTagsEnabledUseCase(
            cameraUploadsRepository = cameraUploadsRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadsRepository)
    }

    @ParameterizedTest(name = "location tags enabled: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that location tags are handled when uploading photos`(locationTagsEnabled: Boolean) =
        runTest {
            underTest(locationTagsEnabled)

            verify(cameraUploadsRepository).setLocationTagsEnabled(locationTagsEnabled)
        }
}