package app.gov.uidai.contactlessregistration.di

import app.gov.uidai.contactlessregistration.usecase.CaptureQueueManager
import app.gov.uidai.contactlessregistration.usecase.impl.CaptureQueueManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkerModule {

    @Binds
    @Singleton
    abstract fun bindCaptureQueueManager(
        impl: CaptureQueueManagerImpl
    ): CaptureQueueManager
}