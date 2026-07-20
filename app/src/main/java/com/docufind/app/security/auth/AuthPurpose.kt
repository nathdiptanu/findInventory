package com.docufind.app.security.auth

enum class AuthPurpose {
    VAULT,
    DOCUMENT_VIEW,
    DOCUMENT_EXPORT,
    BACKUP_RESTORE
}

sealed class AuthResult {
    data object Success : AuthResult()
    data object Cancelled : AuthResult()
    data object Failed : AuthResult()
    data class Error(val message: String) : AuthResult()
}
