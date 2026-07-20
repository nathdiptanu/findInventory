package com.docufind.app.unit.security

import com.docufind.app.security.crypto.Pbkdf2KeyDeriver
import com.docufind.app.security.logging.SecureLogger
import com.docufind.app.security.pin.LockoutManager
import com.docufind.app.security.pin.LockoutState
import com.docufind.app.security.pin.PinManager
import com.docufind.app.security.pin.PinStorage
import com.docufind.app.security.pin.PinVerificationResult
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class PinManagerTest {

    private val pinStorage = mockk<PinStorage>(relaxed = true)
    private val pbkdf2 = Pbkdf2KeyDeriver()
    private val lockoutManager = mockk<LockoutManager>(relaxed = true)
    private val secureLogger = mockk<SecureLogger>(relaxed = true)
    private lateinit var pinManager: PinManager

    @Before
    fun setup() {
        pinManager = PinManager(pinStorage, pbkdf2, lockoutManager, secureLogger)
        every { lockoutManager.checkLockout() } returns LockoutState.Allowed
    }

    @Test
    fun setPin_storesVerifierNotPlaintext() {
        val pin = "123456".toCharArray()
        pinManager.setPin(pin.copyOf())
        verify { pinStorage.savePinVerifier(any(), any()) }
    }

    @Test
    fun verifyPin_correctPin_returnsSuccess() {
        val pin = "654321".toCharArray()
        val salt = pbkdf2.generateSalt()
        val hash = pbkdf2.hashForVerification(pin.copyOf(), salt)
        every { pinStorage.loadPinVerifier() } returns PinStorage.PinVerifier(salt, hash)

        val result = pinManager.verifyPin("654321".toCharArray())

        assertThat(result).isEqualTo(PinVerificationResult.Success)
    }

    @Test
    fun verifyPin_wrongPin_returnsFailure() {
        val pin = "111111".toCharArray()
        val salt = pbkdf2.generateSalt()
        val hash = pbkdf2.hashForVerification(pin.copyOf(), salt)
        every { pinStorage.loadPinVerifier() } returns PinStorage.PinVerifier(salt, hash)
        every { lockoutManager.recordFailure() } returns LockoutState.Allowed
        every { lockoutManager.remainingAttemptsBeforeLockout() } returns 4

        val result = pinManager.verifyPin("999999".toCharArray())

        assertThat(result).isInstanceOf(PinVerificationResult.Failure::class.java)
    }

    @Test
    fun verifyPin_notConfigured_returnsNotConfigured() {
        every { pinStorage.loadPinVerifier() } returns null
        val result = pinManager.verifyPin("123456".toCharArray())
        assertThat(result).isEqualTo(PinVerificationResult.NotConfigured)
    }

    @Test(expected = IllegalArgumentException::class)
    fun setPin_wrongLength_throws() {
        pinManager.setPin("12345".toCharArray())
    }
}
