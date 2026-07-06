package app.gov.uidai.contactlessregistration.usecase

import app.gov.uidai.contactlessregistration.data.remote.network.ApiResult
import app.gov.uidai.contactlessregistration.model.resident.ResidentLookupRequest
import app.gov.uidai.contactlessregistration.model.resident.ResidentLookupResponse

interface ResidentUseCase {

    suspend fun lookupResident(
        aadhaarHash: String,
        ageGroup: String,
        gender: String,
        skinTone: String
    ): ApiResult<ResidentLookupResponse>
}