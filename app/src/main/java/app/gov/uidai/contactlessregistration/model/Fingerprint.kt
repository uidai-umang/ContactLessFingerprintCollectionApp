package app.gov.uidai.contactlessregistration.model

import android.graphics.Bitmap
import `in`.gov.uidai.embedding.model.FingerQuality

open class Fingerprint(
    val fingerPosition: FingerPosition,
    val embedding: ByteArray,
    val fingerQuality: FingerQuality? = null
)

class CLFingerprint(
    fingerPosition: FingerPosition,
    embedding: ByteArray,
    fingerQuality: FingerQuality? = null,
    val jp2ByteArray: ByteArray,
    val bitmap: Bitmap,
    val imageBytes: ByteArray = jp2ByteArray
) : Fingerprint(
    fingerPosition = fingerPosition,
    embedding = embedding,
    fingerQuality = fingerQuality
) {

    fun copy(
        fingerPosition: FingerPosition = this.fingerPosition,
        embedding: ByteArray = this.embedding,
        jp2ByteArray: ByteArray = this.jp2ByteArray,
        fingerQuality: FingerQuality? = this.fingerQuality,
        bitmap: Bitmap = this.bitmap,
        imageBytes: ByteArray = this.imageBytes
    ): CLFingerprint = CLFingerprint(
        fingerPosition = fingerPosition,
        embedding = embedding,
        fingerQuality = fingerQuality,
        jp2ByteArray = jp2ByteArray,
        bitmap = bitmap,
        imageBytes = imageBytes
    )
}

class CBFingerprint(
    fingerPosition: FingerPosition,
    embedding: ByteArray,
    fingerQuality: FingerQuality? = null
) : Fingerprint(
    fingerPosition = fingerPosition,
    embedding = embedding,
    fingerQuality = fingerQuality
)
