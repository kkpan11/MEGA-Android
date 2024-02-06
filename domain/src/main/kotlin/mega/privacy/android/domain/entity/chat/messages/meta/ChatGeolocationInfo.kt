package mega.privacy.android.domain.entity.chat.messages.meta

/**
 * Chat geolocation info
 */
interface ChatGeolocationInfo {
    /**
     * Longitude
     */
    val longitude: Float

    /**
     * Latitude
     */
    val latitude: Float

    /**
     * Image
     */
    val image: String?
}