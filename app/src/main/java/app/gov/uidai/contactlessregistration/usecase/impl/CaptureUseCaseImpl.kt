package app.gov.uidai.contactlessregistration.usecase.impl

import app.gov.uidai.contactlessregistration.data.remote.network.ApiResult
import app.gov.uidai.contactlessregistration.model.capture.BatchCaptureRequest
import app.gov.uidai.contactlessregistration.model.capture.CaptureRequest
import app.gov.uidai.contactlessregistration.model.capture.CaptureResponse
import app.gov.uidai.contactlessregistration.repository.ClfRepository
import app.gov.uidai.contactlessregistration.usecase.CaptureUseCase
import javax.inject.Inject

class CaptureUseCaseImpl @Inject constructor(
    private val clfRepository: ClfRepository
) : CaptureUseCase {

    // Uploads a single fingerprint capture to the backend
    override suspend fun uploadCapture(
        request: CaptureRequest
    ): ApiResult<CaptureResponse> {
        return clfRepository.uploadCapture(request)
    }

    // Wraps list of captures into BatchCaptureRequest and uploads
    override suspend fun uploadBatchCaptures(
        requests: List<CaptureRequest>
    ): ApiResult<List<CaptureResponse>> {
        val batchRequest = BatchCaptureRequest(captures = requests)
        return clfRepository.uploadBatchCaptures(batchRequest)
    }
}