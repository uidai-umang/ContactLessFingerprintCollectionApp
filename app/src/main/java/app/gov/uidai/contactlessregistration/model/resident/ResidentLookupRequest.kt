package app.gov.uidai.contactlessregistration.model.resident

import com.google.gson.annotations.SerializedName

data class ResidentLookupRequest(
    @SerializedName("aadhaar_hash") val aadhaarHash: String,
    @SerializedName("age_group") val ageGroup: String,
    @SerializedName("gender") val gender: String,
    @SerializedName("skin_tone") val skinTone: String
)