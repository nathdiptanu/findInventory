package com.docufind.app.unit.security

import androidx.fragment.app.FragmentActivity
import com.docufind.app.security.auth.AuthGate
import com.docufind.app.security.auth.AuthPurpose
import com.docufind.app.security.auth.AuthResult
import com.docufind.app.security.auth.BiometricAuthManager
import com.docufind.app.security.logging.SecureLogger
import com.docufind.app.security.pin.PinManager
import com.docufind.app.security.session.VaultSessionManager
import com.docufind.app.security.settings.SecurityPreferences
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AuthGateTest {

    private val pinManager = mockk<PinManager>(relaxed = true)
    private val biometricAuthManager = mockk<BiometricAuthManager>()
    private val vaultSessionManager = VaultSessionManager(mockk<SecureLogger>(relaxed = true))
    private val securityPreferences = mockk<SecurityPreferences>()
    private val activity = mockk<FragmentActivity>(relaxed = true)
    private lateinit var authGate: AuthGate

    @Before
    fun setup() {
        every { securityPreferences.preferences } returns flowOf(
            com.docufind.app.security.settings.SecuritySettings(
                biometricEnabled = false,
                allowScreenshots = false,
                autoLockTimeout = com.docufind.app.security.settings.AutoLockTimeout.MIN_5
            )
        )
        every { biometricAuthManager.isBiometricAvailable() } returns false
        authGate = AuthGate(
            pinManager = pinManager,
            biometricAuthManager = biometricAuthManager,
            vaultSessionManager = vaultSessionManager,
            securityPreferences = securityPreferences
        )
    }

    @Test
    fun authenticateForPurpose_vault_shortCircuitsWhenUnlocked() = runTest {
        vaultSessionManager.unlock()

        val result = authGate.authenticateForPurpose(activity, AuthPurpose.VAULT)

        assertThat(result).isEqualTo(AuthResult.Success)
    }

    @Test
    fun authenticateForPurpose_documentExport_neverShortCircuitsWhenUnlocked() = runTest {
        vaultSessionManager.unlock()

        val result = authGate.authenticateForPurpose(activity, AuthPurpose.DOCUMENT_EXPORT)

        assertThat(result).isNotEqualTo(AuthResult.Success)
        assertThat(result).isInstanceOf(AuthResult.Error::class.java)
    }

    @Test
    fun authenticateForPurpose_documentExport_requiresExplicitAuthEvenWhenBiometricDisabled() = runTest {
        vaultSessionManager.unlock()
        coEvery {
            biometricAuthManager.authenticate(activity, AuthPurpose.DOCUMENT_EXPORT)
        } returns AuthResult.Success

        every { securityPreferences.preferences } returns flowOf(
            com.docufind.app.security.settings.SecuritySettings(
                biometricEnabled = true,
                allowScreenshots = false,
                autoLockTimeout = com.docufind.app.security.settings.AutoLockTimeout.MIN_5
            )
        )
        every { biometricAuthManager.isBiometricAvailable() } returns true
        authGate = AuthGate(
            pinManager = pinManager,
            biometricAuthManager = biometricAuthManager,
            vaultSessionManager = vaultSessionManager,
            securityPreferences = securityPreferences
        )

        val result = authGate.authenticateForPurpose(activity, AuthPurpose.DOCUMENT_EXPORT)

        assertThat(result).isEqualTo(AuthResult.Success)
    }
}
