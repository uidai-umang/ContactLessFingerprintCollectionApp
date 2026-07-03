package app.gov.uidai.contactlessregistration.usecase

import app.gov.uidai.contactlessregistration.data.remote.network.ApiResult
import app.gov.uidai.contactlessregistration.model.session.CreateSessionResponse

interface SessionUseCase {

    suspend fun createSession(
        operatorId: String,
        deviceId: String,
        centreId: String,
        residentPseudonymId: String
    ): ApiResult<CreateSessionResponse>

    suspend fun closeSession(
        sessionId: String
    ): ApiResult<Unit>
}