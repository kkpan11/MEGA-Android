package mega.privacy.android.data.mapper.mediaplayer

import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import javax.inject.Inject

/**
 * The mapper class for converting the data entity to [SubtitleFileInfo]
 */
class SubtitleFileInfoMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param id file node handle
     * @param name file name
     * @param url file url
     * @param parentName file parent name
     * @param isMarkedSensitive
     * @param isSensitiveInherited
     */
    operator fun invoke(
        id: Long,
        name: String,
        url: String?,
        parentName: String?,
        isMarkedSensitive: Boolean,
        isSensitiveInherited: Boolean,
    ): SubtitleFileInfo =
        SubtitleFileInfo(
            id = id,
            name = name,
            url = url,
            parentName = parentName,
            isMarkedSensitive = isMarkedSensitive,
            isSensitiveInherited = isSensitiveInherited,
        )
}