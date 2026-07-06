package app.gov.uidai.contactlessregistration.di

import app.gov.uidai.contactlessregistration.repository.ClfRepository
import app.gov.uidai.contactlessregistration.repository.FileRepository
import app.gov.uidai.contactlessregistration.repository.FingerprintRepository
import app.gov.uidai.contactlessregistration.repository.UserRepository
import app.gov.uidai.contactlessregistration.repository.impl.ClfRepositoryImpl
import app.gov.uidai.contactlessregistration.repository.impl.FileRepositoryImpl
import app.gov.uidai.contactlessregistration.repository.impl.FingerprintRepositoryImpl
import app.gov.uidai.contactlessregistration.repository.impl.UserRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository
    
    @Binds
    @Singleton
    abstract fun bindFingerprintRepository(
        fingerprintRepositoryImpl: FingerprintRepositoryImpl
    ): FingerprintRepository

    @Binds
    @Singleton
    abstract fun bindFileRepository(
        fileRepositoryImpl: FileRepositoryImpl
    ): FileRepository

    @Binds
    @Singleton
    abstract fun bindClfRepository(
        impl: ClfRepositoryImpl
    ): ClfRepository
}
