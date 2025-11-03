package app.gov.uidai.contactlessregistration.model

data class SharedUiState(
    val isLoadingEmbedder: Boolean = false,
    val isLoadingAssets: Boolean = false,
    val message: String? = null,
)
