package app.gov.uidai.contactlessregistration.ui.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.gov.uidai.contactlessregistration.model.CLFingerprint
import app.gov.uidai.contactlessregistration.model.FingerPosition
import app.gov.uidai.contactlessregistration.model.FingerType
import app.gov.uidai.contactlessregistration.model.RegistrationUiState
import app.gov.uidai.contactlessregistration.model.SDKResult
import app.gov.uidai.contactlessregistration.model.User
import app.gov.uidai.contactlessregistration.repository.FileRepository
import app.gov.uidai.contactlessregistration.usecase.UserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.collections.set

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val userUseCase: UserUseCase,
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistrationUiState())
    val uiState = _uiState.asStateFlow()

    private val _registrationResult = MutableStateFlow<RegistrationResult?>(null)
    val registrationResult = _registrationResult.asStateFlow()

    private var currentUidHash: String = ""

    fun setUidHash(uidHash: String) {
        currentUidHash = uidHash
    }

    fun onNameChanged(name: String) {
        _uiState.update {
            it.copy(
                name = name
            )
        }
    }

    fun onPhoneNumberChanged(phoneNumber: String) {
        _uiState.update {
            it.copy(
                phoneNumber = phoneNumber
            )
        }
    }

    fun startFingerprintCapture(fingerPosition: FingerPosition, launchSdk: () -> Unit) {
        val currentState = _uiState.value

        if(currentState.loadingFinger != null) {
            _uiState.update {
                it.copy(
                    message = "Already processing ${currentState.loadingFinger.name.replace('_', ' ')}"
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    loadingFinger = fingerPosition
                )
            }
            launchSdk()
        }
    }

    fun onSDKResult(result: SDKResult<CLFingerprint>) {
        val currentState = _uiState.value
        when (result) {
            is SDKResult.Success -> {
                val data = result.data.copy(fingerPosition = currentState.loadingFinger!!)
                val updatedFingerprints = currentState.fingerprints.toMutableMap().apply {
                    set(currentState.loadingFinger, data)
                }
                _uiState.update {
                    currentState.copy(
                        fingerprints = updatedFingerprints,
                        loadingFinger = null,
                        message = null
                    )
                }
            }

            is SDKResult.Error -> {
                _uiState.update {
                    currentState.copy(
                        loadingFinger = null,
                        message = result.message
                    )
                }
            }
        }
    }

    fun removeFingerprint(fingerPosition: FingerPosition) {
        val currentState = _uiState.value
        val updatedFingerprints = currentState.fingerprints.toMutableMap().apply {
            set(fingerPosition, null)
        }

        _uiState.update {
            currentState.copy(
                fingerprints = updatedFingerprints
            )
        }
    }

    fun registerUser() {
        val currentState = _uiState.value

        if (!currentState.canSave) {
            _registrationResult.update {
                RegistrationResult.Error("Cannot save - Required fields are missing")
            }
            return
        }

        if(currentState.loadingFinger != null){
            _uiState.update {
                it.copy(
                    message = "Please wait! ${currentState.loadingFinger.name.replace('_', ' ')} is processing"
                )
            }
            return
        }

        _uiState.update {
            it.copy(isLoading = true)
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fingerprints = currentState.fingerprints.values.filterNotNull()
                // Save user data
                userUseCase.register(
                    uidHash = currentUidHash,
                    user = User(
                        name = currentState.name,
                        phoneNumber = currentState.phoneNumber
                    ),
                    fingerprints = fingerprints
                )

                saveRegisteredImagesToGallery(fingerprints = fingerprints)

                _registrationResult.update { RegistrationResult.Success }

            } catch (ex: Exception) {
                _uiState.update {
                    currentState.copy(
                        isLoading = false,
                        message = "Registration failed: ${ex.message}"
                    )
                }
                _registrationResult.update {
                    RegistrationResult.Error("Registration failed: ${ex.message}")
                }
            }
        }
    }

    suspend fun saveRegisteredImagesToGallery(fingerprints: List<CLFingerprint>) =
        withContext(Dispatchers.IO) {
            fingerprints.forEach {
                val data = it.jp2ByteArray
                val fileName = "[REG]_${it.fingerPosition.name}"
                launch {
                    fileRepository.saveJP2FingerImageToGallery(
                        uid = currentUidHash.take(10),
                        fingerType = FingerType.Contactless,
                        fileName = fileName,
                        data = data
                    )
                }
            }
        }

    fun clearError() {
        _uiState.update {
            it.copy(
                message = null
            )
        }
    }
}

sealed class RegistrationResult {
    object Success : RegistrationResult()
    data class Error(val message: String) : RegistrationResult()
}
