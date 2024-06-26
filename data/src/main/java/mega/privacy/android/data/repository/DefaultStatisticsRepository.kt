package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.preferences.StatisticsPreferencesGateway
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.StatisticsRepository
import javax.inject.Inject

/**
 * Default [StatisticsRepository] implementation
 */
internal class DefaultStatisticsRepository @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaApiGateway: MegaApiGateway,
    private val statisticsPreferencesGateway: StatisticsPreferencesGateway,
) : StatisticsRepository {

    override suspend fun sendEvent(
        eventId: Int,
        message: String,
        addJourneyId: Boolean,
        viewId: String?,
    ) = withContext(ioDispatcher) {
        megaApiGateway.sendEvent(eventId, message, addJourneyId, viewId)
    }

    override suspend fun generateViewId(): String = withContext(ioDispatcher) {
        megaApiGateway.generateViewId()
    }

    override suspend fun getMediaDiscoveryClickCount(): Int =
        statisticsPreferencesGateway.getClickCount().first()

    override suspend fun setMediaDiscoveryClickCount(clickCount: Int) =
        statisticsPreferencesGateway.setClickCount(clickCount)


    override suspend fun getMediaDiscoveryClickCountFolder(mediaHandle: Long): Int =
        statisticsPreferencesGateway.getClickCountFolder(mediaHandle).first()


    override suspend fun setMediaDiscoveryClickCountFolder(
        clickCountFolder: Int,
        mediaHandle: Long,
    ) = statisticsPreferencesGateway.setClickCountFolder(clickCountFolder, mediaHandle)

}