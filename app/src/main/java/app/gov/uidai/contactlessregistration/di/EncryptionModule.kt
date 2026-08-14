package app.gov.uidai.contactlessregistration.di

import app.gov.uidai.contactlessregistration.usecase.CaptureEncryption
import app.gov.uidai.contactlessregistration.usecase.impl.KeystoreCaptureEncryption
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class EncryptionModule {
    @Binds
    abstract fun bindCaptureEncryption(
        impl: KeystoreCaptureEncryption
    ): CaptureEncryption
}