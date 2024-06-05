package test.mega.privacy.android.app.presentation.zipbrowser

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.zipbrowser.ZipBrowserViewModel
import mega.privacy.android.app.presentation.zipbrowser.mapper.ZipInfoUiEntityMapper
import mega.privacy.android.app.utils.Constants.EXTRA_PATH_ZIP
import mega.privacy.android.domain.entity.zipbrowser.ZipTreeNode
import mega.privacy.android.domain.usecase.zipbrowser.GetZipTreeMapUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ZipBrowserViewModelTest {
    private lateinit var underTest: ZipBrowserViewModel

    private val getZipTreeMapUseCase = mock<GetZipTreeMapUseCase>()
    private val zipInfoUiEntityMapper = mock<ZipInfoUiEntityMapper>()
    private val savedStateHandle = mock<SavedStateHandle>()

    private val testDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    private val testZipFullPath = "/testZipFullPath.zip"

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = ZipBrowserViewModel(
            getZipTreeMapUseCase = getZipTreeMapUseCase,
            zipInfoUiEntityMapper = zipInfoUiEntityMapper,
            savedStateHandle = savedStateHandle
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getZipTreeMapUseCase,
            zipInfoUiEntityMapper,
            savedStateHandle
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that the initial state is returned`() = runTest {
        whenever(getZipTreeMapUseCase(anyOrNull())).thenReturn(emptyMap())
        whenever(savedStateHandle.get<String>(EXTRA_PATH_ZIP)).thenReturn(testZipFullPath)
        initUnderTest()

        underTest.uiState.test {
            val initial = awaitItem()
            assertThat(initial.items).isEmpty()
            assertThat(initial.folderDepth).isEqualTo(0)
            assertThat(initial.parentFolderName).isEmpty()
            assertThat(initial.currentZipTreeNode).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the state is updated correctly when ZipNodeTree is returned`() = runTest {
        val testChildren: List<ZipTreeNode> = listOf(mock(), mock())
        val testZipTreeNode = mock<ZipTreeNode> {
            on { children }.thenReturn(testChildren)
        }
        val testZipNodeTree: Map<String, ZipTreeNode> = mapOf(testZipFullPath to testZipTreeNode)
        whenever(savedStateHandle.get<String>(EXTRA_PATH_ZIP)).thenReturn(testZipFullPath)
        whenever(getZipTreeMapUseCase(anyOrNull())).thenReturn(testZipNodeTree)

        initUnderTest()

        underTest.uiState.test {
            val actual = awaitItem()
            assertThat(actual.items).isNotEmpty()
            assertThat(actual.items.size).isEqualTo(2)
            assertThat(actual.folderDepth).isEqualTo(0)
            assertThat(actual.parentFolderName).isEqualTo("ZIP testZipFullPath")
            assertThat(actual.currentZipTreeNode).isEqualTo(testZipTreeNode)
        }
    }
}