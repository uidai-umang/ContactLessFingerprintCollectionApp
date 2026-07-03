package app.gov.uidai.contactlessregistration.usecase.impl

import app.gov.uidai.contactlessregistration.data.remote.network.ApiResult
import app.gov.uidai.contactlessregistration.model.resident.ResidentLookupRequest
import app.gov.uidai.contactlessregistration.model.resident.ResidentLookupResponse
import app.gov.uidai.contactlessregistration.repository.ClfRepository
import app.gov.uidai.contactlessregistration.usecase.ResidentUseCase
import javax.inject.Inject

class ResidentUseCaseImpl @Inject constructor(
    private val clfRepository: ClfRepository
) : ResidentUseCase {

    // Builds the lookup request and delegates to repository
    override suspend fun lookupResident(
        aadhaarHash: String,
        ageGroup: String,
        gender: String,
        skinTone: String
    ): ApiResult<ResidentLookupResponse> {
        val request = ResidentLookupRequest(
            aadhaarHash = aadhaarHash,
            ageGroup = ageGroup,
            gender = gender,
            skinTone = skinTone
        )
        return clfRepository.lookupResident(request)
    }
}