package app.gov.uidai.contactlessregistration.usecase

import app.gov.uidai.contactlessregistration.data.entity.PendingCaptureEntity
import app.gov.uidai.contactlessregistration.model.capture.CaptureResponse
import app.gov.uidai.contactlessregistration.data.remote.network.ApiResult

interface CaptureQueueManager {

    // Called after every successful finger capture.
    // Checks for pending captures for this resident first —
    // if any exist, sends batch (pending + new), else sends single.
    suspend fun uploadOrQueue(
        sessionId: String,
        residentPseudonymId: String,
        operatorId: String,
        fingerType: String,
        hand: String,
        imageBase64: String,
        imageChecksum: String,
        cameraModel: String,
        cameraResolution: String,
        deviceModel: String
    ): ApiResult<List<CaptureResponse>>

    // Called by WorkManager every 15 mins.
    // Processes all pending captures grouped by session_id sequentially.
    suspend fun syncPendingCaptures(): ApiResult<Unit>
}