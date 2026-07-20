package com.docufind.app.security.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class SecureMemoryTest {

    @Test
    fun wipe_clearsByteArray() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        SecureMemory.wipe(data)
        assertArrayEquals(byteArrayOf(0, 0, 0, 0, 0), data)
    }

    @Test
    fun wipe_clearsCharArray() {
        val data = charArrayOf('p', 'i', 'n')
        SecureMemory.wipe(data)
        assertArrayEquals(charArrayOf('\u0000', '\u0000', '\u0000'), data)
    }
}
