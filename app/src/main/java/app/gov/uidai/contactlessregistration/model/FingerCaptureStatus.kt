package app.gov.uidai.contactlessregistration.model

enum class FingerCaptureStatus {
    NOT_CAPTURED,
    CAPTURING,
    UPLOADING,
    CAPTURED,
    PENDING,
    FAILED
}
