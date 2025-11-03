package app.gov.uidai.contactlessregistration.ui.userinfo

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.gov.uidai.contactlessregistration.model.CBFingerprint
import app.gov.uidai.contactlessregistration.model.CLFingerprint
import app.gov.uidai.contactlessregistration.model.FingerPosition
import app.gov.uidai.contactlessregistration.model.FingerType
import app.gov.uidai.contactlessregistration.model.Fingerprint
import app.gov.uidai.contactlessregistration.model.SDKResult
import app.gov.uidai.contactlessregistration.model.UserInfoUiState
import app.gov.uidai.contactlessregistration.repository.FileRepository
import app.gov.uidai.contactlessregistration.usecase.UserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.gov.uidai.embedding.FingerEmbedder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class UserInfoViewModel @Inject constructor(
    private val userUseCase: UserUseCase,
    private val fileRepository: FileRepository,
    private val fingerEmbedder: FingerEmbedder
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserInfoUiState())
    val uiState = _uiState.asStateFlow()

    var currentUIDHash: String = ""
    private var storedFingerprints: List<Fingerprint> = emptyList()

    fun setUidHash(uidHash: String) {
        currentUIDHash = uidHash
        loadUserData()
    }

    private fun loadUserData() {
        _uiState.update {
            it.copy(isLoadingUserData = true)
        }

        viewModelScope.launch {
            try {
                // Load user data
                val (user, fingerprints) = userUseCase.getUserAndFingerprint(currentUIDHash)

                if (user == null) {
                    _uiState.update {
                        it.copy(
                            isLoadingUserData = false,
                            errorMessage = "User not found"
                        )
                    }
                } else {
                    storedFingerprints = fingerprints
                    _uiState.update {
                        it.copy(
                            isLoadingUserData = false,
                            user = user,
                            errorMessage = null
                        )
                    }
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingUserData = false,
                        errorMessage = "Error loading user data: ${e.message}"
                    )
                }
            }
        }
    }

    fun onSavePreferenceChange(value: Boolean) {
        _uiState.update {
            it.copy(
                saveImageAfterCapture = value
            )
        }
    }

    fun startCapture() {
        _uiState.update {
            if (it.user == null) {
                it.copy(
                    errorMessage = "No user data available"
                )
            } else {
                it.copy(
                    isCapturingImage = true,
                    matchingResults = null,
                    errorMessage = null
                )
            }
        }
    }

    fun onSDKResult(result: SDKResult<CLFingerprint>) {
        when (result) {
            is SDKResult.Success -> {
                viewModelScope.launch {
                    performMatching(result.data)
                }

                if (_uiState.value.saveImageAfterCapture) {
                    viewModelScope.launch(Dispatchers.IO) {
                        val fileName = "[CAP]_${result.data.fingerPosition.name}"
                        fileRepository.saveJP2FingerImageToGallery(
                            uid = currentUIDHash.take(10),
                            fingerType = FingerType.Contactless,
                            fileName = fileName,
                            data = result.data.jp2ByteArray
                        )
                    }
                }
            }

            is SDKResult.Error -> {
                _uiState.update {
                    it.copy(
                        errorMessage = result.message,
                        isCapturingImage = false
                    )
                }
            }
        }
    }

    fun startUpload() {
        _uiState.update {
            if (it.user == null) {
                it.copy(
                    errorMessage = "No user data available"
                )
            } else {
                it.copy(
                    isUploadingImage = true,
                    matchingResults = null,
                    errorMessage = null
                )
            }
        }
    }

    fun onUploadImage(uri: Uri?) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val jp2ByteArray = fileRepository.readJP2FingerImageFromGallery(uri!!)!!
                val (embedding, fingerQuality) = fingerEmbedder.embed(jp2ByteArray)
                val fingerprint = CBFingerprint(
                    fingerPosition = FingerPosition.UNKNOWN,
                    embedding = embedding,
                    fingerQuality = fingerQuality
                )
                performMatching(fingerprint)
            } catch (e: Exception) {
                // Handle exceptions during file access or upload
                _uiState.update {
                    it.copy(
                        errorMessage = "Fingerprint upload failed: $e",
                        isUploadingImage = false
                    )
                }
            }
        }
    }

    private suspend fun performMatching(currentFingerprint: Fingerprint) =
        withContext(Dispatchers.Default) {
            try {
                val currentEmbedding = currentFingerprint.embedding
                val matchingResults = mutableMapOf<FingerPosition, Float>()

                storedFingerprints.forEach {
                    val matchingResult = fingerEmbedder.match(
                        currEmbeddings = currentEmbedding,
                        savedEmbeddings = it.embedding
                    )
                    matchingResults[it.fingerPosition] = matchingResult
                }

                _uiState.update {
                    it.copy(
                        currentFingerprint = currentFingerprint,
                        matchingResults = matchingResults,
                        isUploadingImage = false,
                        isCapturingImage = false
                    )
                }

            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        currentFingerprint = currentFingerprint,
                        errorMessage = "Unable to perform matching with the image.",
                        isUploadingImage = false,
                        isCapturingImage = false
                    )
                }
            }
        }

    fun clearMatchingResult() {
        _uiState.update {
            it.copy(
                matchingResults = null
            )
        }
    }

    fun clearError() {
        _uiState.update {
            it.copy(
                errorMessage = null
            )
        }
    }

    fun retryLoadUserInfo() {
        loadUserData()
    }
}
