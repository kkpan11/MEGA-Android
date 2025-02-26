package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Upgrade security use case
 */
class UpgradeSecurityUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke() =
        accountRepository.upgradeSecurity()
}
