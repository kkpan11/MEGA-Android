package mega.privacy.android.domain.entity.chat.messages.invalid

import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction

/**
 * Unrecognizable invalid message
 *
 * @property msgId
 * @property time
 * @property isMine
 * @property userHandle
 */
@Serializable
data class UnrecognizableInvalidMessage(
    override val chatId: Long,
    override val msgId: Long,
    override val time: Long,
    override val isDeletable: Boolean,
    override val isEditable: Boolean,
    override val isMine: Boolean,
    override val userHandle: Long,
    override val reactions: List<Reaction>,
    override val status: ChatMessageStatus,
    override val content: String?,
) : InvalidMessage