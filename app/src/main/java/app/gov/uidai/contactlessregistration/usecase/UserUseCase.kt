package app.gov.uidai.contactlessregistration.usecase

import app.gov.uidai.contactlessregistration.model.Fingerprint
import app.gov.uidai.contactlessregistration.model.User

interface UserUseCase {
    suspend fun register(uidHash: String, user: User, fingerprints: List<Fingerprint>)

    suspend fun isUserRegistered(uidHash: String): Boolean

    suspend fun getUser(uidHash: String): User?

    suspend fun getUserAndFingerprint(uidHash: String): Pair<User?, List<Fingerprint>>

    suspend fun getStoredEmbeddings(uidHash: String): List<ByteArray>
}