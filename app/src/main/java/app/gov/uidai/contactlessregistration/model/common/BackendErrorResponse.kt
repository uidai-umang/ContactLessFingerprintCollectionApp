package app.gov.uidai.contactlessregistration.model.common

import com.google.gson.annotations.SerializedName

data class BackendErrorResponse(
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: Any? = null
)
