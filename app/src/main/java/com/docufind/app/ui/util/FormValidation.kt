package com.docufind.app.ui.util

object FormValidation {

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val IFSC_REGEX = Regex("^[A-Za-z]{4}0[A-Za-z0-9]{6}$")

    fun nameError(name: String): Boolean = name.trim().isBlank()

    fun phoneError(phone: String, required: Boolean): Boolean {
        val trimmed = phone.trim()
        if (trimmed.isEmpty()) return required
        val digits = trimmed.filter { it.isDigit() }
        return digits.length < 7 || digits.length > 15
    }

    fun emailError(email: String): Boolean {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return false
        return !EMAIL_REGEX.matches(trimmed)
    }

    /** Returns true when a non-blank IFSC does not match the 11-char Indian format (4 letters + 0 + 6 alphanumerics). */
    fun ifscError(ifsc: String): Boolean {
        val trimmed = ifsc.trim()
        if (trimmed.isEmpty()) return false
        return !IFSC_REGEX.matches(trimmed.uppercase())
    }

    /** Returns true when a non-blank MICR is not exactly 9 digits. */
    fun micrError(micr: String): Boolean {
        val trimmed = micr.trim()
        if (trimmed.isEmpty()) return false
        return trimmed.filter { it.isDigit() }.length != 9
    }

    /** Shows last four digits with leading bullets; used for account numbers on detail screens. */
    fun maskAccountNumber(value: String): String {
        val digits = value.filter { it.isDigit() }
        if (digits.isEmpty()) return "••••"
        if (digits.length <= 4) return "••••"
        return "•••• ${digits.takeLast(4)}"
    }

    fun normalizePhone(phone: String): String = phone.trim().filter { it.isDigit() || it == '+' }

    fun normalizeIfsc(ifsc: String): String = ifsc.trim().uppercase()
}
