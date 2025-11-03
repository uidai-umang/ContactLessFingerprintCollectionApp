package app.gov.uidai.contactlessregistration.usecase.impl

import app.gov.uidai.contactlessregistration.data.entity.FingerprintEntity
import app.gov.uidai.contactlessregistration.data.entity.UserEntity
import app.gov.uidai.contactlessregistration.model.Fingerprint
import app.gov.uidai.contactlessregistration.model.User
import app.gov.uidai.contactlessregistration.repository.FingerprintRepository
import app.gov.uidai.contactlessregistration.repository.UserRepository
import app.gov.uidai.contactlessregistration.usecase.UserUseCase

class UserUseCaseImpl(
    private val userRepository: UserRepository,
    private val fingerprintRepository: FingerprintRepository
): UserUseCase {
    override suspend fun register(
        uidHash: String,
        user: User,
        fingerprints: List<Fingerprint>
    ) {
        // Save user data
        val userEntity = UserEntity(
            uidHash = uidHash,
            name = user.name,
            phoneNumber = user.phoneNumber
        )

        // Save fingerprint data
        val fingerprintEntities = fingerprints.map { item ->
            val fingerQuality = item.fingerQuality
            val minutiaCount = fingerQuality?.getMinutia() ?: 0.0
            val contactArea = fingerQuality?.getContactArea() ?: 0.0
            val propreietaryQuality = fingerQuality?.getPropreietaryQuality() ?: 0.0

            FingerprintEntity(
                uidHash = uidHash,
                fingerPosition = item.fingerPosition,
                embeddingData = item.embedding,
                minutiaCount = minutiaCount,
                contactArea = contactArea,
                propreietaryQuality = propreietaryQuality
            )
        }

        userRepository.insertUser(userEntity)
        fingerprintRepository.insertFingerprints(fingerprintEntities)
    }

    override suspend fun isUserRegistered(uidHash: String): Boolean {
        return userRepository.isUserRegistered(uidHash)
    }

    override suspend fun getUser(uidHash: String): User? {
        return userRepository.getUserByUidHash(uidHash)?.let {
            User(
                name = it.name,
                phoneNumber = it.phoneNumber
            )
        }
    }

    override suspend fun getUserAndFingerprint(uidHash: String): Pair<User?, List<Fingerprint>> {

        val fingerprints = fingerprintRepository.getFingerprintsByUidHash(uidHash).map {
            Fingerprint(
                fingerPosition = it.fingerPosition,
                embedding = it.embeddingData
            )
        }

        val user = userRepository.getUserByUidHash(uidHash)?.let {
            User(
                name = it.name,
                phoneNumber = it.phoneNumber,
                fingerprintCount = fingerprints.size
            )
        }

        return user to fingerprints
    }

    override suspend fun getStoredEmbeddings(uidHash: String): List<ByteArray> {
        return fingerprintRepository.getFingerprintsByUidHash(uidHash).map {
            it.embeddingData
        }
    }
}