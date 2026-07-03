package app.gov.uidai.contactlessregistration.data.remote.api

import app.gov.uidai.contactlessregistration.model.capture.BatchCaptureRequest
import app.gov.uidai.contactlessregistration.model.capture.CaptureRequest
import app.gov.uidai.contactlessregistration.model.capture.CaptureResponse
import app.gov.uidai.contactlessregistration.model.resident.ResidentLookupRequest
import app.gov.uidai.contactlessregistration.model.resident.ResidentLookupResponse
import app.gov.uidai.contactlessregistration.model.session.CloseSessionRequest
import app.gov.uidai.contactlessregistration.model.session.CreateSessionRequest
import app.gov.uidai.contactlessregistration.model.session.CreateSessionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ClfApiService {

    @POST(Urls.RESIDENT_LOOKUP)
    suspend fun lookupResident(
        @Body request: ResidentLookupRequest
    ): Response<ResidentLookupResponse>

    @POST(Urls.SESSION_CREATE)
    suspend fun createSession(
        @Body request: CreateSessionRequest
    ): Response<CreateSessionResponse>

    @POST(Urls.SESSION_CLOSE)
    suspend fun closeSession(
        @Body request: CloseSessionRequest
    ): Response<Unit>

    @POST(Urls.CAPTURE_UPLOAD)
    suspend fun uploadCapture(
        @Body request: CaptureRequest
    ): Response<CaptureResponse>

    @POST(Urls.CAPTURE_BATCH_UPLOAD)
    suspend fun uploadBatchCaptures(
        @Body request: BatchCaptureRequest
    ): Response<List<CaptureResponse>>
}