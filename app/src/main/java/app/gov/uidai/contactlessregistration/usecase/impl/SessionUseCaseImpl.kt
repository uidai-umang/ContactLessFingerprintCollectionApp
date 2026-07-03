package app.gov.uidai.contactlessregistration.usecase.impl

import app.gov.uidai.contactlessregistration.data.remote.network.ApiResult
import app.gov.uidai.contactlessregistration.model.session.CloseSessionRequest
import app.gov.uidai.contactlessregistration.model.session.CreateSessionRequest
import app.gov.uidai.contactlessregistration.model.session.CreateSessionResponse
import app.gov.uidai.contactlessregistration.repository.ClfRepository
import app.gov.uidai.contactlessregistration.usecase.SessionUseCase
import javax.inject.Inject

class SessionUseCaseImpl @Inject constructor(
    private val clfRepository: ClfRepository
) : SessionUseCase {

    // Builds create session request and delegates to repository
    override suspend fun createSession(
        operatorId: String,
        deviceId: String,
        centreId: String,
        residentPseudonymId: String
    ): ApiResult<CreateSessionResponse> {
        val request = CreateSessionRequest(
            operatorId = operatorId,
            deviceId = deviceId,
            centreId = centreId,
            residentPseudonymId = residentPseudonymId
        )
        return clfRepository.createSession(request)
    }

    // Builds close session request and delegates to repository
    override suspend fun closeSession(
        sessionId: String
    ): ApiResult<Unit> {
        val request = CloseSessionRequest(
            sessionId = sessionId
        )
        return clfRepository.closeSession(request)
    }
}