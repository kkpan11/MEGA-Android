package mega.privacy.android.app.presentation.settings

import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.R
import mega.privacy.android.app.RecyclerViewAssertions.Companion.onViewHolder
import mega.privacy.android.app.RecyclerViewAssertions.Companion.withRowContaining
import mega.privacy.android.app.presentation.settings.SettingsActivity
import mega.privacy.android.domain.usecase.IsUseHttpsEnabledUseCase
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.wheneverBlocking

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Ignore("Ignore the unstable test. Will add the tests back once stability issue is resolved.")
class SettingsActivityTest{

    var hiltRule = HiltAndroidRule(this)
    private val isUseHttpsEnabled = mock<IsUseHttpsEnabledUseCase>()

    @get:Rule
    var ruleChain: RuleChain = RuleChain
        .outerRule(hiltRule)
        .around(ActivityScenarioRule(SettingsActivity::class.java))

    @Before
    fun setUp() {
        wheneverBlocking { isUseHttpsEnabled() }.thenReturn(true)
        hiltRule.inject()
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    fun test_that_settings_advanced_fragment_is_loaded_post_30() {
        testNavigateToAdvancedSettings()
    }

    @Test
    @SdkSuppress(maxSdkVersion = 29)
    fun test_that_settings_advanced_fragment_is_loaded_pre_30() {
        testNavigateToAdvancedSettings()
    }

    private fun testNavigateToAdvancedSettings() {
        onPreferences()
            .perform(
                RecyclerViewActions.scrollToHolder(
                    hasDescendant(withText(R.string.settings_advanced_features)).onViewHolder()
                )
            )

        onPreferences()
            .perform(
                RecyclerViewActions.actionOnHolderItem(
                    hasDescendant(withText(R.string.settings_advanced_features)).onViewHolder(),
                    click()
                )
            )

        onPreferences()
            .check(
                withRowContaining(
                    hasDescendant(withText(R.string.setting_subtitle_use_https_only))
                )
            )
    }

}