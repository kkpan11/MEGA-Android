package mega.privacy.android.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.MegaDatabase
import mega.privacy.android.data.database.entity.ActiveTransferEntity
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.uri.UriPath
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActiveTransferDaoTest {
    private lateinit var activeTransferDao: ActiveTransferDao
    private lateinit var db: MegaDatabase

    private val entities = TransferType.entries.flatMap { transferType: TransferType ->
        (1..4).map { index ->
            val tag = index + (10 * transferType.ordinal)
            val uniqueId = index + (20L * transferType.ordinal)
            ActiveTransferEntity(
                uniqueId = uniqueId,
                tag = tag,
                transferType = transferType,
                totalBytes = 1024 * (tag.toLong() % 5 + 1),
                isFinished = index.rem(3) == 0,
                isPaused = false,
                isFolderTransfer = false,
                isAlreadyTransferred = false,
                isCancelled = true,
                appData = emptyList(),
                fileName = "file$index.txt",
                localPath = "path",
            )
        }
    }

    @Before
    fun createDb() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, MegaDatabase::class.java
        ).build()
        activeTransferDao = db.activeTransfersDao()
        entities.forEach {
            activeTransferDao.insertOrUpdateActiveTransfer(it)
        }
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun test_that_insert_a_new_entity_actually_inserts_the_entity() = runTest {
        val newEntity = ActiveTransferEntity(
            uniqueId = 200L,
            tag = 100,
            transferType = TransferType.GENERAL_UPLOAD,
            totalBytes = 1024,
            isFinished = true,
            isFolderTransfer = false,
            isPaused = false,
            isAlreadyTransferred = false,
            isCancelled = true,
            appData = listOf(
                TransferAppData.CameraUpload,
                TransferAppData.OriginalUriPath(UriPath("content://uri")),
            ),
            fileName = "fileName",
            localPath = "path",
        )
        activeTransferDao.insertOrUpdateActiveTransfer(newEntity)
        val actual = activeTransferDao.getActiveTransferByUniqueId(newEntity.uniqueId)
        assertThat(actual).isEqualTo(newEntity)
    }

    @Test
    fun test_that_insert_a_duplicated_transfer_updates_the_original_one() = runTest {
        val firstEntity = entities.first { !it.isFinished }
        val modified =
            firstEntity.copy(isFinished = !firstEntity.isFinished, fileName = "newFileName.txt")
        activeTransferDao.insertOrUpdateActiveTransfer(modified)
        val result = activeTransferDao.getCurrentActiveTransfersByType(firstEntity.transferType)
        assertThat(result).contains(modified)
        assertThat(result).doesNotContain(firstEntity)
    }

    @Test
    fun test_that_insert_a_duplicated_transfer_updates_the_original_one_only_if_not_finished() =
        runTest {
            val entitiesToModified = entities.associateBy { it.copy(isPaused = !it.isPaused) }
            activeTransferDao.insertOrUpdateActiveTransfers(entitiesToModified.values.toList())

            entitiesToModified.forEach { (original, modified) ->
                val result = activeTransferDao.getActiveTransferByUniqueId(original.uniqueId)
                if (original.isFinished) {
                    assertThat(result).isEqualTo(original)
                } else {
                    assertThat(result?.isPaused).isEqualTo(modified.isPaused)
                }
            }
        }

    @Test
    fun test_that_getActiveTransferByUniqueId_returns_the_correct_active_transfer() = runTest {
        entities.forEach { entity ->
            val actual = activeTransferDao.getActiveTransferByUniqueId(entity.uniqueId)
            assertThat(actual).isEqualTo(entity)
        }
    }

    @Test
    fun test_that_getActiveTransferByTag_returns_the_correct_active_transfer() = runTest {
        entities.forEach { entity ->
            val actual = activeTransferDao.getActiveTransferByTag(entity.tag)
            assertThat(actual).isEqualTo(entity)
        }
    }

    @Test
    fun test_that_getActiveTransfersByType_returns_all_transfers_of_that_type() = runTest {
        TransferType.entries.forEach { type ->
            val expected = entities.filter { it.transferType == type }
            val actual = activeTransferDao.getActiveTransfersByType(type).first()
            assertThat(actual).containsExactlyElementsIn(expected)
        }
    }

    @Test
    fun test_that_getCurrentActiveTransfersByType_returns_all_transfers_of_that_type() = runTest {
        TransferType.entries.forEach { type ->
            val expected = entities.filter { it.transferType == type }
            val actual = activeTransferDao.getCurrentActiveTransfersByType(type)
            assertThat(actual).containsExactlyElementsIn(expected)
        }
    }

    @Test
    fun test_that_getCurrentActiveTransfers_returns_all_transfers() = runTest {
        val expected = entities
        val actual = activeTransferDao.getCurrentActiveTransfers()
        assertThat(actual).containsExactlyElementsIn(expected)
    }

    @Test
    fun test_that_deleteAllActiveTransfersByType_deletes_all_transfers_of_that_type() = runTest {
        TransferType.entries.forEach { type ->
            val initial = activeTransferDao.getCurrentActiveTransfersByType(type)
            assertThat(initial).isNotEmpty()
            activeTransferDao.deleteAllActiveTransfersByType(type)
            val actual = activeTransferDao.getCurrentActiveTransfersByType(type)
            assertThat(actual).isEmpty()
        }
    }

    @Test
    fun test_that_deleteAllActiveTransfers_deletes_all_transfers() = runTest {
        val initial = activeTransferDao.getCurrentActiveTransfers()
        assertThat(initial).isNotEmpty()
        activeTransferDao.deleteAllActiveTransfers()
        val actual = activeTransferDao.getCurrentActiveTransfers()
        assertThat(actual).isEmpty()
    }

    @Test
    fun test_that_setActiveTransfersAsFinishedByUniqueId_set_as_finished_transfers_with_given_tags() =
        runTest {
            TransferType.entries.forEach { type ->
                val initial = activeTransferDao.getCurrentActiveTransfersByType(type)
                val toFinish = initial.take(initial.size / 2).filter { !it.isFinished }
                assertThat(initial).isNotEmpty()
                assertThat(toFinish).isNotEmpty()
                activeTransferDao.setActiveTransfersAsFinishedByUniqueId(
                    uniqueIds = toFinish.map { it.uniqueId },
                    cancelled = false
                )
                val actual = activeTransferDao.getCurrentActiveTransfersByType(type)
                toFinish.forEach { finished ->
                    assertThat(actual.first { it.tag == finished.tag }.isFinished).isTrue()
                    assertThat(actual.first { it.tag == finished.tag }.isCancelled).isFalse()
                }
            }
        }

    @Test
    fun test_that_setActiveTransfersAsFinishedByUniqueId_set_as_finished_and_cancelled_all_transfers_with_given_tags() =
        runTest {
            TransferType.entries.forEach { type ->
                val initial = activeTransferDao.getCurrentActiveTransfersByType(type)
                val toFinish = initial.take(initial.size / 2).filter { !it.isFinished }
                assertThat(initial).isNotEmpty()
                assertThat(toFinish).isNotEmpty()
                activeTransferDao.setActiveTransfersAsFinishedByUniqueId(
                    uniqueIds = toFinish.map { it.uniqueId },
                    cancelled = true
                )
                val actual = activeTransferDao.getCurrentActiveTransfersByType(type)
                toFinish.forEach { finished ->
                    assertThat(actual.first { it.tag == finished.tag }.isFinished).isTrue()
                    assertThat(actual.first { it.tag == finished.tag }.isCancelled).isTrue()
                }
            }
        }
}
