package app.gov.uidai.contactlessregistration.model.capture

import com.google.gson.annotations.SerializedName

data class BatchCaptureRequest(
    @SerializedName("captures") val captures: List<CaptureRequest>
)