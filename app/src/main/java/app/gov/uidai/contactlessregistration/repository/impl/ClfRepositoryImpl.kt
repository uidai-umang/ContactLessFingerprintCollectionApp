package app.gov.uidai.contactlessregistration.repository.impl

import app.gov.uidai.contactlessregistration.data.remote.api.ClfApiService
import app.gov.uidai.contactlessregistration.data.remote.network.ApiResult
import app.gov.uidai.contactlessregistration.data.remote.network.MultipartHelper.buildBatchMetadataParts
import app.gov.uidai.contactlessregistration.data.remote.network.MultipartHelper.buildImagePart
import app.gov.uidai.contactlessregistration.data.remote.network.MultipartHelper.buildMetadataParts
import app.gov.uidai.contactlessregistration.data.remote.network.ResponseHandler
import app.gov.uidai.contactlessregistration.model.capture.BatchCaptureRequest
import app.gov.uidai.contactlessregistration.model.capture.CaptureRequest
import app.gov.uidai.contactlessregistration.model.capture.CaptureResponse
import app.gov.uidai.contactlessregistration.model.resident.ResidentLookupRequest
import app.gov.uidai.contactlessregistration.model.resident.ResidentLookupResponse
import app.gov.uidai.contactlessregistration.model.session.CloseSessionRequest
import app.gov.uidai.contactlessregistration.model.session.CreateSessionRequest
import app.gov.uidai.contactlessregistration.model.session.CreateSessionResponse
import app.gov.uidai.contactlessregistration.repository.ClfRepository
import javax.inject.Inject

class ClfRepositoryImpl @Inject constructor(
    private val apiService: ClfApiService
) : ClfRepository {

    override suspend fun lookupResident(
        request: ResidentLookupRequest
    ): ApiResult<ResidentLookupResponse> = ResponseHandler.safeApiCall {
        apiService.lookupResident(request)
    }

    override suspend fun createSession(
        request: CreateSessionRequest
    ): ApiResult<CreateSessionResponse> = ResponseHandler.safeApiCall {
        apiService.createSession(request)
    }

    override suspend fun closeSession(
        request: CloseSessionRequest
    ): ApiResult<Unit> = ResponseHandler.safeApiCall {
        apiService.closeSession(request)
    }

    override suspend fun uploadCapture(
        request: CaptureRequest
    ): ApiResult<CaptureResponse> = ResponseHandler.safeApiCall {
        apiService.uploadCapture(
            image = buildImagePart(request.imageBytes, request.fingerType),
            metadata = buildMetadataParts(request)
        )
    }

    override suspend fun uploadBatchCaptures(
        requests: List<CaptureRequest>
    ): ApiResult<List<CaptureResponse>> = ResponseHandler.safeApiCall {
        apiService.uploadBatchCaptures(
            images = requests.map { buildImagePart(it.imageBytes, it.fingerType) },
            metadata = buildBatchMetadataParts(requests)
        )
    }
}