package com.docufind.app.scan

object PhoneNumberNormalizer {

    fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        val withoutTel = trimmed.removePrefix("tel:").trim()
        val digitsAndPlus = buildString {
            withoutTel.forEachIndexed { index, char ->
                when {
                    char == '+' && index == 0 -> append(char)
                    char.isDigit() -> append(char)
                }
            }
        }
        if (digitsAndPlus.isEmpty()) return null

        val digitCount = digitsAndPlus.count { it.isDigit() }
        if (digitCount !in 7..15) return null

        return digitsAndPlus
    }

    fun digitsOnly(raw: String): String = raw.filter { it.isDigit() }
}
