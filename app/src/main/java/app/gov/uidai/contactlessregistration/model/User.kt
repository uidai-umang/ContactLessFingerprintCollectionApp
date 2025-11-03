package app.gov.uidai.contactlessregistration.model

data class User(
    val name: String,
    val phoneNumber: String,
    val fingerprintCount: Int = 0
)