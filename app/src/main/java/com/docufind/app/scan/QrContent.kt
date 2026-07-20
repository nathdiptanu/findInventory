package com.docufind.app.scan

sealed class QrContent {
    data class Phone(val number: String) : QrContent()
    data class WhatsApp(val number: String) : QrContent()
    data class Email(val address: String) : QrContent()
    data class Url(val url: String, val isSafeScheme: Boolean) : QrContent()
    data class Contact(
        val displayName: String?,
        val phone: String?,
        val email: String?,
        val rawVCard: String
    ) : QrContent()
    data class PlainText(val text: String) : QrContent()
}
