package app.gov.uidai.contactlessregistration.di

import android.content.Context
import app.gov.uidai.contactlessregistration.repository.FingerprintRepository
import app.gov.uidai.contactlessregistration.repository.UserRepository
import app.gov.uidai.contactlessregistration.usecase.FingerSDKManager
import app.gov.uidai.contactlessregistration.usecase.UIDManager
import app.gov.uidai.contactlessregistration.usecase.UserUseCase
import app.gov.uidai.contactlessregistration.usecase.impl.FingerSDKManagerImpl
import app.gov.uidai.contactlessregistration.usecase.impl.UIDManagerImpl
import app.gov.uidai.contactlessregistration.usecase.impl.UserUseCaseImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import `in`.gov.uidai.embedding.FingerEmbedder
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Singleton
    @Provides
    fun provideUserUseCase(
        userRepository: UserRepository,
        fingerprintRepository: FingerprintRepository
    ): UserUseCase {
        return UserUseCaseImpl(
            userRepository = userRepository,
            fingerprintRepository = fingerprintRepository
        )
    }

    @Singleton
    @Provides
    fun provideFingerSDKManager(
        @ApplicationContext context: Context,
        fingerEmbedder: FingerEmbedder
    ): FingerSDKManager {
        return FingerSDKManagerImpl(
            context = context,
            fingerEmbedder =  fingerEmbedder
        )
    }

    @Provides
    @Singleton
    fun provideUIDManager(): UIDManager {
        return UIDManagerImpl()
    }

}