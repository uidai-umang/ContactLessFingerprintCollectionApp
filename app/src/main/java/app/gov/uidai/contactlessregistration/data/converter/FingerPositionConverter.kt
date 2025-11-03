package app.gov.uidai.contactlessregistration.data.converter

import androidx.room.TypeConverter
import app.gov.uidai.contactlessregistration.model.FingerPosition

class FingerPositionConverter {
    @TypeConverter
    fun fromFingerPosition(position: FingerPosition): String {
        return position.name
    }

    @TypeConverter
    fun toFingerPosition(position: String): FingerPosition {
        return FingerPosition.valueOf(position)
    }
}