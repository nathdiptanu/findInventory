package com.docufind.app.security.crypto

object SecureMemory {
    fun wipe(data: ByteArray?) {
        if (data == null) return
        data.fill(0)
    }

    fun wipe(data: CharArray?) {
        if (data == null) return
        data.fill('\u0000')
    }

    inline fun <T> useAndWipe(bytes: ByteArray, block: (ByteArray) -> T): T {
        return try {
            block(bytes)
        } finally {
            wipe(bytes)
        }
    }
}
