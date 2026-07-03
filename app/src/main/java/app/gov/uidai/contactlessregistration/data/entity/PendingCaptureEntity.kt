package app.gov.uidai.contactlessregistration.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_captures")
data class PendingCaptureEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: String,
    val residentPseudonymId: String,
    val operatorId: String,
    val fingerType: String,
    val hand: String,
    val imageBytes: ByteArray,
    val imageChecksum: String,
    val cameraModel: String,
    val cameraResolution: String,
    val deviceModel: String,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    // ByteArray needs manual equals/hashCode — Room requires this
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PendingCaptureEntity
        if (id != other.id) return false
        if (!imageBytes.contentEquals(other.imageBytes)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + imageBytes.contentHashCode()
        return result
    }
}