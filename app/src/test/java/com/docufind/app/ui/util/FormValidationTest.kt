package com.docufind.app.ui.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class FormValidationTest {

    @Test
    fun nameError_blankIsInvalid() {
        assertTrue(FormValidation.nameError(""))
        assertTrue(FormValidation.nameError("   "))
    }

    @Test
    fun nameError_nonBlankIsValid() {
        assertFalse(FormValidation.nameError("Jane Doe"))
    }

    @Test
    fun phoneError_requiredBlankIsInvalid() {
        assertTrue(FormValidation.phoneError("", required = true))
    }

    @Test
    fun phoneError_optionalBlankIsValid() {
        assertFalse(FormValidation.phoneError("", required = false))
    }

    @Test
    fun phoneError_tooFewDigitsIsInvalid() {
        assertTrue(FormValidation.phoneError("12345", required = true))
    }

    @Test
    fun phoneError_validDigitsIsValid() {
        assertFalse(FormValidation.phoneError("+1 555 123 4567", required = true))
    }

    @Test
    fun emailError_blankIsValid() {
        assertFalse(FormValidation.emailError(""))
    }

    @Test
    fun emailError_invalidFormatIsInvalid() {
        assertTrue(FormValidation.emailError("not-an-email"))
    }

    @Test
    fun emailError_validFormatIsValid() {
        assertFalse(FormValidation.emailError("user@example.com"))
    }

    @Test
    fun ifscError_blankIsValid() {
        assertFalse(FormValidation.ifscError(""))
        assertFalse(FormValidation.ifscError("   "))
    }

    @Test
    fun ifscError_validIndianIfscIsValid() {
        assertFalse(FormValidation.ifscError("SBIN0001234"))
        assertFalse(FormValidation.ifscError("sbin0001234"))
        assertFalse(FormValidation.ifscError("HDFC0A12345"))
        assertFalse(FormValidation.ifscError("UTIB0000553"))
    }

    @Test
    fun ifscError_invalidFormatIsInvalid() {
        assertTrue(FormValidation.ifscError("SBIN001234"))
        assertTrue(FormValidation.ifscError("SBIN00012345"))
        assertTrue(FormValidation.ifscError("12345678901"))
        assertTrue(FormValidation.ifscError("SBIN000123"))
    }

    @Test
    fun micrError_blankIsValid() {
        assertFalse(FormValidation.micrError(""))
    }

    @Test
    fun micrError_nineDigitsIsValid() {
        assertFalse(FormValidation.micrError("400229003"))
        assertFalse(FormValidation.micrError("400229-003"))
    }

    @Test
    fun micrError_wrongLengthIsInvalid() {
        assertTrue(FormValidation.micrError("12345"))
        assertTrue(FormValidation.micrError("1234567890"))
    }

    @Test
    fun maskAccountNumber_showsLastFourDigits() {
        assertEquals("•••• 7890", FormValidation.maskAccountNumber("1234567890"))
        assertEquals("••••", FormValidation.maskAccountNumber("1234"))
        assertEquals("••••", FormValidation.maskAccountNumber(""))
    }

    @Test
    fun normalizeIfsc_uppercasesValue() {
        assertEquals("SBIN0001234", FormValidation.normalizeIfsc("sbin0001234"))
    }
}
