package app.gov.uidai.contactlessregistration.usecase

import app.gov.uidai.contactlessregistration.data.remote.network.ApiResult
import app.gov.uidai.contactlessregistration.model.capture.CaptureRequest
import app.gov.uidai.contactlessregistration.model.capture.CaptureResponse

interface CaptureUseCase {

    suspend fun uploadCapture(
        request: CaptureRequest
    ): ApiResult<CaptureResponse>

    suspend fun uploadBatchCaptures(
        requests: List<CaptureRequest>
    ): ApiResult<List<CaptureResponse>>
}