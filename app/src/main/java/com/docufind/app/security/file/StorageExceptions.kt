package com.docufind.app.security.file

class FileTooLargeException(
    val maxBytes: Long = MAX_VAULT_FILE_BYTES
) : Exception("File exceeds maximum size of $maxBytes bytes")

class UnsupportedFileTypeException(
    val mimeType: String
) : Exception("Unsupported file type: $mimeType")

class CorruptedFileException(
    message: String = "File could not be read or is corrupted",
    cause: Throwable? = null
) : Exception(message, cause)

const val MAX_VAULT_FILE_BYTES: Long = 10L * 1024L * 1024L
const val VAULT_STREAM_BUFFER_BYTES = 8192
