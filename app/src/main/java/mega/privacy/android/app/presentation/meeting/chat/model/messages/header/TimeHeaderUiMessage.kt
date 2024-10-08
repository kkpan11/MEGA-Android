package mega.privacy.android.app.presentation.meeting.chat.model.messages.header

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UIMessageState
import mega.privacy.android.app.presentation.meeting.chat.view.message.TimeHeader
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.model.UIReaction

/**
 * Date header ui message
 *
 * @property timeSent
 */
data class TimeHeaderUiMessage(
    override val id: Long,
    override val timeSent: Long,
    override val displayAsMine: Boolean,
    override val userHandle: Long,
) : HeaderMessage() {

    @Composable
    override fun MessageListItem(
        state: UIMessageState,
        onLongClick: (TypedMessage) -> Unit,
        onMoreReactionsClicked: (Long) -> Unit,
        onReactionClicked: (Long, String, List<UIReaction>) -> Unit,
        onReactionLongClick: (String, List<UIReaction>) -> Unit,
        onForwardClicked: (TypedMessage) -> Unit,
        onSelectedChanged: (Boolean) -> Unit,
        onNotSentClick: (TypedMessage) -> Unit,
        navHostController: NavHostController,
    ) {
        TimeHeader(
            timeString = TimeUtils.formatTime(timeSent),
            displayAsMine = displayAsMine,
            userHandle = userHandle,
            shouldShowName = !state.isOneToOne && userHandle != -1L && !displayAsMine
        )
    }

    override fun key() = "time_header_${id}_${userHandle}"
}