package com.docufind.app.export.pdf

object PdfExportPasswordValidator {
    const val MIN_LENGTH = 8

    sealed class ValidationResult {
        data object Valid : ValidationResult()
        data class Invalid(val message: String) : ValidationResult()
    }

    fun validate(password: CharArray?): ValidationResult {
        if (password == null || password.isEmpty()) {
            return ValidationResult.Invalid("Password is required.")
        }
        if (password.size < MIN_LENGTH) {
            return ValidationResult.Invalid("Password must be at least $MIN_LENGTH characters.")
        }
        return ValidationResult.Valid
    }

    fun passwordsMatch(password: CharArray?, confirm: CharArray?): Boolean {
        if (password == null || confirm == null) return false
        if (password.size != confirm.size) return false
        return password.contentEquals(confirm)
    }
}
