package app.gov.uidai.contactlessregistration.repository

import app.gov.uidai.contactlessregistration.data.entity.UserEntity

interface UserRepository {
    suspend fun getUserByUidHash(uidHash: String): UserEntity?
    suspend fun insertUser(user: UserEntity)
    suspend fun deleteUser(uidHash: String)
    suspend fun isUserRegistered(uidHash: String): Boolean
}
