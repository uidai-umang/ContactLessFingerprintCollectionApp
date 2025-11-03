package app.gov.uidai.contactlessregistration.repository

import android.net.Uri
import app.gov.uidai.contactlessregistration.model.FingerType

interface FileRepository {
    suspend fun saveJP2FingerImageToGallery(uid: String, fingerType: FingerType, fileName: String, data: ByteArray)
    suspend fun readAsset(path: String): ByteArray
    suspend fun readJP2FingerImageFromGallery(uri: Uri): ByteArray?
}