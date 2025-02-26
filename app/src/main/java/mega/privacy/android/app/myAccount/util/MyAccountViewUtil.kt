package mega.privacy.android.app.myAccount.util

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.MyAccountPaymentInfoContainerBinding
import mega.privacy.android.app.databinding.MyAccountUsageContainerBinding
import mega.privacy.android.app.utils.StringUtils.formatColorTag
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.transfer.UsedTransferStatus
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava

object MyAccountViewUtil {

    /**
     * Enum class defining the active fragment.
     */
    enum class ActiveFragment {
        MY_ACCOUNT, MY_ACCOUNT_USAGE
    }

    private fun getGettingInfo(context: Context) = context.getString(R.string.recovering_info)

    /**
     * Updates the views related to usage of storage and transfers
     * for all type of accounts except business ones.
     *
     * @param viewModel MyAccountViewModel to check the data.
     */
    fun MyAccountUsageContainerBinding.update(
        context: Context,
        storageState: StorageState,
        isFreeAccount: Boolean,
        totalStorage: String,
        totalTransfer: String,
        usedStorage: String,
        usedStoragePercentage: Int,
        usedTransfer: String,
        usedTransferPercentage: Int,
        usedTransferStatus: UsedTransferStatus,
    ) {
        usageLayoutBusiness.isVisible = false
        storageLayout.isVisible = true
        transferLayout.isVisible = true

        if (usedStorage.isEmpty()) {
            storageProgressPercentage.isVisible = false
            storageProgressBar.progress = 0
            storageProgress.text = getGettingInfo(context)
        } else {
            val isStorageOverQuota = storageState == StorageState.Red

            storageProgressPercentage.apply {
                isVisible = true
                text = context.getString(
                    R.string.used_storage_transfer_percentage,
                    usedStoragePercentage.toString()
                )
            }

            storageProgressBar.progress = usedStoragePercentage

            val storageColors = when (storageState) {
                StorageState.Red -> ContextCompat.getColorStateList(
                    context,
                    R.color.color_support_error
                )

                StorageState.Orange -> ContextCompat.getColorStateList(
                    context,
                    R.color.color_support_warning
                )

                else -> ContextCompat.getColorStateList(context, R.color.color_support_success)
            }
            storageProgressBar.progressTintList = storageColors
            storageProgressPercentage.setTextColor(storageColors)

            storageProgress.text = context.getString(
                R.string.used_storage_transfer,
                usedStorage,
                totalStorage
            ).let { text ->
                if (isStorageOverQuota) {
                    text.formatColorTag(storageProgress.context, 'A', R.color.color_text_error)
                        .toSpannedHtmlText()
                } else {
                    text.replace("[A]", "")
                        .replace("[/A]", "")
                }
            }
        }

        transferLayout.isVisible = !isFreeAccount

        if (usedTransfer.isEmpty()) {
            transferProgressPercentage.isVisible = false
            transferProgressBar.progress = 0
            transferProgress.text = getGettingInfo(context)
        } else {
            transferProgressPercentage.apply {
                isVisible = true
                text = context.getString(
                    R.string.used_storage_transfer_percentage,
                    usedTransferPercentage.toString()
                )
            }

            transferProgressBar.progress = usedTransferPercentage

            val transferColors = when (usedTransferStatus) {
                UsedTransferStatus.Full -> ContextCompat.getColorStateList(
                    context,
                    R.color.color_support_error
                )

                UsedTransferStatus.AlmostFull -> ContextCompat.getColorStateList(
                    context,
                    R.color.color_support_warning
                )

                else -> ContextCompat.getColorStateList(context, R.color.color_support_success)
            }
            transferProgressBar.progressTintList = transferColors
            transferProgressPercentage.setTextColor(transferColors)

            transferProgress.text = context.getString(
                R.string.used_storage_transfer,
                usedTransfer,
                totalTransfer
            ).replace("[A]", "")
                .replace("[/A]", "")
        }

        root.post { checkImagesOrProgressBarVisibility(isFreeAccount) }
    }

    /**
     * Updates the views related to usage of storage and transfers
     * for only business accounts.
     *
     * @param viewModel MyAccountViewModel to check the data.
     */
    fun MyAccountUsageContainerBinding.updateBusinessOrProFlexi(
        context: Context,
        usedStorage: String,
        usedTransFer: String,
    ) {
        storageLayout.isVisible = false
        transferLayout.isVisible = false
        usageLayoutBusiness.isVisible = true

        storageProgressB.text = usedStorage.ifEmpty { getGettingInfo(context) }
        transferProgressB.text = usedTransFer.ifEmpty { getGettingInfo(context) }
    }

    /**
     * Updates the views related to payments for all type of accounts except business ones.
     *
     * @param viewModel MyAccountViewModel to check the data.
     * @param fragment Value from `ActiveFragment` enum indicating what is the active fragment.
     */
    fun MyAccountPaymentInfoContainerBinding.update(
        renewTime: Long,
        expirationTime: Long,
        hasRenewableSubscription: Boolean = false,
        hasExpirableSubscription: Boolean = false,
        isFreeAccount: Boolean,
        fragment: ActiveFragment,
    ): Boolean {
        businessStatusText.isVisible = false

        return if ((hasRenewableSubscription || hasExpirableSubscription) && !isFreeAccount) {
            setRenewOrExpiryDate(renewTime, expirationTime, hasRenewableSubscription, fragment)
            true
        } else false
    }

