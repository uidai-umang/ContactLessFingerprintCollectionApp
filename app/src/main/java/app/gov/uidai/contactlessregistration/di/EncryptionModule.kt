package app.gov.uidai.contactlessregistration.di

import app.gov.uidai.contactlessregistration.usecase.CaptureEncryption
import app.gov.uidai.contactlessregistration.usecase.impl.KeystoreCaptureEncryption
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import `in`.gov.uidai.contactlessregistration.encryption.Encrypter
import `in`.gov.uidai.contactlessregistration.encryption.EncryptionService
import `in`.gov.uidai.contactlessregistration.encryption.X509CertificateFactory
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EncryptionModule {
    // TODO: swap for the real backend-issued PEM once available -- same
    // build-flavor pattern as the reference app's staging/prod certificate
    // constants. Belongs in BuildConfig or a remote-config-fetched value,
    // not hardcoded long-term.
    private val ENCRYPTION_CERTIFICATE_PEM = """
        -----BEGIN CERTIFICATE-----
        <placeholder -- inject real cert here per build flavor>
        -----END CERTIFICATE-----
    """


    @Binds
    abstract fun bindCaptureEncryption(
        impl: KeystoreCaptureEncryption
    ): CaptureEncryption


    @Provides
    @Singleton
    fun provideX509CertificateFactory(): X509CertificateFactory = X509CertificateFactory()

    @Provides
    @Singleton
    fun provideEncrypter(
        certificateFactory: X509CertificateFactory
    ): Encrypter {
        val certificate = certificateFactory.generateCertificate(ENCRYPTION_CERTIFICATE_PEM)
        return Encrypter(certificate)
    }

    @Provides
    @Singleton
    fun provideEncryptionService(
        encrypter: Encrypter,
        certificateFactory: X509CertificateFactory
    ): EncryptionService {
        val certificate = certificateFactory.generateCertificate(ENCRYPTION_CERTIFICATE_PEM)
        return EncryptionService(encrypter, certificate)
    }
}