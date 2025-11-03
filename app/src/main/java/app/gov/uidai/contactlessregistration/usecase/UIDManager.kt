package app.gov.uidai.contactlessregistration.usecase

interface UIDManager {
    fun validateUID(uid: String): Boolean
    fun hashUID(uid: String): String
}