    /**
     * Updates the views related to payments for only business accounts.
     *
     * @param megaApi       MegaApiAndroid to check business status.
     * @param viewModel     MyAccountViewModel to check the data.
     * @param expandedView  True if the binding is in the expanded view, false otherwise.
     * @param fragment      Value from `ActiveFragment` enum indicating what is the active fragment.
     */
    fun MyAccountPaymentInfoContainerBinding.businessOrProFlexiUpdate(
        context: Context,
        renewTime: Long,
        expirationTime: Long,
        hasRenewableSubscription: Boolean = false,
        hasExpirableSubscription: Boolean = false,
        megaApi: MegaApiAndroid,
        expandedView: Boolean,
        fragment: ActiveFragment,
        isProFlexi: Boolean,
    ) {
        if (!megaApi.isMasterBusinessAccount && !isProFlexi) {
            return
        }

        val businessStatus = megaApi.businessStatus

        when {
            businessStatus == MegaApiJava.BUSINESS_STATUS_EXPIRED -> {
                setBusinessProFlexiAlert(true, expandedView, context)
            }

            businessStatus == MegaApiJava.BUSINESS_STATUS_GRACE_PERIOD -> {
                setBusinessProFlexiAlert(false, expandedView, context)
            }

            hasRenewableSubscription || hasExpirableSubscription -> {
                setRenewOrExpiryDate(renewTime, expirationTime, hasRenewableSubscription, fragment)
            }
        }
    }

    /**
     * Updates the views related to payments for all type of accounts.
     *
     * @param viewModel MyAccountViewModel to check the data.
     * @param renewable True if the subscriptions is renewable, false otherwise.
     * @param fragment  Value from `ActiveFragment` enum indicating what is the active fragment.
     */
    private fun MyAccountPaymentInfoContainerBinding.setRenewOrExpiryDate(
        renewTime: Long,
        expirationTime: Long,
        renewable: Boolean,
        fragment: ActiveFragment,
    ) {
        businessStatusText.isVisible = false

        renewExpiryText.apply {
            isVisible = true
            text = context.getString(
                if (renewable) R.string.account_info_renews_on else R.string.account_info_expires_on,
                TimeUtils.formatDate(
                    if (renewable) renewTime else expirationTime,
                    TimeUtils.DATE_MM_DD_YYYY_FORMAT,
                    context
                )
            )

            text = when (fragment) {
                ActiveFragment.MY_ACCOUNT -> {
                    text.toString()
                        .formatColorTag(context, 'A', R.color.grey_087_white_054)
                        .formatColorTag(context, 'B', R.color.grey_087_white_054)
                        .toSpannedHtmlText()
                }

                ActiveFragment.MY_ACCOUNT_USAGE -> {
                    text.toString()
                        .formatColorTag(context, 'A', R.color.grey_087_white_054)
                        .formatColorTag(context, 'B', R.color.grey_087_white_054)
                        .toSpannedHtmlText()
                }
            }
        }
    }

    /**
     * Updates the business alert view by setting the expired or grace period message.
     *
     * @param expired True if the account is expired, false otherwise.
     * @param expandedView  True if the binding is in the expanded view, false otherwise.
     */
    private fun MyAccountPaymentInfoContainerBinding.setBusinessProFlexiAlert(
        expired: Boolean,
        expandedView: Boolean,
        context: Context,
    ) {
        renewExpiryText.isVisible = false

        businessStatusText.apply {
            isVisible = true
            text = context.getString(
                if (expired) R.string.payment_overdue_label
                else R.string.payment_required_label
            )

            setTextAppearance(
                when {
                    expandedView && expired -> R.style.TextAppearance_Mega_Body1_Red600Red300
                    expandedView -> R.style.TextAppearance_Mega_Body1_Amber700Amber300
                    expired -> R.style.TextAppearance_Mega_Body1_Red600Red300
                    else -> R.style.TextAppearance_Mega_Body1_Amber700Amber300
                }
            )
        }
    }

    /**
     * Checks if should show the images of storage and transfer in case of business accounts
     * and the ProgressBars in case of non business accounts.
     * If some of the text storageProgress, storageLabel, transferProgress or transferLabel
     * occupies more than one line, then they should be hidden.
     *
     * @param isFreeAccount True if is a free account, false otherwise.
     */
    private fun MyAccountUsageContainerBinding.checkImagesOrProgressBarVisibility(isFreeAccount: Boolean) {
        val visible = when {
            isFreeAccount -> storageProgress.lineCount == 1 && storageLabel.lineCount == 1
            else -> storageProgress.lineCount == 1 && storageLabel.lineCount == 1
                    && transferProgress.lineCount == 1 && transferLabel.lineCount == 1
        }

        storageProgressLayout.isVisible = visible
        transferProgressLayout.isVisible = visible
    }
}