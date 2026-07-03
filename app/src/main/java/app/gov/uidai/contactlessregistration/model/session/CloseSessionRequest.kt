package app.gov.uidai.contactlessregistration.model.session

import com.google.gson.annotations.SerializedName

data class CloseSessionRequest(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("close_reason") val closeReason: String = "completed"
)