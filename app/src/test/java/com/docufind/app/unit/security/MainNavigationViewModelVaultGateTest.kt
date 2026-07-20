package com.docufind.app.unit.security

import com.docufind.app.reminder.ReminderPendingNavigation
import com.docufind.app.security.auth.AuthGate
import com.docufind.app.security.auth.BiometricAuthManager
import com.docufind.app.security.logging.SecureLogger
import com.docufind.app.security.pin.PinManager
import com.docufind.app.security.session.VaultSessionManager
import com.docufind.app.security.settings.AutoLockTimeout
import com.docufind.app.security.settings.SecurityPreferences
import com.docufind.app.security.settings.SecuritySettings
import com.docufind.app.ui.navigation.MainNavigationViewModel
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Test

class MainNavigationViewModelVaultGateTest {

    private val reminderPendingNavigation = mockk<ReminderPendingNavigation>(relaxed = true)
    private val vaultSessionManager = VaultSessionManager(mockk<SecureLogger>(relaxed = true))
    private val authGate = mockk<AuthGate>(relaxed = true)
    private val pinManager = mockk<PinManager>()
    private val biometricAuthManager = mockk<BiometricAuthManager>(relaxed = true)
    private val securityPreferences = mockk<SecurityPreferences>()
    private lateinit var viewModel: MainNavigationViewModel

    @Before
    fun setup() {
        every { securityPreferences.preferences } returns flowOf(
            SecuritySettings(biometricEnabled = false, allowScreenshots = false, autoLockTimeout = AutoLockTimeout.MIN_5)
        )
        every { pinManager.hasPinConfigured() } returns true
        every { biometricAuthManager.isBiometricAvailable() } returns true
        viewModel = MainNavigationViewModel(
            reminderPendingNavigation,
            vaultSessionManager,
            authGate,
            pinManager,
            biometricAuthManager,
            securityPreferences
        )
    }

    @Test
    fun openRecord_whenLocked_showsUnlockPrompt() {
        vaultSessionManager.lock()
        viewModel.openRecord("rec-123")
        assertThat(viewModel.unlockPrompt.value.visible).isTrue()
        assertThat(viewModel.navigateToRecord.value).isNull()
    }

    @Test
    fun openRecord_whenUnlocked_navigatesImmediately() {
        vaultSessionManager.unlock()
        viewModel.openRecord("rec-123")
        assertThat(viewModel.navigateToRecord.value).isEqualTo("rec-123")
        assertThat(viewModel.unlockPrompt.value.visible).isFalse()
    }

    @Test
    fun openRecord_withoutPin_routesToVaultSetup() {
        every { pinManager.hasPinConfigured() } returns false
        viewModel.openRecord("rec-123")
        assertThat(viewModel.unlockPrompt.value.navigateToVaultForSetup).isTrue()
        assertThat(viewModel.navigateToVault.value).isTrue()
    }

    @Test
    fun openSearch_whenLocked_showsUnlockPrompt() {
        vaultSessionManager.lock()
        viewModel.openSearch("")
        assertThat(viewModel.unlockPrompt.value.visible).isTrue()
        assertThat(viewModel.navigateToSearch.value).isFalse()
    }

    @Test
    fun openRecord_blankId_isIgnored() {
        vaultSessionManager.unlock()
        viewModel.openRecord("")
        assertThat(viewModel.navigateToRecord.value).isNull()
    }
}
