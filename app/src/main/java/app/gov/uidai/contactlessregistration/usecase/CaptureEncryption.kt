package app.gov.uidai.contactlessregistration.usecase

import java.io.File

interface CaptureEncryption {
    fun encryptTo(file: File, bytes: ByteArray)
    fun decryptFrom(file: File): ByteArray
}