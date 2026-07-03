package app.gov.uidai.contactlessregistration.di

import android.content.Context
import app.gov.uidai.contactlessregistration.data.dao.PendingCaptureDao
import app.gov.uidai.contactlessregistration.repository.ClfRepository
import app.gov.uidai.contactlessregistration.repository.FingerprintRepository
import app.gov.uidai.contactlessregistration.repository.UserRepository
import app.gov.uidai.contactlessregistration.usecase.CaptureQueueManager
import app.gov.uidai.contactlessregistration.usecase.CaptureUseCase
import app.gov.uidai.contactlessregistration.usecase.FingerSDKManager
import app.gov.uidai.contactlessregistration.usecase.ResidentUseCase
import app.gov.uidai.contactlessregistration.usecase.SessionUseCase
import app.gov.uidai.contactlessregistration.usecase.UIDManager
import app.gov.uidai.contactlessregistration.usecase.UserUseCase
import app.gov.uidai.contactlessregistration.usecase.impl.CaptureQueueManagerImpl
import app.gov.uidai.contactlessregistration.usecase.impl.CaptureUseCaseImpl
import app.gov.uidai.contactlessregistration.usecase.impl.FingerSDKManagerImpl
import app.gov.uidai.contactlessregistration.usecase.impl.ResidentUseCaseImpl
import app.gov.uidai.contactlessregistration.usecase.impl.SessionUseCaseImpl
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

    @Provides
    @Singleton
    fun provideResidentUseCase(
        clfRepository: ClfRepository
    ): ResidentUseCase = ResidentUseCaseImpl(
        clfRepository = clfRepository
    )

    @Provides
    @Singleton
    fun provideSessionUseCase(
        clfRepository: ClfRepository
    ): SessionUseCase = SessionUseCaseImpl(
        clfRepository = clfRepository
    )

    @Provides
    @Singleton
    fun provideCaptureUseCase(
        clfRepository: ClfRepository
    ): CaptureUseCase = CaptureUseCaseImpl(
        clfRepository = clfRepository
    )

    @Provides
    @Singleton
    fun provideCaptureQueueManager(
        captureUseCase: CaptureUseCase,
        pendingCaptureDao: PendingCaptureDao
    ): CaptureQueueManager = CaptureQueueManagerImpl(
        captureUseCase = captureUseCase,
        pendingCaptureDao = pendingCaptureDao
    )
}