package app.gov.uidai.contactlessregistration.model

data class UIDEntryUiState(
    val uid: String = "",
    val rememberMe: Boolean = false,
    val isLoading: Boolean = false,
    val isMarkingEntry: Boolean = false,
    val isMarkingExit: Boolean = false,
    val isValidUID: Boolean = false,
    val user: User? = null,
    val isUserRegistered: Boolean? = null,
    val attendanceTimeStamp: String? = null,
    val textInputErrorMessage: String? = null,
    val canMarkEntry: Boolean = true,
    val message: String? = null,
){
    val isTextFieldEnabled: Boolean
        get() = !isLoading && !isMarkingEntry && !isMarkingExit
}