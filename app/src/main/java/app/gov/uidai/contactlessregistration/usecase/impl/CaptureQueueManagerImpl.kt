package app.gov.uidai.contactlessregistration.usecase.impl

import android.util.Log
import app.gov.uidai.contactlessregistration.data.dao.PendingCaptureDao
import app.gov.uidai.contactlessregistration.data.entity.PendingCaptureEntity
import app.gov.uidai.contactlessregistration.data.remote.network.ApiResult
import app.gov.uidai.contactlessregistration.model.capture.CaptureRequest
import app.gov.uidai.contactlessregistration.model.capture.CaptureResponse
import app.gov.uidai.contactlessregistration.usecase.CaptureQueueManager
import app.gov.uidai.contactlessregistration.usecase.CaptureUseCase
import javax.inject.Inject

class CaptureQueueManagerImpl @Inject constructor(
    private val captureUseCase: CaptureUseCase,
    private val pendingCaptureDao: PendingCaptureDao
) : CaptureQueueManager {

    companion object {
        private const val TAG = "CaptureQueueManager"
        private const val MAX_RETRY_COUNT = 5
    }

    // Checks for any pending captures for this resident first.
    // If pending exist → batch upload (pending + new capture together).
    // If none → single upload.
    // On any failure → saves new capture to local DB.
    override suspend fun uploadOrQueue(
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
    ): ApiResult<List<CaptureResponse>> {

        val newCapture = buildCaptureRequest(
            sessionId = sessionId,
            residentPseudonymId = residentPseudonymId,
            operatorId = operatorId,
            fingerType = fingerType,
            hand = hand,
            imageBase64 = imageBase64,
            imageChecksum = imageChecksum,
            cameraModel = cameraModel,
            cameraResolution = cameraResolution,
            deviceModel = deviceModel
        )

        // Check if any pending captures exist for this resident
        val pendingCaptures = pendingCaptureDao.getByResidentId(residentPseudonymId)

        return if (pendingCaptures.isEmpty()) {
            // No pending — try single upload
            Log.d(TAG, "No pending captures for resident. Trying single upload.")
            uploadSingle(newCapture)
        } else {
            // Pending exist — batch upload everything together
            Log.d(TAG, "${pendingCaptures.size} pending captures found. Trying batch upload.")
            uploadBatch(
                pendingCaptures = pendingCaptures,
                newCapture = newCapture,
                sessionId = sessionId,
                residentPseudonymId = residentPseudonymId,
                operatorId = operatorId
            )
        }
    }

    // Tries single upload. On failure saves to local DB.
    private suspend fun uploadSingle(
        request: CaptureRequest
    ): ApiResult<List<CaptureResponse>> {
        val result = captureUseCase.uploadCapture(request)

        return when (result) {
            is ApiResult.Success -> {
                Log.d(TAG, "Single upload succeeded: ${request.fingerType}")
                ApiResult.Success(listOf(result.data))
            }

            is ApiResult.Error -> {
                Log.w(TAG, "Single upload failed. Saving to local queue: ${result.message}")
                saveToPendingQueue(request)
                ApiResult.Error(result.message, result.code)
            }
        }
    }

    // Builds batch from pending + new capture, tries batch upload.
    // On success clears session's pending queue.
    // On failure saves new capture to local DB — existing pending already there.
    private suspend fun uploadBatch(
        pendingCaptures: List<PendingCaptureEntity>,
        newCapture: CaptureRequest,
        sessionId: String,
        residentPseudonymId: String,
        operatorId: String
    ): ApiResult<List<CaptureResponse>> {

        // Build full batch — existing pending + new capture
        val batchRequests = pendingCaptures.map { it.toCaptureRequest() } + newCapture

        val result = captureUseCase.uploadBatchCaptures(batchRequests)

        return when (result) {
            is ApiResult.Success -> {
                // Clear all pending for this session — they're now uploaded
                pendingCaptureDao.deleteBySessionId(sessionId)
                Log.d(TAG, "Batch upload succeeded. Cleared ${pendingCaptures.size} pending captures.")
                ApiResult.Success(result.data)
            }

            is ApiResult.Error -> {
                // Save new capture to pending — existing ones already in DB
                Log.w(TAG, "Batch upload failed. Saving new capture to queue: ${result.message}")
                saveToPendingQueue(newCapture)
                ApiResult.Error(result.message, result.code)
            }
        }
    }

    // Called by WorkManager every 15 mins.
    // Groups all pending by session_id and uploads sequentially.
    // Stops on first session failure — retries everything next cycle.
    override suspend fun syncPendingCaptures(): ApiResult<Unit> {
        val allPending = pendingCaptureDao.getAll()

        if (allPending.isEmpty()) {
            Log.d(TAG, "Sync: No pending captures found.")
            return ApiResult.Success(Unit)
        }

        Log.d(TAG, "Sync: Found ${allPending.size} pending captures. Grouping by session.")

        // Group by session_id preserving insertion order
        val groupedBySession = allPending.groupBy { it.sessionId }

        for ((sessionId, captures) in groupedBySession) {

            // Skip captures that have exceeded max retry limit
            val retryable = captures.filter { it.retryCount < MAX_RETRY_COUNT }
            if (retryable.isEmpty()) {
                Log.w(TAG, "Session $sessionId exceeded max retries. Skipping.")
                continue
            }

            Log.d(TAG, "Sync: Uploading ${retryable.size} captures for session $sessionId")

            val result = captureUseCase.uploadBatchCaptures(
                retryable.map { it.toCaptureRequest() }
            )

            when (result) {
                is ApiResult.Success -> {
                    pendingCaptureDao.deleteBySessionId(sessionId)
                    Log.d(TAG, "Sync: Session $sessionId uploaded successfully.")
                }

                is ApiResult.Error -> {
                    pendingCaptureDao.incrementRetryCount(sessionId)
                    Log.w(TAG, "Sync: Session $sessionId failed. Retrying next cycle.")
                    // Stop processing — retry all remaining next cycle
                    return ApiResult.Error(result.message, result.code)
                }
            }
        }

        return ApiResult.Success(Unit)
    }

    // Saves a failed capture request to local Room DB pending queue
    private suspend fun saveToPendingQueue(request: CaptureRequest) {
        val entity = PendingCaptureEntity(
            sessionId = request.sessionId,
            residentPseudonymId = request.residentPseudonymId,
            operatorId = request.operatorId,
            fingerType = request.fingerType,
            hand = request.hand,
            imageBase64 = request.imageBase64,
            imageChecksum = request.imageChecksum,
            cameraModel = request.cameraModel,
            cameraResolution = request.cameraResolution,
            deviceModel = request.deviceModel
        )
        pendingCaptureDao.insert(entity)
    }

    // Converts PendingCaptureEntity back to CaptureRequest for upload
    private fun PendingCaptureEntity.toCaptureRequest(): CaptureRequest {
        return CaptureRequest(
            sessionId = sessionId,
            residentPseudonymId = residentPseudonymId,
            operatorId = operatorId,
            fingerType = fingerType,
            hand = hand,
            imageBase64 = imageBase64,
            imageChecksum = imageChecksum,
            cameraModel = cameraModel,
            cameraResolution = cameraResolution,
            deviceModel = deviceModel
        )
    }

    // Builds a CaptureRequest from individual fields
    private fun buildCaptureRequest(
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
    ): CaptureRequest {
        return CaptureRequest(
            sessionId = sessionId,
            residentPseudonymId = residentPseudonymId,
            operatorId = operatorId,
            fingerType = fingerType,
            hand = hand,
            imageBase64 = imageBase64,
            imageChecksum = imageChecksum,
            cameraModel = cameraModel,
            cameraResolution = cameraResolution,
            deviceModel = deviceModel
        )
    }
}