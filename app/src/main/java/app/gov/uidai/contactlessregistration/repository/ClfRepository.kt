package app.gov.uidai.contactlessregistration.repository

import app.gov.uidai.contactlessregistration.data.remote.network.ApiResult
import app.gov.uidai.contactlessregistration.model.capture.BatchCaptureRequest
import app.gov.uidai.contactlessregistration.model.capture.CaptureRequest
import app.gov.uidai.contactlessregistration.model.capture.CaptureResponse
import app.gov.uidai.contactlessregistration.model.resident.ResidentLookupRequest
import app.gov.uidai.contactlessregistration.model.resident.ResidentLookupResponse
import app.gov.uidai.contactlessregistration.model.session.CloseSessionRequest
import app.gov.uidai.contactlessregistration.model.session.CreateSessionRequest
import app.gov.uidai.contactlessregistration.model.session.CreateSessionResponse

interface ClfRepository {

    suspend fun lookupResident(
        request: ResidentLookupRequest
    ): ApiResult<ResidentLookupResponse>

    suspend fun createSession(
        request: CreateSessionRequest
    ): ApiResult<CreateSessionResponse>

    suspend fun closeSession(
        request: CloseSessionRequest
    ): ApiResult<Unit>

    suspend fun uploadCapture(
        request: CaptureRequest
    ): ApiResult<CaptureResponse>

    suspend fun uploadBatchCaptures(
        request: BatchCaptureRequest
    ): ApiResult<List<CaptureResponse>>
}